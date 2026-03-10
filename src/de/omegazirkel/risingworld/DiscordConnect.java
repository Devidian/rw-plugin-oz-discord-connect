/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.omegazirkel.risingworld;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.Method;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.UserStatus;
import org.json.simple.JSONObject;

import de.omegazirkel.risingworld.discordconnect.JavaCordBot;
import de.omegazirkel.risingworld.discordconnect.PluginSettings;
import de.omegazirkel.risingworld.discordconnect.Utils;
import de.omegazirkel.risingworld.discordconnect.ui.DiscordConnectPlayerPluginSettings;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChatEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

/**
 *
 * @author Maik "Devidian" Laschober
 */
public class DiscordConnect extends Plugin implements Listener, FileChangeListener {
	static final String pluginCMD = "dc";

	public static OZLogger logger() {
		return OZLogger.getInstance("OZ.DiscordConnect");
	}

	static final Colors c = Colors.getInstance();
	private static I18n t = null;
	private static PluginSettings s = null;
	static JavaCordBot DiscordBot = null;
	public static String name;

	public static DiscordConnect instance = null;

	// Live properties
	static boolean flagRestart = false;
	static Plugin pluginGlobalIntercom = null;

	// Timer
	static Timer restartTimer = new Timer("OZDiscordConnect-RestartTimer", true);
	static Timer activityTimer = new Timer("OZDiscordConnect-ActivityTimer", true);
	static TimerTask restartTask = null;
	static TimerTask restartForcedTask = null;
	static TimerTask activityTask = null;
	static String lastActivity = "";

	public void setFlagRestart(boolean value) {
		flagRestart = value;
	}

	public String getBotLanguage() {
		return s.botLang;
	}

	public String getStatusUserName() {
		String username = s.statusUsername;
		if (s.useServerName) {
			username = Server.getName();
		}
		return username;
	}

	public I18n getTranslator() {
		return t;
	}

	@Override
	public void onEnable() {
		name = this.getDescription("name");
		DiscordConnect.instance = this; // for timer
		// Register event listener
		registerEventListener(this);
		t = I18n.getInstance(this);
		s = PluginSettings.getInstance(this);
		// lookup connected plugins
		pluginGlobalIntercom = getPluginByName("OZ - Global Intercom");
		if (pluginGlobalIntercom != null) {
			logger().info("ℹ️ Global Intercom found! ID: " + pluginGlobalIntercom.getID());
		}
		s.initSettings();
		this.initialize();

		// register plugin settings
		PlayerPluginSettingsOverlay.registerPlayerPluginSettings(new DiscordConnectPlayerPluginSettings());
		logger().info("✅ " + this.getName() + " Plugin is enabled version:" + this.getDescription("version"));

	}

	private void initialize() {
		// restartTimesString, 10);
		if (s.restartTimed) {
			String[] restartTimes = s.restartTimesString.split("\\|");
			initRestartSchedule(restartTimes);
		}
		// only execute if DiscordBot was not yet initialized
		if (DiscordBot != null)
			return;
		if (!s.botEnable) {
			logger().warn("❌ DiscordBot is disabled");
			return;
		}

		try {
			DiscordBot = new JavaCordBot(this);
			DiscordBot.init();
		} catch (Exception ex) {
			logger().error(ex.toString());
		}

		// Start activity update timer
		if (activityTask != null) {
			activityTask.cancel();
		}
		activityTask = new TimerTask() {
			@Override
			public void run() {
				if (s.botEnable && JavaCordBot.api != null && JavaCordBot.api.getStatus() == UserStatus.ONLINE) {
					String currentActivity = "Running, " + Server.getPlayerCount() + " of " + Server.getMaxPlayerCount()
							+ " players";
					if (Server.getPlayerCount() == 0)
						currentActivity = "Running, waiting for players!";
					if (!currentActivity.equals(lastActivity)) {
						JavaCordBot.api.updateActivity(currentActivity);
						lastActivity = currentActivity;
						logger().debug("Updated Discord activity to: " + currentActivity);
					}
				}
			}
		};
		activityTimer.schedule(activityTask, 0, 10000); // Check every 10 seconds
		this.statusNotification("TC_STATUS_ENABLED");
	}

	/**
	 *
	 */
	@Override
	public void onDisable() {
		logger().warn("⚠️ Disabling " + this.getName() + " ...");
		this.statusNotification("TC_STATUS_DISABLED");
		if (s.botEnable) {
			JavaCordBot.disconnect();
			DiscordBot = null;
		}
		// Timer-Tasks und Timer abbrechen und zurücksetzen
		if (restartTask != null) {
			restartTask.cancel();
		}
		if (restartForcedTask != null) {
			restartForcedTask.cancel();
		}
		if (activityTask != null) {
			activityTask.cancel();
		}
		logger().warn("❌ " + this.getName() + " disabled.");
	}

	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event) {
		Player player = event.getPlayer();
		String lang = player.getSystemLanguage();
		String commandLine = event.getCommand();
		Vector3f pos = player.getPosition();

		String[] cmdParts = commandLine.split(" ", 2);
		String command = cmdParts[0];

		if (command.equals("/" + pluginCMD)) {
			// Invalid number of arguments (0)
			if (cmdParts.length < 2) {
				player.sendTextMessage(c.error + this.getName() + ":>" + c.text
						+ t.get("TC_MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
								.replace("PH_COMMAND_HELP", c.command + "/" + pluginCMD + " help\n" + c.text));
				return;
			}

			String option = cmdParts[1];

			switch (option) {
				case "restart":
					boolean canTriggerRestart = s.allowRestart && (player.isAdmin() || (!s.restartAdminOnly
							&& player.getTotalPlayTime() > s.restartMinimumTime && s.restartMinimumTime > 0));
					if (canTriggerRestart) {
						String msgDC = t.get("TC_DC_SHUTDOWN", s.botLang).replace("PH_PLAYER", player.getName());
						this.sendDiscordStatusMessage(msgDC);
						this.broadcastMessage("TC_BC_SHUTDOWN", player.getName());
						flagRestart = true;
					} else {
						player.sendTextMessage(
								c.error + this.getName() + ":>" + c.text + t.get("TC_CMD_RESTART_NOTALLOWED", lang));
					}
					break;
				case "info":
					String infoMessage = t.get("TC_CMD_INFO", lang)
							.replace("PH_CMD_SUPPORT", c.command + "/support TEXT" + c.text)
							.replace("PH_CMD_HELP", c.command + "/" + pluginCMD + " help" + c.text);
					player.sendTextMessage(c.okay + this.getName() + ":> " + c.text + infoMessage);
					break;
				case "help":
					String helpMessage = t.get("TC_CMD_HELP", lang)
							.replace("PH_CMD_SUPPORT", c.command + "/support TEXT" + c.text)
							.replace("PH_CMD_HELP", c.command + "/" + pluginCMD + " help" + c.text)
							.replace("PH_CMD_RESTART", c.command + "/" + pluginCMD + " restart" + c.text)
							.replace("PH_CMD_INFO", c.command + "/" + pluginCMD + " info" + c.text)
							.replace("PH_CMD_STATUS", c.command + "/" + pluginCMD + " status" + c.text)
							.replace("PH_CMD_JOIN", c.command + "/joinDiscord" + c.text);
					player.sendTextMessage(c.okay + this.getName() + ":> " + c.text + helpMessage);
					break;
				case "status":
					String statusMessage = t.get("TC_CMD_STATUS", lang)
							.replace("PH_VERSION", c.okay + this.getDescription("version") + c.text)
							.replace("PH_LANGUAGE",
									s.colorLocalSelf + player.getLanguage() + " / " + player.getSystemLanguage()
											+ c.text)
							.replace("PH_USEDLANG", s.colorLocalOther + t.getLanguageUsed(lang) + c.text)
							.replace("PH_LANG_AVAILABLE", c.okay + t.getLanguageAvailable() + c.text);
					player.sendTextMessage(c.okay + this.getName() + ":> " + c.text + statusMessage);
					break;
				default:
					break;
			}

		} else if (command.equals("/support")) {
			if (cmdParts.length < 2) {
				return;
			}
			String message = cmdParts[1];

			if (s.postSupport) {
				String supportMessage = "```" + player.getName() + ": " + message
						.replace("+screennogui", "🖼️")
						.replace("+screen", "🖼️")
						.replace("+sng ", "🖼️ ")
						.replace("+s ", "🖼️ ")
						+ (s.addTeleportCommand ? "\nTeleport command:> goto " + pos.x + " " + pos.y + " " + pos.z : "")
						+ "```";
				Boolean screenshotWithGui = message.contains("+screen") || message.contains("+s ");
				Boolean screenshotWithoutGui = message.contains("+screennogui") || message.contains("+sng ");
				Boolean hasScreenshot = screenshotWithGui || screenshotWithoutGui;
				if (s.supportScreenshot == true && hasScreenshot == true) {

					int playerResolutionX = player.getScreenResolutionX();
					float sizeFactor = 1.0f;
					if (playerResolutionX > s.maxScreenWidth) {
						sizeFactor = (s.maxScreenWidth * 1f / playerResolutionX * 1f);
					}
					logger().debug("Taking screenshot with factor " + sizeFactor);

					player.createScreenshot(sizeFactor, 1, !screenshotWithoutGui, (BufferedImage bimg) -> {
						final ByteArrayOutputStream os = new ByteArrayOutputStream();
						try {
							ImageIO.write(bimg, "jpg", os);
							this.sendDiscordSupportMessage("SupportTicket", supportMessage, os.toByteArray(),
									player.getSystemLanguage());
							player.sendTextMessage(
									c.okay + this.getName() + ":>" + c.text + t.get("TC_SUPPORT_SUCCESS", lang));
						} catch (Exception e) {
							// throw new UncheckedIOException(ioe);
							logger().error(e.toString());
						}
					});
				} else {
					this.sendDiscordSupportMessage("SupportTicket", supportMessage, player.getSystemLanguage());
					player.sendTextMessage(c.okay + this.getName() + ":>" + c.text + t.get("TC_SUPPORT_SUCCESS", lang));
				}
			} else {
				player.sendTextMessage(
						c.error + this.getName() + ":>" + c.text + t.get("TC_SUPPORT_NOTAVAILABLE", lang));
			}
		} else if (command.equals("/joinDiscord")) {
			if (s.joinDiscord.isEmpty()) {
				player.sendTextMessage(c.error + this.getName() + ":>" + c.text + t.get("TC_CMD_JOINDISCORD_NA", lang));
			} else {
				player.connectToDiscord("https://discord.gg/" + s.joinDiscord);
			}
		}

	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerChat(PlayerChatEvent event) {

		String message = event.getChatMessage();
		String noColorText = message.replaceAll("</?color(?:=#?[A-Fa-f0-9]{6})?>", "");
		Boolean processMessage = s.postChat;

		if (!processMessage) {
			return;
		}

		if (pluginGlobalIntercom != null) {
			processMessage = !((GlobalIntercom) pluginGlobalIntercom).isGIMessage(event.getPlayer(),
					event.getChatMessage());
			if (noColorText.startsWith("#%")) {
				noColorText = noColorText.substring(2);
			}
		}

		if (processMessage && noColorText.trim().length() > 0) {
			Player player = event.getPlayer();
			// check for teleport shortcut
			noColorText = noColorText.replace("+tp ",
					"`goto " + player.getPosition().toString().replaceAll("[,()]", "") + "`");

			// check for screenshot shortcut
			Boolean screenshotWithGui = noColorText.contains("+screen") || noColorText.contains("+s ");
			Boolean screenshotWithoutGui = noColorText.contains("+screennogui") || noColorText.contains("+sng ");
			Boolean hasScreenshot = screenshotWithGui || screenshotWithoutGui;

			if (s.allowScreenshots == true && hasScreenshot == true) {
				int playerResolutionX = player.getScreenResolutionX();
				float sizeFactor = 1.0f;
				if (playerResolutionX > s.maxScreenWidth) {
					sizeFactor = (s.maxScreenWidth * 1f / playerResolutionX * 1f);
				}
				final String textToSend = noColorText = noColorText
						.replace("+screennogui", "🖼️")
						.replace("+screen", "🖼️")
						.replace("+sng ", "🖼️ ")
						.replace("+s ", "🖼️ ");
				logger().debug("Taking screenshot with factor " + sizeFactor);
				player.createScreenshot(sizeFactor, 1, !screenshotWithoutGui, (BufferedImage bimg) -> {
					final ByteArrayOutputStream os = new ByteArrayOutputStream();
					try {
						ImageIO.write(bimg, "jpg", os);
						this.sendDiscordChatMessage(player.getName(), textToSend, os.toByteArray(),
								player.getSystemLanguage());
					} catch (Exception e) {
						// throw new UncheckedIOException(ioe);
						logger().error(e.toString());
					}
				});
			} else {
				if (hasScreenshot == true) {
					logger().warn("⚠️ Screenshot taking not enabled");
				}
				this.sendDiscordChatMessage(player.getName(), noColorText, player.getSystemLanguage());
			}
			if (s.colorizeChat) {
				broadcastChatMessage(player, noColorText);
				event.setCancelled(true);
			}
		}
	}

	/**
	 *
	 * @param eventPlayer
	 * @param noColorText
	 */
	private void broadcastChatMessage(Player eventPlayer, String noColorText) {
		Player[] players = Server.getAllPlayers();
		for (Player player : players) {
			String color = s.colorLocalOther;
			if (player.getUID() == eventPlayer.getUID()) {
				color = s.colorLocalSelf;
			} else if (eventPlayer.isAdmin()) {
				color = s.colorLocalAdmin;
			}

			String group = "";

			if (s.showGroup) {
				group = " (" + eventPlayer.getPermissionGroup() + ")";
			}

			player.sendTextMessage(
					color + s.defaultChatPrefix.replace("**PH_LANGUAGE**", eventPlayer.getSystemLanguage())
							+ eventPlayer.getName() + group + ": " + c.text + noColorText);
		}
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		if (s.sendPluginWelcome) {
			Player player = event.getPlayer();
			String lang = player.getSystemLanguage();
			player.sendTextMessage(t.get("TC_MSG_PLUGIN_WELCOME", lang)
					.replace("PH_PLUGIN_NAME", getDescription("name"))
					.replace("PH_PLUGIN_CMD", pluginCMD)
					.replace("PH_PLUGIN_VERSION", getDescription("version")));
		}
	}

	/**
	 *
	 * @param event
	 */

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		if (flagRestart) {
			int playersLeft = Server.getPlayerCount() - 1;
			if (playersLeft == 0) {
				this.sendDiscordStatusMessage(t.get("TC_RESTART_PLAYER_LAST", s.botLang));
				JavaCordBot.api.updateActivity("Restarting...");
				restart();
			} else if (playersLeft > 1) {
				this.broadcastMessage("TC_BC_PLAYER_REMAIN", playersLeft);
			}
		}
	}

	public void sendDiscordMessageToTextChannel(String message, long channelId) {
		sendDiscordMessageToTextChannel(message, channelId, null);
	}

	/**
	 * use this to send a message to a discord channel from other plugins than
	 * DiscordConnect
	 * 
	 * @param message
	 * @param channelId
	 * @param image
	 */
	public void sendDiscordMessageToTextChannel(String message, long channelId, byte[] image) {
		if (channelId == 0) {
			logger().warn("⚠️ channelId = 0, set channelId in plugin settings or deactivate this channel");
			return;
		}
		Optional<Channel> channel = JavaCordBot.api.getChannelById(channelId);
		if (!channel.isEmpty()) {
			ServerTextChannel tc = channel.get().asServerTextChannel().get();
			if (image != null) {
				try {
					tc.sendMessage(message, Utils.byteArrayToFile(image, "screenshot.jpg")).join();
				} catch (IOException e) {
					logger().error("Exception on sending discord chat message: " + e.getMessage());
					e.printStackTrace();
					// send without image
					tc.sendMessage(message).join();
				}
			} else {
				tc.sendMessage(message).join();
			}
			logger().debug("✅ Sent message to #" + tc.getName() + ": " + message);
		} else {
			logger().error("❌ ChannelId <" + channelId + "> not found not found cant send message: " + message);
		}
	}

	/**
	 *
	 * @param username
	 * @param text
	 * @param channel
	 * @param image
	 */
	private void sendDiscordMessageToWebHook(String username, String text, URI channel, byte[] image) {
		if (channel == null || channel.toString() == "") {
			logger().error("⚠️ Cant send message to webhook <channel:" +
					channel + "> <text:" + text + "> <username:" + username + ">");
			return;
		}
		// Username Validation
		username = username.replaceAll("[@:`]", "");
		if (username.length() < 2)
			username += "__";
		if (username.length() > 32)
			username = username.substring(0, 31);

		JSONObject json = new JSONObject();
		json.put("content", text);
		json.put("username", username);

		if (s.overrideAvatar) {
			String avatarUrl = "https://api.adorable.io/avatars/128/" + username.replace(" ", "%20");
			json.put("avatar_url", avatarUrl);
		}

		try (CloseableHttpAsyncClient client = HttpAsyncClients.createDefault()) {
			client.start();

			// ---------- send text message ----------
			SimpleHttpRequest post = SimpleHttpRequest.create(Method.POST, channel);
			post.setBody(json.toJSONString(), ContentType.APPLICATION_JSON);

			CompletableFuture<SimpleHttpResponse> textFuture = new CompletableFuture<>();

			client.execute(post, new FutureCallback<>() {
				@Override
				public void completed(SimpleHttpResponse result) {
					int status = result.getCode();
					if (status != 204) {
						logger().debug("Discord text response: " + status + "\n" + result.getBodyText());
					}
					textFuture.complete(result);
				}

				@Override
				public void failed(Exception ex) {
					logger().error("Discord text failed: " + ex.getMessage());
					textFuture.completeExceptionally(ex);
				}

				@Override
				public void cancelled() {
					logger().error("Discord text request cancelled");
					textFuture.cancel(true);
				}
			});

			// ---------- Optional: Image ----------
			CompletableFuture<SimpleHttpResponse> imageFuture = new CompletableFuture<>();

			if (image != null) {
				try {
					// random boundary generation (required for multipart)
					String boundary = "----BOUNDARY_" + System.currentTimeMillis();

					// Multipart Body
					MultipartEntityBuilder builder = MultipartEntityBuilder.create()
							.setMode(HttpMultipartMode.STRICT)
							.setBoundary(boundary)
							.addTextBody("username", username,
									ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));

					if (s.overrideAvatar) {
						builder.addTextBody(
								"avatar_url",
								"https://api.adorable.io/avatars/128/" + username.replace(" ", "%20"),
								ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
					}

					builder.addBinaryBody("file", image, ContentType.IMAGE_JPEG, "screenshot.jpg");

					HttpEntity multipart = builder.build();

					// Entity to ByteArray
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					multipart.writeTo(baos);
					byte[] bodyBytes = baos.toByteArray();

					// Request aufbauen
					SimpleHttpRequest req = SimpleHttpRequest.create("POST", channel);
					req.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
					req.setBody(bodyBytes, ContentType.MULTIPART_FORM_DATA);

					// Request ausführen
					client.execute(req, new FutureCallback<>() {
						@Override
						public void completed(SimpleHttpResponse result) {
							int status = result.getCode();
							if (status != 204) {
								logger().debug("Discord image response: " + status + "\n" + result.getBodyText());
							}
							imageFuture.complete(result);
						}

						@Override
						public void failed(Exception ex) {
							logger().error("Discord image failed: " + ex.getMessage());
							imageFuture.completeExceptionally(ex);
						}

						@Override
						public void cancelled() {
							logger().error("Discord image request cancelled");
							imageFuture.cancel(true);
						}
					});

					// Warten oder später weiterverwenden
					imageFuture.join();

				} catch (Exception ex) {
					logger().error("Error building multipart request: " + ex.getMessage());
					ex.printStackTrace();
				}
			} else {
				imageFuture.complete(null);
			}

			// ---------- optional wait ----------
			CompletableFuture.allOf(textFuture, imageFuture).join();

		} catch (Exception ex) {
			logger().error("Error initializing async client: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 * @param username
	 * @param text
	 */
	public void sendDiscordChatMessage(String username, String text, String language) {
		this.sendDiscordChatMessage(username, text, null, language);
	}

	/**
	 * 
	 * @param username
	 * @param text
	 * @param image
	 */
	public void sendDiscordChatMessage(String username, String text, byte[] image, String language) {
		if (s.webHookChatUrl != null && s.webHookChatUrl.toString() != "")
			this.sendDiscordMessageToWebHook(username, text, s.webHookChatUrl, image);
		else if (s.chatChannelId != 0 && JavaCordBot.api != null) {
			String message = s.discordChatSyntax
					.replace("**PH_PLAYER**", username)
					.replace("**PH_MESSAGE**", text)
					.replace("**PH_PLAYER_LANG**", language);
			sendDiscordMessageToTextChannel(message, s.chatChannelId, image);
		} else {
			logger().error("❌ Unable to send chat message: " + text);
		}
	}

	/**
	 *
	 * @param username
	 * @param text
	 * @param language
	 */
	private void sendDiscordSupportMessage(String username, String text, String language) {
		this.sendDiscordSupportMessage(username, text, null, language);
	}

	/**
	 * 
	 * @param username
	 * @param text
	 * @param image
	 */
	private void sendDiscordSupportMessage(String username, String text, byte[] image, String language) {
		if (s.webHookSupportUrl != null && s.webHookSupportUrl.toString() != "")
			this.sendDiscordMessageToWebHook(username, text, s.webHookSupportUrl, image);
		else if (s.supportChannelId != 0 && JavaCordBot.api != null) {
			String message = s.discordChatSyntax
					.replace("**PH_PLAYER**", username)
					.replace("**PH_MESSAGE**", text)
					.replace("**PH_PLAYER_LANG**", language);
			this.sendDiscordMessageToTextChannel(message, s.supportChannelId, image);
		} else {
			logger().error("❌ Unable to send support message: " + text);
		}
	}

	/**
	 * public API for status channel
	 *
	 * @param username
	 * @param text
	 */
	public void sendDiscordStatusMessage(String text) {
		if (s.webHookStatusUrl != null && s.webHookStatusUrl.toString() != "")
			this.sendDiscordMessageToWebHook(getStatusUserName(), text, s.webHookStatusUrl, null);
		else if (s.statusChannelId != 0 && JavaCordBot.api != null) {
			this.sendDiscordMessageToTextChannel(text, s.statusChannelId);
		} else {
			logger().error("❌ Unable to send status message: " + text);
		}
	}

	/**
	 * public API for event channel
	 *
	 * @param username
	 * @param text
	 */
	public void sendDiscordEventMessage(String text) {
		if (s.webHookEventUrl != null && s.webHookEventUrl.toString() != "")
			this.sendDiscordMessageToWebHook(getStatusUserName(), text, s.webHookEventUrl, null);
		else if (s.eventChannelId != 0 && JavaCordBot.api != null) {
			this.sendDiscordMessageToTextChannel(text, s.eventChannelId);
		} else {
			logger().error("❌ Unable to send event message: " + text);
		}
	}

	/**
	 *
	 * @param times
	 */
	private void initRestartSchedule(String[] times) {
		try {
			Calendar cal = Calendar.getInstance();
			int minHour = 24;
			int minMinute = 60;
			int nextHour = -1;
			int nextMinute = -1;
			for (String time : times) {
				String[] timeParts = time.split(":");
				int hour = Integer.parseInt(timeParts[0]);
				int minute = Integer.parseInt(timeParts[1]);
				// get min time if we have to jump to the next day
				if (hour <= minHour) {
					minHour = hour;
					if (minute <= minMinute) {
						minMinute = minute;
					}
				}
				// look for the next time to restart (nearest)
				// Same hour but greater minutes
				if (hour == cal.get(HOUR_OF_DAY) && minute > cal.get(MINUTE)
						&& (nextMinute < 0 || nextMinute > minute)) {
					logger().debug("new time found: " + hour + ":" + minute);
					nextHour = hour;
					nextMinute = minute;
					// if hour is greater than current AND nextHour is not set or greater than hour
					// AND nextMinute is not set or hour is equal nextHour and nextMinute is greater
				} else if (hour > cal.get(HOUR_OF_DAY) && (nextHour < 0 || nextHour >= hour)
						&& (nextMinute < 0 || (hour == nextHour && nextMinute > minute))) {
					logger().debug("new time found: " + hour + ":" + minute);
					nextHour = hour;
					nextMinute = minute;
				}

			}

			if (nextHour < 0) {
				nextHour = minHour;
				nextMinute = minMinute;
			}

			if (nextHour < cal.get(HOUR_OF_DAY) || (nextHour == cal.get(HOUR_OF_DAY) && nextMinute < cal.get(MINUTE))) {
				cal.set(DAY_OF_MONTH, cal.get(DAY_OF_MONTH) + 1);
			}
			cal.set(HOUR_OF_DAY, nextHour);
			cal.set(MINUTE, nextMinute);

			logger().info("Next Server restart time is scheduled on " + nextHour + ":" + nextMinute);

			if (restartTask != null) {
				restartTask.cancel();
			}

			restartTask = new TimerTask() {
				@Override
				public void run() {
					int playerNum = Server.getPlayerCount();
					if (playerNum > 0) {
						logger().info("Setting restart flag for scheduled server-restart");
						broadcastMessage("TC_RS_SCHEDULE_INFO");
						flagRestart = true;
						if (DiscordConnect.instance != null)
							DiscordConnect.instance.statusNotification("TC_STATUS_RESTART_FLAG");
						if (s.forceRestartAfter > 0) {
							broadcastMessage("TC_RS_SCHEDULE_WARN", s.forceRestartAfter);
						}
					} else {
						logger().info("Restarting server now (scheduled)");
						if (DiscordConnect.instance != null)
							DiscordConnect.instance.statusNotification("TC_STATUS_RESTART_SCHEDULED");

						restart();
					}
				}
			};

			restartTimer.schedule(restartTask, cal.getTime());

			// force restarting
			if (s.forceRestartAfter > 0) {
				if (restartForcedTask != null) {
					restartForcedTask.cancel();
				}

				restartForcedTask = new TimerTask() {
					@Override
					public void run() {
						logger().warn("Force server restart now!");
						for (Player player : Server.getAllPlayers()) {
							player.kick("Server restart");
						}
						forceRestart();
					}
				};

				cal.set(MINUTE, nextMinute + s.forceRestartAfter);
				restartTimer.schedule(restartForcedTask, cal.getTime());
			}

			// clear canceled tasks;
			restartTimer.purge();
			activityTimer.purge();

		} catch (Exception e) {
			logger().fatal(e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param i18nIndex
	 * @param playerName
	 */
	private void broadcastMessage(String i18nIndex, String playerName) {
		for (Player player : Server.getAllPlayers()) {
			try {
				String lang = player.getSystemLanguage();
				player.sendTextMessage(c.warning + this.getName() + ":> " + c.text
						+ t.get(i18nIndex, lang).replace("PH_PLAYER", playerName));
			} catch (Exception e) {
				logger().error(e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 *
	 * @param i18nIndex
	 * @param number
	 */
	private void broadcastMessage(String i18nIndex, int number) {
		for (Player player : Server.getAllPlayers()) {
			try {
				String lang = player.getSystemLanguage();
				player.sendTextMessage(c.warning + this.getName() + ":> " + c.text
						+ t.get(i18nIndex, lang).replace("PH_NUMBER", number + ""));
			} catch (Exception e) {
				logger().error(e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 *
	 * @param i18nIndex
	 */
	private void broadcastMessage(String i18nIndex) {
		this.broadcastMessage(i18nIndex, "");
	}

	@Override
	public void onSettingsChanged(Path settingsPath) {
		if (s.reportSettingsChanged) {
			this.sendDiscordStatusMessage(t.get("TC_UPDATE_SETTINGS", s.botLang));
		}
		s.initSettings(settingsPath.toString());
		this.initialize();
		// set logger level
		Utils.logger().setLevel(s.logLevel);
		JavaCordBot.logger().setLevel(s.logLevel);
		logger().setLevel(s.logLevel);
	}

	public static void forceRestart() {
		Server.saveAll();
		if (instance == null) {
			logger().error("DiscordConnect instance is null, cannot execute forceRestart()");
			return;
		}
		JavaCordBot.api.updateActivity("Restarting soon...");

		instance.statusNotification("TC_STATUS_RESTART_FORCED");
		instance.executeDelayed(5, () -> {
			if (s.useShutdownNotRestart)
				Server.sendInputCommand("shutdown");
			else
				Server.sendInputCommand("restart");
		});
	}

	public static void restart() {
		Server.saveAll();

		if (instance == null) {
			logger().error("DiscordConnect instance is null, cannot execute restart()");
			return;
		}

		JavaCordBot.api.updateActivity("Restarting soon...");
		instance.executeDelayed(5, () -> {
			if (s.useShutdownNotRestart)
				Server.sendInputCommand("shutdown");
			else
				Server.sendInputCommand("restart");
		});
	}

	public void statusNotification(String message) {
		if (s.reportServerStatus) {
			String messageText = t.get(message, s.botLang)
					.replace("PH_PLUGIN_NAME", this.getDescription("name"))
					.replace("PH_PLUGIN_VERSION", this.getDescription("version"))
					.replace("PH_PLAYER_COUNT", Server.getPlayerCount() + "");
			// Sende Statusnachricht nur, wenn der Bot aktiviert und verbunden ist
			if (s.botEnable && JavaCordBot.api != null && JavaCordBot.api.getStatus() == UserStatus.ONLINE) {

				this.sendDiscordStatusMessage(messageText);
			} else {
				logger().warn("Could not send: " + messageText);
			}
		}
	}
}
