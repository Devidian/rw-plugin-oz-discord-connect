package de.omegazirkel.risingworld.discordconnect;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.tools.OZLogger;

public class PluginSettings {
	private static PluginSettings instance = null;

	private static DiscordConnect plugin;

	private static OZLogger logger() {
		return OZLogger.getInstance("OZ.DiscordConnect.Settings");
	}

	// Settings
	public String logLevel = "ALL";
	public String joinDiscord = "";
	public boolean botEnable = false;
	public boolean sendPluginWelcome = false;
	public String botToken = "";
	public String botAdmins = "";
	public String botLang = "en";

	// Discord chat settings
	public boolean postChat = false;
	public boolean overrideAvatar = true;
	public URI webHookChatUrl = null;
	public long chatChannelId = 0;
	public String botChatChannelName = "server-chat";

	// Discord support settings
	public boolean postSupport = false;
	public boolean supportScreenshot = true;
	public boolean addTeleportCommand = true;
	public URI webHookSupportUrl = null;
	public long supportChannelId = 0;

	// Discord status settings
	public boolean postStatus = false;
	public boolean useServerName = false;
	public boolean reportServerStatus = false;
	public boolean reportSettingsChanged = true;
	public boolean reportJarChanged = true;
	public String statusUsername = "My Server";
	public URI webHookStatusUrl = null;
	public long statusChannelId = 0;

	// Discord event settings
	public boolean postTrackedEvents = false;
	public boolean trackMountKill = false;
	public boolean trackNonHostileAnimalKill = false;
	public boolean trackPickupables = false;
	public boolean trackPlayerDeaths = false;
	public boolean trackPlayerTeleports = false;
	public boolean trackWeatherChanges = false;
	public boolean trackSeasonChanges = false;
	public URI webHookEventUrl = null;
	public long eventChannelId = 0;

	// Discord SlashCommands settings
	public boolean botSecure = true;
	public boolean restartAdminOnly = true;
	public boolean allowRestart = false;

	// other settings
	// reloadallplugins
	public int restartMinimumTime = 86400;// (60 * 60 * 24); // 1 Day default
	public boolean restartTimed = false; // restart schedule
	public int forceRestartAfter = 5; // Minutes

	public boolean allowScreenshots = true;
	public int maxScreenWidth = 1920;

	public boolean colorizeChat = true;
	public boolean showGroup = false;
	public String colorSupport = "<color=#782d8e>";
	public String colorLocalSelf = "<color=#ddffdd>";
	public String colorLocalAdmin = "<color=#db3208>";
	public String colorLocalOther = "<color=#dddddd>";
	public String colorLocalDiscord = "<color=#ddddff>";
	public String defaultChatPrefix = "[LOCAL] ";
	public String discordChatSyntax = "[chat] **PH_PLAYER**: **PH_MESSAGE**";

	public String restartTimesString = "";

	public Map<String, Short> discordCommands = new HashMap<>();

	// END Settings

	public static PluginSettings getInstance(DiscordConnect p) {
		plugin = p;
		return getInstance();
	}

	public static PluginSettings getInstance() {

		if (instance == null) {
			instance = new PluginSettings();
		}
		return instance;
	}

	private PluginSettings() {
	}

	public void initSettings() {
		initSettings((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
	}

	public void initSettings(String filePath) {
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
			logLevel = settings.getProperty("logLevel", "ALL");
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
			restartTimesString = settings.getProperty("restartTimes", "00:00");

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

			logger().info(plugin.getName() + " Plugin settings loaded");

			logger().info("Will send chat to Discord: " + String.valueOf(postChat));
			logger().info("Will send status to Discord: " + String.valueOf(postStatus));
			logger().info("Will send support tickets to Discord: " + String.valueOf(postSupport));
			logger().info("Sending welcome message on login is: " + String.valueOf(sendPluginWelcome));
			logger().info("Loglevel is set to " + logLevel);
			logger().setLevel(logLevel);
			DiscordConnect.logger().setLevel(logLevel);
			DiscordConnect.eventLogger().setLevel(logLevel);

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
}
