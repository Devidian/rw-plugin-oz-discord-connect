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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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

import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.definitions.Npcs;
import net.risingworld.api.definitions.Npcs.Behaviour;
import net.risingworld.api.definitions.WeatherDefs;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.npc.NpcDeathEvent;
import net.risingworld.api.events.npc.NpcDeathEvent.Cause;
import net.risingworld.api.events.player.PlayerChatEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDeathEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.player.PlayerTeleportEvent;
import net.risingworld.api.events.player.world.PlayerDestroyObjectEvent;
import net.risingworld.api.events.player.world.PlayerRemoveObjectEvent;
import net.risingworld.api.events.world.SeasonChangeEvent;
import net.risingworld.api.events.world.WeatherChangeEvent;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

/**
 *
 * @author Maik "Devidian" Laschober
 */
public class DiscordWebHook extends Plugin implements Listener, FileChangeListener {
	static final String pluginCMD = "dc";

	public static OZLogger logger() {
		return OZLogger.getInstance("OZ.DiscordConnect");
	}

	public static OZLogger eventLogger() {
		return OZLogger.getInstance("OZ.DiscordConnect.Event");
	}

	static final Colors c = Colors.getInstance();
	private static I18n t = null;

	static DiscordWebHook instance = null;
	// Settings
	// static int logLevel = 0;
	static String joinDiscord = "";
	static boolean botEnable = false;
	static boolean sendPluginWelcome = false;
	static String botToken = "";
	static String botAdmins = "";
	static String botLang = "en";

	// Discord chat settings
	static boolean postChat = false;
	static boolean overrideAvatar = true;
	static URI webHookChatUrl = null;
	static long chatChannelId = 0;
	static String botChatChannelName = "server-chat";

	// Discord support settings
	static boolean postSupport = false;
	static boolean supportScreenshot = true;
	static boolean addTeleportCommand = true;
	static URI webHookSupportUrl = null;
	static long supportChannelId = 0;

	// Discord status settings
	static boolean postStatus = false;
	static boolean useServerName = false;
	static boolean reportServerStatus = false;
	static boolean reportSettingsChanged = true;
	static boolean reportJarChanged = true;
	static String statusUsername = "My Server";
	static URI webHookStatusUrl = null;
	static long statusChannelId = 0;

	// Discord event settings
	static boolean postTrackedEvents = false;
	static boolean trackMountKill = false;
	static boolean trackNonHostileAnimalKill = false;
	static boolean trackPickupables = false;
	static boolean trackPlayerDeaths = false;
	static boolean trackPlayerTeleports = false;
	static boolean trackWeatherChanges = false;
	static boolean trackSeasonChanges = false;
	static URI webHookEventUrl = null;
	static long eventChannelId = 0;

	// Discord SlashCommands settings
	static boolean botSecure = true;
	static boolean restartAdminOnly = true;
	static boolean allowRestart = false;

	// other settings
	// reloadallplugins
	static int restartMinimumTime = 86400;// (60 * 60 * 24); // 1 Day default
	static boolean restartTimed = false; // restart schedule
	static int forceRestartAfter = 5; // Minutes

	static boolean allowScreenshots = true;
	static int maxScreenWidth = 1920;

	static boolean colorizeChat = true;
	static boolean showGroup = false;
	static String colorSupport = "<color=#782d8e>";
	static String colorLocalSelf = "<color=#ddffdd>";
	static String colorLocalAdmin = "<color=#db3208>";
	static String colorLocalOther = "<color=#dddddd>";
	static String colorLocalDiscord = "<color=#ddddff>";
	public static String defaultChatPrefix = "[LOCAL] ";
	public static String discordChatSyntax = "[chat] **PH_PLAYER**: **PH_MESSAGE**";

	public static Map<String, Short> discordCommands = new HashMap<>();

	// END Settings
	// Live properties
	static boolean flagRestart = false;
	static Plugin pluginGlobalIntercom = null;
	static JavaCordBot DiscordBot = null;

	// Timer
	static Timer restartTimer = new Timer("OZDiscordConnect-RestartTimer", true);
	static Timer activityTimer = new Timer("OZDiscordConnect-ActivityTimer", true);
	static TimerTask restartTask = null;
	static TimerTask restartForcedTask = null;
	static TimerTask activityTask = null;
	static String lastActivity = "";

	// getter
	public String getBotToken() {
		return botToken;
	}

	public String getBotAdmins() {
		return botAdmins;
	}

	public String getBotLanguage() {
		return botLang;
	}

	public boolean getBotSecure() {
		return botSecure;
	}

	public boolean getOverrideAvatar() {
		return overrideAvatar;
	}

	public String getColorSupport() {
		return colorSupport;
	}

	public String getColorEndTag() {
		return "</color>";
	}

	public void setFlagRestart(boolean value) {
		flagRestart = value;
	}

	public String getBotChatChannelName() {
		return botChatChannelName;
	}

	public long getBotChatChannelId() {
		return chatChannelId;
	}

	public String getColorLocalDiscord() {
		return colorLocalDiscord;
	}

	public String getColorLocalAdmin() {
		return colorLocalAdmin;
	}

	public boolean getShowGroupSetting() {
		return showGroup;
	}

	public String getStatusUserName() {
		String username = statusUsername;
		if (useServerName) {
			username = Server.getName();
		}
		return username;
	}

	public I18n getTranslator() {
		return t;
	}

	@Override
	public void onEnable() {
		// Register event listener
		t = new I18n(this);
		registerEventListener(this);
		pluginGlobalIntercom = getPluginByName("OZ - Global Intercom");
		if (pluginGlobalIntercom != null) {
			logger().info("ℹ️ Global Intercom found! ID: " + pluginGlobalIntercom.getID());
		}
		this.initSettings();
		this.initialize();
	}

	private void initialize() {
		// only execute if DiscordBot was not yet initialized
		if (DiscordBot != null)
			return;
		if (!botEnable) {
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
				if (botEnable && JavaCordBot.api != null && JavaCordBot.api.getStatus() == UserStatus.ONLINE) {
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

		logger().info("✅ " + this.getName() + " Plugin is enabled version:" + this.getDescription("version"));
		DiscordWebHook.instance = this;

		this.statusNotification("STATUS_ENABLED");
	}

	/**
	 *
	 */
	@Override
	public void onDisable() {
		this.statusNotification("STATUS_DISABLED");
		if (botEnable) {
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
						+ t.get("MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
								.replace("PH_COMMAND_HELP", c.command + "/" + pluginCMD + " help\n" + c.text));
				return;
			}

			String option = cmdParts[1];

			switch (option) {
				case "restart":
					boolean canTriggerRestart = allowRestart && (player.isAdmin() || (!restartAdminOnly
							&& player.getTotalPlayTime() > restartMinimumTime && restartMinimumTime > 0));
					if (canTriggerRestart) {
						String msgDC = t.get("DC_SHUTDOWN", botLang).replace("PH_PLAYER", player.getName());
						this.sendDiscordStatusMessage(msgDC);
						this.broadcastMessage("BC_SHUTDOWN", player.getName());
						flagRestart = true;
					} else {
						player.sendTextMessage(
								c.error + this.getName() + ":>" + c.text + t.get("CMD_RESTART_NOTALLOWED", lang));
					}
					break;
				case "info":
					String infoMessage = t.get("CMD_INFO", lang)
							.replace("PH_CMD_SUPPORT", c.command + "/support TEXT" + c.text)
							.replace("PH_CMD_HELP", c.command + "/" + pluginCMD + " help" + c.text);
					player.sendTextMessage(c.okay + this.getName() + ":> " + c.text + infoMessage);
					break;
				case "help":
					String helpMessage = t.get("CMD_HELP", lang)
							.replace("PH_CMD_SUPPORT", c.command + "/support TEXT" + c.text)
							.replace("PH_CMD_HELP", c.command + "/" + pluginCMD + " help" + c.text)
							.replace("PH_CMD_RESTART", c.command + "/" + pluginCMD + " restart" + c.text)
							.replace("PH_CMD_INFO", c.command + "/" + pluginCMD + " info" + c.text)
							.replace("PH_CMD_STATUS", c.command + "/" + pluginCMD + " status" + c.text)
							.replace("PH_CMD_JOIN", c.command + "/joinDiscord" + c.text);
					player.sendTextMessage(c.okay + this.getName() + ":> " + c.text + helpMessage);
					break;
				case "status":
					String statusMessage = t.get("CMD_STATUS", lang)
							.replace("PH_VERSION", c.okay + this.getDescription("version") + c.text)
							.replace("PH_LANGUAGE",
									colorLocalSelf + player.getLanguage() + " / " + player.getSystemLanguage() + c.text)
							.replace("PH_USEDLANG", colorLocalOther + t.getLanguageUsed(lang) + c.text)
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

			if (postSupport) {
				String supportMessage = "```" + player.getName() + ": " + message
						.replace("+screennogui", "🖼️")
						.replace("+screen", "🖼️")
						.replace("+sng ", "🖼️ ")
						.replace("+s ", "🖼️ ")
						+ (addTeleportCommand ? "\nTeleport command:> goto " + pos.x + " " + pos.y + " " + pos.z : "")
						+ "```";
				Boolean screenshotWithGui = message.contains("+screen") || message.contains("+s ");
				Boolean screenshotWithoutGui = message.contains("+screennogui") || message.contains("+sng ");
				Boolean hasScreenshot = screenshotWithGui || screenshotWithoutGui;
				if (supportScreenshot == true && hasScreenshot == true) {

					int playerResolutionX = player.getScreenResolutionX();
					float sizeFactor = 1.0f;
					if (playerResolutionX > maxScreenWidth) {
						sizeFactor = (maxScreenWidth * 1f / playerResolutionX * 1f);
					}
					logger().debug("Taking screenshot with factor " + sizeFactor);

					player.createScreenshot(sizeFactor, 1, !screenshotWithoutGui, (BufferedImage bimg) -> {
						final ByteArrayOutputStream os = new ByteArrayOutputStream();
						try {
							ImageIO.write(bimg, "jpg", os);
							this.sendDiscordSupportMessage("SupportTicket", supportMessage, os.toByteArray());
							player.sendTextMessage(
									c.okay + this.getName() + ":>" + c.text + t.get("SUPPORT_SUCCESS", lang));
						} catch (Exception e) {
							// throw new UncheckedIOException(ioe);
							logger().error(e.toString());
						}
					});
				} else {
					this.sendDiscordSupportMessage("SupportTicket", supportMessage);
					player.sendTextMessage(c.okay + this.getName() + ":>" + c.text + t.get("SUPPORT_SUCCESS", lang));
				}
			} else {
				player.sendTextMessage(c.error + this.getName() + ":>" + c.text + t.get("SUPPORT_NOTAVAILABLE", lang));
			}
		} else if (command.equals("/joinDiscord")) {
			if (joinDiscord.isEmpty()) {
				player.sendTextMessage(c.error + this.getName() + ":>" + c.text + t.get("CMD_JOINDISCORD_NA", lang));
			} else {
				player.connectToDiscord("https://discord.gg/" + joinDiscord);
			}
		} else if (command.equals("/ozrestart")) {
			player.sendTextMessage(c.error + this.getName() + ":>" + c.text + t.get("CMD_ERR_DEPRECATED", lang)
					.replace("PH_NEWCMD", c.command + "/" + pluginCMD + " restart" + c.text));
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
		Boolean processMessage = postChat;

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

			if (allowScreenshots == true && hasScreenshot == true) {
				int playerResolutionX = player.getScreenResolutionX();
				float sizeFactor = 1.0f;
				if (playerResolutionX > maxScreenWidth) {
					sizeFactor = (maxScreenWidth * 1f / playerResolutionX * 1f);
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
						this.sendDiscordChatMessage(player.getName(), textToSend, os.toByteArray());
					} catch (Exception e) {
						// throw new UncheckedIOException(ioe);
						logger().error(e.toString());
					}
				});
			} else {
				if (hasScreenshot == true) {
					logger().warn("⚠️ Screenshot taking not enabled");
				}
				this.sendDiscordChatMessage(player.getName(), noColorText);
			}
			if (colorizeChat) {
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
			String color = colorLocalOther;
			if (player.getUID() == eventPlayer.getUID()) {
				color = colorLocalSelf;
			} else if (eventPlayer.isAdmin()) {
				color = colorLocalAdmin;
			}

			String group = "";

			if (showGroup) {
				group = " (" + eventPlayer.getPermissionGroup() + ")";
			}

			player.sendTextMessage(
					color + defaultChatPrefix + eventPlayer.getName() + group + ": " + c.text + noColorText);
		}
	}

	@EventMethod
	public void onSeasonChange(SeasonChangeEvent event) {
		String message = t.get("EVENT_SEASON_CHANGE", botLang)
				.replace("PH_SEASON", event.getSeason().toString());
		eventLogger().info(message);
		if (postTrackedEvents && trackSeasonChanges)
			sendDiscordEventMessage(message);
	}

	@EventMethod
	public void onWeatherChange(WeatherChangeEvent event) {
		WeatherDefs.Weather defCurrent = event.getCurrentWeather();
		String currentWeatherName = defCurrent.name;
		WeatherDefs.Weather defNext = event.getNextWeather();
		String nextWeatherName = defNext != null ? defNext.name : "";

		String message = t.get("EVENT_WEATHER_CHANGE", botLang)
				.replace("PH_WEATHER_FROM", currentWeatherName)
				.replace("PH_WEATHER_TO", nextWeatherName);

		eventLogger().info(message);
		if (postTrackedEvents && trackWeatherChanges)
			sendDiscordEventMessage(message);
	}

	@EventMethod
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();

		String message = t.get("EVENT_PLAYER_TELEPORT", botLang)
				.replace("PH_PLAYER", player.getName())
				.replace("PH_LOCATION", player.getPosition().toString().replaceAll("[,()]", ""));

		eventLogger().info(message);
		if (postTrackedEvents && trackPlayerTeleports)
			sendDiscordEventMessage(message);
	}

	@EventMethod
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getPlayer();
		String message = t.get("EVENT_PLAYER_DEATH", botLang)
				.replace("PH_PLAYER", player.getName())
				.replace("PH_CAUSE", event.getCause().toString())
				.replace("PH_LOCATION", event.getDeathPosition().toString().replaceAll("[,()]", ""));

		eventLogger().info(message);
		if (postTrackedEvents && trackPlayerDeaths)
			sendDiscordEventMessage(message);
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		if (sendPluginWelcome) {
			Player player = event.getPlayer();
			String lang = player.getSystemLanguage();
			player.sendTextMessage(t.get("MSG_PLUGIN_WELCOME", lang));
		}
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerConnect(PlayerConnectEvent event) {
		if (postStatus) {
			Player player = event.getPlayer();
			eventLogger().info("Player " + player.getName() + " connected at "
					+ player.getPosition().toString().replaceAll("[,()]", ""));
			this.sendDiscordStatusMessage(
					t.get("EVENT_PLAYER_CONNECTED", botLang).replace("PH_PLAYER", player.getName()));
		}
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		if (postStatus) {
			Player player = event.getPlayer();
			eventLogger().info("Player " + player.getName() + " disconnected at "
					+ player.getPosition().toString().replaceAll("[,()]", ""));
			this.sendDiscordStatusMessage(
					t.get("EVENT_PLAYER_DISCONNECTED", botLang).replace("PH_PLAYER", player.getName()));

		}
		if (flagRestart) {
			int playersLeft = Server.getPlayerCount() - 1;
			if (playersLeft == 0) {
				this.sendDiscordStatusMessage(t.get("RESTART_PLAYER_LAST", botLang));
				JavaCordBot.api.updateActivity("Restarting...");
				restart();
			} else if (playersLeft > 1) {
				this.broadcastMessage("BC_PLAYER_REMAIN", playersLeft);
			}
		}
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerRemoveObject(PlayerRemoveObjectEvent event) {
		boolean pickupable = event.getObjectDefinition().pickupable;
		String name = event.getObjectDefinition().name;
		Player player = event.getPlayer();
		int posX = event.getChunkPositionX();
		int posZ = event.getChunkPositionZ();
		String posMap = ((int) posX) + (posX > 0 ? "W" : "E") + " " + ((int) posZ) + (posZ > 0 ? "N" : "S");
		if (!pickupable || !trackPickupables)
			return;
		String msg = t.get("EVENT_OBJECT_REMOVE", botLang)
				.replace("PH_PLAYER", player.getName())
				.replace("PH_OBJECT_NAME", name)
				.replace("PH_LOCATION", player.getPosition().toString().replaceAll("[,()]", ""))
				.replace("PH_MAP_COORDINATES", posMap);
		eventLogger().info(msg);
		this.sendDiscordEventMessage(msg);
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerDestroyObject(PlayerDestroyObjectEvent event) {
		boolean pickupable = event.getObjectDefinition().pickupable;
		String name = event.getObjectDefinition().name;
		Player player = event.getPlayer();
		int posX = event.getChunkPositionX();
		int posZ = event.getChunkPositionZ();
		String posMap = ((int) posX) + (posX > 0 ? "W" : "E") + " " + ((int) posZ) + (posZ > 0 ? "N" : "S");
		if (!pickupable || !trackPickupables)
			return;
		String msg = t.get("EVENT_OBJECT_DESTROY", botLang)
				.replace("PH_PLAYER", player.getName())
				.replace("PH_OBJECT_NAME", name)
				.replace("PH_LOCATION", player.getPosition().toString().replaceAll("[,()]", ""))
				.replace("PH_MAP_COORDINATES", posMap);
		eventLogger().warn(msg);
		this.sendDiscordEventMessage(msg);
	}

	/**
	 * track mount and non aggressive animal deaths
	 *
	 * @param event
	 */
	@EventMethod
	public void onNpcDeath(NpcDeathEvent event) {
		// Cause.KilledByPlayer);
		Npc npc = event.getNpc();
		String name = npc.getName();
		String npcClass = npc.getDefinition().name;
		Vector3f pos = event.getDeathPosition();
		String posMap = ((int) pos.x) + (pos.x > 0 ? "W" : "E") + " " + ((int) pos.z) + (pos.z > 0 ? "N" : "S");

		String replacementNPCNameString = (name != null) ? name : "Unnamed NPC";
		String replacementNPCClassString = (npcClass != null) ? npcClass : "Unknown class";
		String replacementLocatioString = (pos != null) ? pos.toString() : "x x x (N/A)";
		String replacementMapCoordinates = (posMap != null) ? posMap : "xW xN";

		if (event.getCause() != Cause.KilledByPlayer) {
			eventLogger().debug(
					"NPC <" + replacementNPCNameString + "> <" + replacementNPCClassString + "> died from "
							+ event.getCause() + " at "
							+ replacementLocatioString + " (" + replacementMapCoordinates + ")");
			return;
		}
		Player player = (Player) event.getKiller();

		if (npc.getTypeID() == Npcs.Type.Mount.value && trackMountKill) {
			// a mount was killed
			String msg = t.get("EVENT_KILL_MOUNT", botLang)
					.replace("PH_PLAYER", player.getName())
					.replace("PH_NPC_NAME", replacementNPCNameString)
					.replace("PH_NPC_CLASS", replacementNPCClassString)
					.replace("PH_LOCATION", replacementLocatioString)
					.replace("PH_MAP_COORDINATES", replacementMapCoordinates);
			eventLogger().warn(msg);
			this.sendDiscordEventMessage(msg);
			return;
		} else if (npc.getTypeID() == Npcs.Type.Animal.value && trackNonHostileAnimalKill
				&& npc.getDefinition().behaviour.value != Behaviour.Aggressive.value) {
			// Non agressive animal was killed
			String msg = t.get("EVENT_KILL_ANIMAL", botLang)
					.replace("PH_PLAYER", player.getName())
					.replace("PH_NPC_NAME", replacementNPCNameString)
					.replace("PH_NPC_CLASS", replacementNPCClassString)
					.replace("PH_LOCATION", replacementLocatioString)
					.replace("PH_MAP_COORDINATES", replacementMapCoordinates);
			eventLogger().warn(msg);
			this.sendDiscordEventMessage(msg);
			return;
		}
		eventLogger().debug(
				player.getName()
						+ " killed NPC <name: " + replacementNPCNameString + "> <class:" + replacementNPCClassString
						+ "> <typeId: " + npc.getTypeID() + "> <variant: " + npc.getVariant() + "> at "
						+ replacementLocatioString + " (" + replacementMapCoordinates + ")");

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

		if (overrideAvatar) {
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

					if (overrideAvatar) {
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
	public void sendDiscordChatMessage(String username, String text) {
		if (webHookChatUrl != null && webHookChatUrl.toString() != "")
			this.sendDiscordMessageToWebHook(username, text, webHookChatUrl, null);
		else if (chatChannelId != 0 && JavaCordBot.api != null) {
			Optional<Channel> channel = JavaCordBot.api.getChannelById(chatChannelId);
			if (!channel.isEmpty()) {
				ServerTextChannel tc = channel.get().asServerTextChannel().get();
				tc.sendMessage(discordChatSyntax.replace("**PH_PLAYER**", username).replace("**PH_MESSAGE**", text))
						.join();
				logger().debug("Sent message to #" + tc.getName() + ": " + text);
			} else {
				logger().error("❌ Channel of chatChannelId not found: " + chatChannelId);
			}
		} else {
			logger().error("❌ Unable to send chat message: " + text);
		}
	}

	/**
	 * 
	 * @param username
	 * @param text
	 * @param image
	 */
	public void sendDiscordChatMessage(String username, String text, byte[] image) {
		if (webHookChatUrl != null && webHookChatUrl.toString() != "")
			this.sendDiscordMessageToWebHook(username, text, webHookChatUrl, image);
		else if (chatChannelId != 0 && JavaCordBot.api != null) {
			Optional<Channel> channel = JavaCordBot.api.getChannelById(chatChannelId);
			if (!channel.isEmpty()) {
				ServerTextChannel tc = channel.get().asServerTextChannel().get();
				try {
					tc.sendMessage(discordChatSyntax.replace("**PH_PLAYER**", username).replace("**PH_MESSAGE**", text),
							Utils.byteArrayToFile(image, "screenshot.jpg"))
							.join();
					logger().debug("Sent message to #" + tc.getName() + ": " + text);
				} catch (Exception e) {
					logger().error(e.toString());
					tc.sendMessage(discordChatSyntax.replace("**PH_PLAYER**", username).replace("**PH_MESSAGE**", text))
							.join();
					e.printStackTrace();
				}
			} else {
				logger().error("Channel of chatChannelId not found: " + chatChannelId);
			}
		} else {
			logger().error("❌ Unable to send chat message: " + text);
		}
	}

	/**
	 *
	 * @param username
	 * @param text
	 * @param channel
	 */
	private void sendDiscordSupportMessage(String username, String text) {
		if (webHookSupportUrl != null && webHookSupportUrl.toString() != "")
			this.sendDiscordMessageToWebHook(username, text, webHookSupportUrl, null);
		else if (supportChannelId != 0 && JavaCordBot.api != null) {
			Optional<Channel> channel = JavaCordBot.api.getChannelById(supportChannelId);
			if (!channel.isEmpty()) {
				ServerTextChannel tc = channel.get().asServerTextChannel().get();
				tc.sendMessage(text).join();
				logger().debug("Sent message to #" + tc.getName() + ": " + text);
			} else {
				logger().error("Channel of supportChannelId not found: " + supportChannelId);
			}
		} else {
			logger().error("❌ Unable to send support message: " + text);
		}
	}

	/**
	 * 
	 * @param username
	 * @param text
	 * @param image
	 */
	private void sendDiscordSupportMessage(String username, String text, byte[] image) {
		if (webHookSupportUrl != null && webHookSupportUrl.toString() != "")
			this.sendDiscordMessageToWebHook(username, text, webHookSupportUrl, image);
		else if (supportChannelId != 0 && JavaCordBot.api != null) {
			Optional<Channel> channel = JavaCordBot.api.getChannelById(supportChannelId);
			if (!channel.isEmpty()) {
				ServerTextChannel tc = channel.get().asServerTextChannel().get();
				try {
					tc.sendMessage(discordChatSyntax.replace("**PH_PLAYER**", username).replace("**PH_MESSAGE**", text),
							Utils.byteArrayToFile(image, "screenshot.jpg"))
							.join();
					logger().debug("Sent message to #" + tc.getName() + ": " + text);

				} catch (Exception e) {
					logger().error(e.toString());
					tc.sendMessage(discordChatSyntax.replace("**PH_PLAYER**", username).replace("**PH_MESSAGE**", text))
							.join();
					logger().debug("Sent message to #" + tc.getName() + ": " + text);
					e.printStackTrace();
				}
			} else {
				logger().error("❌ Channel of supportChannelId not found: " + supportChannelId);
			}
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
		if (webHookStatusUrl != null && webHookStatusUrl.toString() != "")
			this.sendDiscordMessageToWebHook(getStatusUserName(), text, webHookStatusUrl, null);
		else if (statusChannelId != 0 && JavaCordBot.api != null) {
			Optional<Channel> channel = JavaCordBot.api.getChannelById(statusChannelId);
			if (!channel.isEmpty()) {
				ServerTextChannel tc = channel.get().asServerTextChannel().get();
				tc.sendMessage(text).join();
				logger().debug("Sent message to #" + tc.getName() + ": " + text);
			} else {
				logger().error("Channel of statusChannelId not found: " + statusChannelId);
			}
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
		if (webHookEventUrl != null && webHookEventUrl.toString() != "")
			this.sendDiscordMessageToWebHook(getStatusUserName(), text, webHookEventUrl, null);
		else if (eventChannelId != 0 && JavaCordBot.api != null) {
			Optional<Channel> channel = JavaCordBot.api.getChannelById(eventChannelId);
			if (!channel.isEmpty()) {
				ServerTextChannel tc = channel.get().asServerTextChannel().get();
				tc.sendMessage(text).join();
			} else {
				logger().error("Channel of eventChannelId not found: " + eventChannelId);
			}
		} else {
			logger().error("❌ Unable to send event message: " + text);
		}
	}

	private void initSettings() {
		initSettings((getPath() != null ? getPath() : ".") + "/settings.properties");
	}

	private void initSettings(String filePath) {
		Path settingsFile = Paths.get(filePath);
		Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.properties");

		try {
			if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile)) {
				logger().info("settings.properties not found, copying from settings.default.properties...");
				Files.copy(defaultSettingsFile, settingsFile);
			}

			Properties settings = new Properties();
			if (Files.exists(settingsFile)) {
				try (FileInputStream in = new FileInputStream(settingsFile.toFile())) {
					settings.load(new InputStreamReader(in, "UTF8"));
				}
			} else {
				logger().warn(
						"⚠️ Neither settings.properties nor settings.default.properties found. Using default values.");
			}
			// fill global values
			// logLevel = Integer.parseInt(settings.getProperty("logLevel", "0"));
			postChat = settings.getProperty("postChat", "false").contentEquals("true");
			joinDiscord = settings.getProperty("joinDiscord", "");
			overrideAvatar = settings.getProperty("overrideAvatar", "true").contentEquals("true");

			postStatus = settings.getProperty("postStatus", "false").contentEquals("true");
			reportServerStatus = settings.getProperty("reportServerStatus", "true").contentEquals("true");
			reportSettingsChanged = settings.getProperty("reportSettingsChanged", "true").contentEquals("true");
			reportJarChanged = settings.getProperty("reportJarChanged", "true").contentEquals("true");
			statusUsername = settings.getProperty("statusUsername", "");
			useServerName = settings.getProperty("useServerName", "false").contentEquals("true");

			postSupport = settings.getProperty("postSupport", "false").contentEquals("true");
			supportScreenshot = settings.getProperty("supportScreenshot", "true").contentEquals("true");
			addTeleportCommand = settings.getProperty("addTeleportCommand", "true").contentEquals("true");

			botChatChannelName = settings.getProperty("botChatChannelName", "server-chat");
			botEnable = settings.getProperty("botEnable", "false").contentEquals("true");
			botSecure = settings.getProperty("botSecure", "true").contentEquals("true");
			botToken = settings.getProperty("botToken", "");
			botLang = settings.getProperty("botLang", "en");
			botAdmins = settings.getProperty("botAdmins", "");

			// discord bot commands
			// simple commands with no options
			discordCommands.put("help", Short.parseShort(settings.getProperty("botCMDhelp", "1")));
			discordCommands.put("online", Short.parseShort(settings.getProperty("botCMDonline", "1")));
			discordCommands.put("getversion", Short.parseShort(settings.getProperty("botCMDversion", "1")));
			discordCommands.put("getweather", Short.parseShort(settings.getProperty("botCMDweather", "1")));
			discordCommands.put("gettime", Short.parseShort(settings.getProperty("botCMDtime", "1")));
			discordCommands.put("getbanned", Short.parseShort(settings.getProperty("botCMDbanned", "1")));
			discordCommands.put("restart", Short.parseShort(settings.getProperty("botCMDrestart", "2")));
			// commands with options
			discordCommands.put("support", Short.parseShort(settings.getProperty("botCMDsupport", "2")));
			discordCommands.put("kick", Short.parseShort(settings.getProperty("botCMDkick", "2")));
			discordCommands.put("ban", Short.parseShort(settings.getProperty("botCMDban", "2")));
			discordCommands.put("group", Short.parseShort(settings.getProperty("botCMDgroup", "2")));
			discordCommands.put("yell", Short.parseShort(settings.getProperty("botCMDyell", "2")));
			discordCommands.put("broadcast", Short.parseShort(settings.getProperty("botCMDbroadcast", "2")));
			discordCommands.put("bc", Short.parseShort(settings.getProperty("botCMDbc", "2")));
			discordCommands.put("unban", Short.parseShort(settings.getProperty("botCMDunban", "2")));
			discordCommands.put("teleporttoplayer", Short.parseShort(settings.getProperty("botCMDtptp", "2")));
			discordCommands.put("makeadmin", Short.parseShort(settings.getProperty("botCMDmkadmin", "2")));
			discordCommands.put("unadmin", Short.parseShort(settings.getProperty("botCMDunadmin", "2")));
			discordCommands.put("setweather", Short.parseShort(settings.getProperty("botCMDsetweather", "2")));
			discordCommands.put("settime", Short.parseShort(settings.getProperty("botCMDsettime", "2")));
			discordCommands.put("sethealth", Short.parseShort(settings.getProperty("botCMDsethealth", "2")));
			discordCommands.put("sethunger", Short.parseShort(settings.getProperty("botCMDsethunger", "2")));
			discordCommands.put("setthirst", Short.parseShort(settings.getProperty("botCMDsetthirst", "2")));
			discordCommands.put("reloadplugins", Short.parseShort(settings.getProperty("botCMDreloadplugins", "2")));
			// badass stuff
			postTrackedEvents = settings.getProperty("postTrackedEvents", "false").contentEquals("true");
			trackMountKill = settings.getProperty("trackMountKill", "false").contentEquals("true");
			trackNonHostileAnimalKill = settings.getProperty("trackNonHostileAnimalKill", "false")
					.contentEquals("true");
			trackPickupables = settings.getProperty("trackPickupables", "false").contentEquals("true");
			trackPlayerDeaths = settings.getProperty("trackPlayerDeaths", "false").contentEquals("true");
			trackPlayerTeleports = settings.getProperty("trackPlayerTeleports", "false").contentEquals("true");
			trackWeatherChanges = settings.getProperty("trackWeatherChanges", "false").contentEquals("true");
			trackSeasonChanges = settings.getProperty("trackSeasonChanges", "false").contentEquals("true");

			// colors

			colorizeChat = settings.getProperty("colorizeChat", "true").contentEquals("true");
			showGroup = settings.getProperty("showGroup", "false").contentEquals("true");
			colorSupport = settings.getProperty("colorSupport", "<color=#782d8e>");
			colorLocalSelf = settings.getProperty("colorLocalSelf", "<color=#ddffdd>");
			colorLocalAdmin = settings.getProperty("colorLocalAdmin", "<color=#db3208>");
			colorLocalOther = settings.getProperty("colorLocalOther", "<color=#dddddd>");
			colorLocalDiscord = settings.getProperty("colorLocalDiscord", "<color=#ddddff>");
			defaultChatPrefix = settings.getProperty("defaultChatPrefix", "[LOCAL] ");
			discordChatSyntax = settings.getProperty("discordChatSyntax", "[chat] **PH_PLAYER**: **PH_MESSAGE**");

			// screenshots
			allowScreenshots = settings.getProperty("allowScreenshots", "true").contentEquals("true");
			maxScreenWidth = Integer.parseInt(settings.getProperty("maxScreenWidth", "1920"));

			// motd settings
			sendPluginWelcome = settings.getProperty("sendPluginWelcome", "false").contentEquals("true");

			// restart settings
			restartTimed = settings.getProperty("restartTimed", "false").contentEquals("true");
			allowRestart = settings.getProperty("allowRestart", "false").contentEquals("true");
			restartAdminOnly = settings.getProperty("restartAdminOnly", "false").contentEquals("true");
			// "false").contentEquals("true");
			restartMinimumTime = Integer.parseInt(settings.getProperty("restartMinimumTime", "86400"));
			forceRestartAfter = Integer.parseInt(settings.getProperty("forceRestartAfter", "0"));

			// parse next restart time (we only need the next beacause we have to lookup
			// again after restart)
			String restartTimesString = settings.getProperty("restartTimes", "00:00");

			// WebhookUrls
			webHookChatUrl = new URI(settings.getProperty("webHookChatUrl", ""));
			webHookStatusUrl = new URI(settings.getProperty("webHookStatusUrl", ""));
			webHookSupportUrl = new URI(settings.getProperty("webHookSupportUrl", ""));
			webHookEventUrl = new URI(settings.getProperty("webHookEventUrl", ""));
			// ChannelIds
			eventChannelId = Long.parseLong(settings.getProperty("eventChannelId", "0"));
			chatChannelId = Long.parseLong(settings.getProperty("chatChannelId", "0"));
			statusChannelId = Long.parseLong(settings.getProperty("statusChannelId", "0"));
			supportChannelId = Long.parseLong(settings.getProperty("supportChannelId", "0"));

			// restartTimesString, 10);
			if (restartTimed) {
				String[] restartTimes = restartTimesString.split("\\|");
				initRestartSchedule(restartTimes);
			}

			logger().info(this.getName() + " Plugin settings loaded");

			logger().info("Will send chat to Discord: " + String.valueOf(postChat));
			logger().info("Will send status to Discord: " + String.valueOf(postStatus));
			logger().info("Will send support tickets to Discord: " + String.valueOf(postSupport));
			logger().info("Sending welcome message on login is: " + String.valueOf(sendPluginWelcome));

		} catch (IOException ex) {
			logger().error("IOException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		} catch (NumberFormatException ex) {
			logger().error("NumberFormatException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		} catch (URISyntaxException ex) {
			logger().error("URISyntaxException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
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
						broadcastMessage("RS_SCHEDULE_INFO");
						flagRestart = true;
						if (DiscordWebHook.instance != null)
							DiscordWebHook.instance.statusNotification("STATUS_RESTART_FLAG");
						if (forceRestartAfter > 0) {
							broadcastMessage("RS_SCHEDULE_WARN", forceRestartAfter);
						}
					} else {
						logger().info("Restarting server now (scheduled)");
						if (DiscordWebHook.instance != null)
							DiscordWebHook.instance.statusNotification("STATUS_RESTART_SCHEDULED");
						JavaCordBot.api.updateActivity("Restarting soon...").join();
						restart();
					}
				}
			};

			restartTimer.schedule(restartTask, cal.getTime());

			// force restarting
			if (forceRestartAfter > 0) {
				if (restartForcedTask != null) {
					restartForcedTask.cancel();
				}

				restartForcedTask = new TimerTask() {
					@Override
					public void run() {
						logger().warn("Force server restart now!");
						JavaCordBot.api.updateActivity("Restarting soon...").join();
						for (Player player : Server.getAllPlayers()) {
							player.kick("Server restart");
						}
						forceRestart();
					}
				};

				cal.set(MINUTE, nextMinute + forceRestartAfter);
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
		if (reportSettingsChanged) {
			this.sendDiscordStatusMessage(t.get("UPDATE_SETTINGS", botLang));
		}
		initSettings(settingsPath.toString());
		this.initialize();
	}

	public static void forceRestart() {
		Server.saveAll();

		if (instance == null) {
			logger().error("DiscordWebHook instance is null, cannot execute forceRestart()");
			return;
		}

		instance.sendDiscordStatusMessage("STATUS_RESTART_FOCED");
		instance.executeDelayed(5, () -> {
			Server.sendInputCommand("restart");
		});
	}

	public static void restart() {
		Server.saveAll();

		if (instance == null) {
			logger().error("DiscordWebHook instance is null, cannot execute restart()");
			return;
		}

		instance.executeDelayed(5, () -> {
			Server.sendInputCommand("restart");
		});
	}

	public void statusNotification(String message) {
		if (reportServerStatus) {
			String messageText = t.get(message, botLang)
					.replace("PH_PLUGIN_NAME", this.getDescription("name"))
					.replace("PH_PLUGIN_VERSION", this.getDescription("version"))
					.replace("PH_PLAYER_COUNT", Server.getPlayerCount() + "");
			// Sende Statusnachricht nur, wenn der Bot aktiviert und verbunden ist
			if (botEnable && JavaCordBot.api != null && JavaCordBot.api.getStatus() == UserStatus.ONLINE) {

				this.sendDiscordStatusMessage(messageText);
			} else {
				logger().warn("Could not send: " + messageText);
			}
		}
	}
}
