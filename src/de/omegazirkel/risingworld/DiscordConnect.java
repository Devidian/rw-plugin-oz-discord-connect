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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import com.google.gson.Gson;

import de.omegazirkel.risingworld.discordconnect.ChatShortcutParser;
import de.omegazirkel.risingworld.discordconnect.DiscordConnectPluginInfoStatusProvider;
import de.omegazirkel.risingworld.discordconnect.DiscordChatMessage;
import de.omegazirkel.risingworld.discordconnect.DiscordCommandRequest;
import de.omegazirkel.risingworld.discordconnect.DiscordCommandResult;
import de.omegazirkel.risingworld.discordconnect.DiscordCommandService;
import de.omegazirkel.risingworld.discordconnect.JdaDiscordClient;
import de.omegazirkel.risingworld.discordconnect.PluginSettings;
import de.omegazirkel.risingworld.discordconnect.ui.DiscordConnectPlayerPluginData;
import de.omegazirkel.risingworld.discordconnect.ui.DiscordConnectPlayerPluginSettings;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.DiagnosticThreadFactory;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import de.omegazirkel.risingworld.tools.ServerThreadDispatcher;
import de.omegazirkel.risingworld.tools.db.SQLiteConnectionFactory;
import de.omegazirkel.risingworld.tools.settings.PlayerPluginAdminSettings;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
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
	private JdaDiscordClient discordClient;
	private DiscordCommandService commandService;
	private CloseableHttpAsyncClient webhookHttpClient;
	private String activeBotToken;
	public static String name;
	public static Connection sqliteCon;
	public static PlayerSettings playerSettings;
	private ServerThreadDispatcher serverThreadDispatcher;
	private ThreadPoolExecutor discordTransportExecutor;

	public static DiscordConnect instance = null;

	// Live properties
	static boolean flagRestart = false;
	static Plugin pluginGlobalIntercom = null;

	// Timer
	Timer restartTimer;
	Timer activityTimer;
	TimerTask restartTask;
	TimerTask restartForcedTask;
	TimerTask activityTask;
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
		initializeTimers();
		serverThreadDispatcher = new ServerThreadDispatcher(this);
		discordTransportExecutor = new ThreadPoolExecutor(
				1,
				1,
				0L,
				TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(256),
				new DiagnosticThreadFactory("OZDiscordConnect", "Discord transport", "OZDiscordConnect-Transport",
						true, logger()::debug),
				(task, executor) -> logger().warn("Discord transport queue is full or shutting down; message dropped"));
		webhookHttpClient = HttpAsyncClients.createDefault();
		webhookHttpClient.start();
		// Register event listener
		registerEventListener(this);
		t = I18n.getInstance(this);
		s = PluginSettings.getInstance(this);
		sqliteCon = SQLiteConnectionFactory.open(this);
		playerSettings = new PlayerSettings(sqliteCon);
		// lookup connected plugins
		pluginGlobalIntercom = getPluginByName("OZ - Global Intercom");
		if (pluginGlobalIntercom != null) {
			logger().info("ℹ️ Global Intercom found! ID: " + pluginGlobalIntercom.getID());
		}
		s.initSettings();
		commandService = new DiscordCommandService(this);
		this.initialize();
		this.statusNotification("TC_STATUS_ENABLED");

		// register plugin settings
		AssetManager.loadIconFromPlugin(this, "oz-discord-connect");
		PlayerPluginSettingsOverlay.registerPlayerPluginSettings(new DiscordConnectPlayerPluginSettings(getDescription("version")));
		PlayerPluginSettingsOverlay.registerPlayerPluginData(new DiscordConnectPlayerPluginData(getDescription("version")));
		PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(
				new PlayerPluginAdminSettings(name, getDescription("version"), () -> s.adminSettingsEntries(),
						s::initSettings));
			PluginInfoStatusProviders
					.registerProvider(new DiscordConnectPluginInfoStatusProvider(this, getDescription("version")));
			PluginShortcutVisibility.register(name, DiscordConnectPlayerPluginSettings::shortcutVisible);
			PluginMenuManager.registerPluginMenu(new MenuItem(name, "oz-discord-connect",
					"Discord Connect", player -> {
						player.hideRadialMenu(true);
						PluginInfoStatusProviders.show(player, name);
					}));
			logger().info("✅ " + this.getName() + " Plugin is enabled version:" + this.getDescription("version"));

	}

	private void initialize() {
		// restartTimesString, 10);
		if (s.restartTimed) {
			String[] restartTimes = s.restartTimesString.split("\\|");
			initRestartSchedule(restartTimes);
		}
		if (!s.botEnable) {
			stopDiscordClient();
			logger().warn("Discord bot is disabled");
			return;
		}
		if (discordClient == null || !s.botToken.equals(activeBotToken)) {
			stopDiscordClient();
			activeBotToken = s.botToken;
			JdaDiscordClient client = new JdaDiscordClient(this);
			discordClient = client;
			submitDiscordTransport(() -> {
				try {
					client.start();
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					logger().warn("JDA startup interrupted");
					handleDiscordClientStartFailure(client);
				} catch (RuntimeException ex) {
					logger().error("JDA startup failed: " + ex.getMessage());
					handleDiscordClientStartFailure(client);
				}
			});
		}

		// Start activity update timer
		if (activityTask != null) {
			activityTask.cancel();
		}
		activityTask = new TimerTask() {
			@Override
			public void run() {
				dispatchServer(DiscordConnect.this::updateDiscordActivity);
			}
		};
		activityTimer.schedule(activityTask, 0, 10000); // Check every 10 seconds
	}

	private void updateDiscordActivity() {
				if (s.botEnable && discordClient != null && discordClient.isReady()) {
					String currentActivity = "Running, " + Server.getPlayerCount() + " of " + Server.getMaxPlayerCount()
							+ " players";
					if (Server.getPlayerCount() == 0)
						currentActivity = "Running, waiting for players!";
					if (!currentActivity.equals(lastActivity)) {
						discordClient.updateActivity(currentActivity);
						lastActivity = currentActivity;
						logger().debug("Updated Discord activity to: " + currentActivity);
					}
				}
	}

	/**
	 *
	 */
	@Override
	public void onDisable() {
		logger().warn("⚠️ Disabling " + this.getName() + " ...");
		if (serverThreadDispatcher != null) {
			serverThreadDispatcher.close();
		}
		shutdownTimers();
		if (name != null) {
			PluginShortcutVisibility.unregister(name);
			PluginInfoStatusProviders.unregisterProvider(name);
		}
		this.statusNotification("TC_STATUS_DISABLED");
		stopDiscordClient();
		shutdownDiscordTransport();
		closeWebhookHttpClient();
		commandService = null;
		closeDatabase();
		DiscordConnect.instance = null;
	}

	private void stopDiscordClient() {
		JdaDiscordClient client = discordClient;
		discordClient = null;
		activeBotToken = null;
		if (client != null) {
			client.stopAccepting();
			submitDiscordTransport(client::close);
		}
	}

	private void handleDiscordClientStartFailure(JdaDiscordClient client) {
		client.close();
		if (discordClient == client) {
			discordClient = null;
			activeBotToken = null;
		}
	}

	private void closeWebhookHttpClient() {
		CloseableHttpAsyncClient client = webhookHttpClient;
		webhookHttpClient = null;
		if (client != null) {
			try {
				client.close();
			} catch (IOException ex) {
				logger().warn("Failed to close Discord webhook HTTP client: " + ex.getMessage());
			}
		}
	}

	void initializeTimers() {
		shutdownTimers();
		restartTimer = new Timer("OZDiscordConnect-RestartTimer", true);
		activityTimer = new Timer("OZDiscordConnect-ActivityTimer", true);
	}

	void shutdownTimers() {
		if (restartTask != null) {
			restartTask.cancel();
			restartTask = null;
		}
		if (restartForcedTask != null) {
			restartForcedTask.cancel();
			restartForcedTask = null;
		}
		if (activityTask != null) {
			activityTask.cancel();
			activityTask = null;
		}
		if (restartTimer != null) {
			restartTimer.cancel();
			restartTimer.purge();
			restartTimer = null;
		}
		if (activityTimer != null) {
			activityTimer.cancel();
			activityTimer.purge();
			activityTimer = null;
		}
	}

	private void closeDatabase() {
		if (sqliteCon != null) {
			try {
				sqliteCon.close();
			} catch (SQLException ex) {
				logger().warn("Failed to close Discord Connect database connection: " + ex.getMessage());
			}
			sqliteCon = null;
		}
		logger().warn("❌ " + this.getName() + " disabled.");
	}

	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event) {
		Player player = event.getPlayer();
		String lang = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);
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
					PluginInfoStatusProviders.show(player, name);
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
					PluginInfoStatusProviders.show(player, name);
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
					final int playerDbId = player.getDbID();
					final String playerLanguage = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);
					player.createScreenshot(sizeFactor, 1, !screenshotWithoutGui, (BufferedImage bimg) -> {
						final ByteArrayOutputStream os = new ByteArrayOutputStream();
						try {
							ImageIO.write(bimg, "jpg", os);
							this.sendDiscordSupportMessage("SupportTicket", supportMessage, os.toByteArray(),
									playerLanguage);
							dispatchServer(() -> {
								Player onlinePlayer = Server.getPlayerByDbID(playerDbId);
								if (onlinePlayer != null) {
									onlinePlayer.sendTextMessage(
											c.okay + this.getName() + ":>" + c.text + t.get("TC_SUPPORT_SUCCESS", lang));
								}
							});
						} catch (Exception e) {
							// throw new UncheckedIOException(ioe);
							logger().error(e.toString());
						}
					});
				} else {
					this.sendDiscordSupportMessage("SupportTicket", supportMessage, de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player));
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
			processMessage = !isGlobalIntercomMessage(event.getPlayer(), event.getChatMessage());
			if (noColorText.startsWith("#%")) {
				noColorText = noColorText.substring(2);
			}
		}

		if (processMessage && noColorText.trim().length() > 0) {
			Player player = event.getPlayer();
			String teleportCommand = "`goto " + player.getPosition().toString().replaceAll("[,()]", "") + "`";
			ChatShortcutParser.Result shortcuts = ChatShortcutParser.parse(noColorText, teleportCommand);
			noColorText = shortcuts.message();
			// check for teleport shortcut

			// check for screenshot shortcut
			Boolean screenshotWithoutGui = shortcuts.screenshotWithoutGui();
			Boolean hasScreenshot = shortcuts.hasScreenshot();

			if (s.allowScreenshots == true && hasScreenshot == true) {
				int playerResolutionX = player.getScreenResolutionX();
				float sizeFactor = 1.0f;
				if (playerResolutionX > s.maxScreenWidth) {
					sizeFactor = (s.maxScreenWidth * 1f / playerResolutionX * 1f);
				}
				final String textToSend = noColorText;
				final String playerName = player.getName();
				final String playerLanguage = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);
				logger().debug("Taking screenshot with factor " + sizeFactor);
				player.createScreenshot(sizeFactor, 1, !screenshotWithoutGui, (BufferedImage bimg) -> {
					final ByteArrayOutputStream os = new ByteArrayOutputStream();
					try {
						ImageIO.write(bimg, "jpg", os);
						this.sendDiscordChatMessage(playerName, textToSend, os.toByteArray(), playerLanguage);
					} catch (Exception e) {
						// throw new UncheckedIOException(ioe);
						logger().error(e.toString());
					}
				});
			} else {
				if (hasScreenshot == true) {
					logger().warn("⚠️ Screenshot taking not enabled");
				}
				this.sendDiscordChatMessage(player.getName(), noColorText, de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player));
			}
			if (s.colorizeChat) {
				broadcastChatMessage(player, noColorText);
				event.setCancelled(true);
			}
		}
	}

	private boolean isGlobalIntercomMessage(Player player, String message) {
		try {
			Object result = pluginGlobalIntercom.getClass()
					.getMethod("isGIMessage", Player.class, String.class)
					.invoke(pluginGlobalIntercom, player, message);
			return Boolean.TRUE.equals(result);
		} catch (ReflectiveOperationException ex) {
			logger().warn("Global Intercom does not provide isGIMessage(Player, String); chat filtering disabled: "
					+ ex.getMessage());
			pluginGlobalIntercom = null;
			return false;
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
					color + s.defaultChatPrefix.replace("**PH_LANGUAGE**", de.omegazirkel.risingworld.OZTools.getPlayerLanguage(eventPlayer))
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
			String lang = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);
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
				updateDiscordActivity("Restarting...");
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
		byte[] imageCopy = image == null ? null : Arrays.copyOf(image, image.length);
		JdaDiscordClient client = discordClient;
		submitDiscordTransport(() -> sendDiscordMessageToTextChannelNow(client, message, channelId, imageCopy));
	}

	private void sendDiscordMessageToTextChannelNow(JdaDiscordClient client, String message, long channelId, byte[] image) {
		if (channelId == 0) {
			logger().warn("⚠️ channelId = 0, set channelId in plugin settings or deactivate this channel");
			return;
		}
		if (client == null) {
			logger().warn("Cannot send direct Discord channel message while bot is disabled");
			return;
		}
		client.sendChannelMessage(channelId, message, image);
	}

	/**
	 *
	 * @param username
	 * @param text
	 * @param channel
	 * @param image
	 */
	private void sendDiscordMessageToWebHook(String username, String text, URI channel, byte[] image) {
		byte[] imageCopy = image == null ? null : Arrays.copyOf(image, image.length);
		submitDiscordTransport(() -> sendDiscordMessageToWebHookNow(username, text, channel, imageCopy));
	}

	private void sendDiscordMessageToWebHookNow(String username, String text, URI channel, byte[] image) {
		if (channel == null || channel.toString().isBlank()) {
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

		java.util.Map<String, String> json = new java.util.HashMap<>();
		json.put("content", text);
		json.put("username", username);

		if (s.overrideAvatar) {
			String avatarUrl = "https://api.adorable.io/avatars/128/" + username.replace(" ", "%20");
			json.put("avatar_url", avatarUrl);
		}

		CloseableHttpAsyncClient client = webhookHttpClient;
		if (client == null) {
			logger().warn("Discord webhook HTTP client is unavailable");
			return;
		}
		try {

			// ---------- send text message ----------
			SimpleHttpRequest post = SimpleHttpRequest.create(Method.POST, channel);
			post.setBody(new Gson().toJson(json), ContentType.APPLICATION_JSON);

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
			logger().error("Discord webhook request failed: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	private void submitDiscordTransport(Runnable task) {
		ThreadPoolExecutor executor = discordTransportExecutor;
		if (executor == null || executor.isShutdown()) {
			logger().warn("Discord transport is unavailable; message dropped");
			return;
		}
		executor.execute(() -> {
			try {
				task.run();
			} catch (RuntimeException ex) {
				logger().error("Discord transport failed: " + ex.getMessage());
			}
		});
	}

	private void shutdownDiscordTransport() {
		ThreadPoolExecutor executor = discordTransportExecutor;
		discordTransportExecutor = null;
		if (executor == null) {
			return;
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException ex) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
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
		if (s.webHookChatUrl != null && !s.webHookChatUrl.toString().isBlank())
			this.sendDiscordMessageToWebHook(username, text, s.webHookChatUrl, image);
		else if (s.chatChannelId != 0 && discordClient != null) {
			String message = s.discordChatSyntax
					.replace("**PH_PLAYER**", username)
					.replace("**PH_MESSAGE**", text)
					.replace("**PH_LANGUAGE**", language);
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
		if (s.webHookSupportUrl != null && !s.webHookSupportUrl.toString().isBlank())
			this.sendDiscordMessageToWebHook(username, text, s.webHookSupportUrl, image);
		else if (s.supportChannelId != 0 && discordClient != null) {
			String message = s.discordChatSyntax
					.replace("**PH_PLAYER**", username)
					.replace("**PH_MESSAGE**", text)
					.replace("**PH_LANGUAGE**", language);
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
		if (s.webHookStatusUrl != null && !s.webHookStatusUrl.toString().isBlank())
			this.sendDiscordMessageToWebHook(getStatusUserName(), text, s.webHookStatusUrl, null);
		else if (s.statusChannelId != 0 && discordClient != null) {
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
		if (s.webHookEventUrl != null && !s.webHookEventUrl.toString().isBlank())
			this.sendDiscordMessageToWebHook(getStatusUserName(), text, s.webHookEventUrl, null);
		else if (s.eventChannelId != 0 && discordClient != null) {
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
					dispatchServer(DiscordConnect.this::handleScheduledRestart);
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
						dispatchServer(DiscordConnect.this::handleForcedRestart);
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

	private void handleScheduledRestart() {
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

	private void handleForcedRestart() {
						logger().warn("Force server restart now!");
						for (Player player : Server.getAllPlayers()) {
							player.kick("Server restart");
						}
						forceRestart();
	}

	public boolean dispatchServer(Runnable task) {
		return serverThreadDispatcher != null && serverThreadDispatcher.dispatch(task);
	}

	public boolean dispatchDiscordChat(DiscordChatMessage message) {
		return dispatchServer(() -> {
			if (!message.channelName().equalsIgnoreCase(s.botChatChannelName)
					&& message.channelId() != s.chatChannelId) {
				return;
			}
			String color = message.admin() && s.showGroup ? s.colorLocalAdmin : s.colorLocalDiscord;
			String group = message.admin() && s.showGroup ? " (discord/admin)" : "";
			Server.broadcastTextMessage(color + s.defaultChatPrefix.replace("**PH_LANGUAGE**", "discord")
					+ message.displayName() + group + ": " + c.endTag + message.content());
		});
	}

	public boolean dispatchDiscordCommand(DiscordCommandRequest request) {
		return dispatchServer(() -> {
			DiscordCommandService service = commandService;
			if (service == null) {
				sendDiscordCommandResult(DiscordCommandResult.text(request.responseToken(), "Plugin is disabled"));
				return;
			}
			sendDiscordCommandResult(service.execute(request));
		});
	}

	private void sendDiscordCommandResult(DiscordCommandResult result) {
		submitDiscordTransport(() -> {
			JdaDiscordClient client = discordClient;
			if (client != null) {
				client.sendResult(result);
			}
		});
	}

	/**
	 *
	 * @param i18nIndex
	 * @param playerName
	 */
	private void broadcastMessage(String i18nIndex, String playerName) {
		for (Player player : Server.getAllPlayers()) {
			try {
				String lang = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);
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
				String lang = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);
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
		logger().setLevel(s.logLevel);
	}

	public static void forceRestart() {
		Server.saveAll();
		if (instance == null) {
			logger().error("DiscordConnect instance is null, cannot execute forceRestart()");
			return;
		}
		instance.updateDiscordActivity("Restarting soon...");

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

		instance.updateDiscordActivity("Restarting soon...");
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
			// Direct channel sends are queued behind an in-progress JDA startup.
			if (canSendStatusNotification(
					s.webHookStatusUrl != null && !s.webHookStatusUrl.toString().isBlank(),
					s.botEnable,
					discordClient != null)) {

				this.sendDiscordStatusMessage(messageText);
			} else {
				logger().warn("Could not send: " + messageText);
			}
		}
	}

	static boolean canSendStatusNotification(boolean webhookConfigured, boolean botEnabled, boolean clientPresent) {
		return webhookConfigured || (botEnabled && clientPresent);
	}

	private void updateDiscordActivity(String activity) {
		JdaDiscordClient client = discordClient;
		if (client != null) {
			client.updateActivity(activity);
		}
	}
}
