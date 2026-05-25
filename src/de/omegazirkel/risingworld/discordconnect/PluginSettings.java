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
import de.omegazirkel.risingworld.tools.settings.AdminSettingsEntry;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsType;
import de.omegazirkel.risingworld.tools.settings.SettingsFileEditor;

public class PluginSettings {
	private static PluginSettings instance = null;

	private static DiscordConnect plugin;

	private static OZLogger logger() {
		return DiscordConnect.logger();
	}

	// Settings
	public String logLevel = "ALL";
	public boolean reloadOnChange = true;
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
	// public boolean postStatus = false;
	public boolean useServerName = false;
	public boolean reportServerStatus = false;
	public boolean reportSettingsChanged = true;
	public boolean reportJarChanged = true;
	public String statusUsername = "My Server";
	public URI webHookStatusUrl = null;
	public long statusChannelId = 0;

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
	public boolean useShutdownNotRestart = true;

	// screenshots

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
			reloadOnChange = settings.getProperty("reloadOnChange", "true").contentEquals("true");
			postChat = settings.getProperty("postChat", "false").contentEquals("true");
			joinDiscord = settings.getProperty("joinDiscord", "");
			overrideAvatar = settings.getProperty("overrideAvatar", "true").contentEquals("true");

			// postStatus = settings.getProperty("postStatus", "false").contentEquals("true");
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
			// postTrackedEvents = settings.getProperty("postTrackedEvents", "false").contentEquals("true");
			// trackMountKill = settings.getProperty("trackMountKill", "false").contentEquals("true");
			// trackNonHostileAnimalKill = settings.getProperty("trackNonHostileAnimalKill", "false")
			// 		.contentEquals("true");
			// trackPickupables = settings.getProperty("trackPickupables", "false").contentEquals("true");
			// trackPlayerDeaths = settings.getProperty("trackPlayerDeaths", "false").contentEquals("true");
			// trackPlayerTeleports = settings.getProperty("trackPlayerTeleports", "false").contentEquals("true");
			// trackWeatherChanges = settings.getProperty("trackWeatherChanges", "false").contentEquals("true");
			// trackSeasonChanges = settings.getProperty("trackSeasonChanges", "false").contentEquals("true");

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
			useShutdownNotRestart = settings.getProperty("useShutdownNotRestart", "true").contentEquals("true");

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
			// logger().info("Will send status to Discord: " + String.valueOf(postStatus));
			logger().info("Will send support tickets to Discord: " + String.valueOf(postSupport));
			logger().info("Sending welcome message on login is: " + String.valueOf(sendPluginWelcome));
			logger().info("Loglevel is set to " + logLevel);
			logger().setLevel(logLevel);

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

	public java.util.List<AdminSettingsEntry> adminSettingsEntries() {
		return java.util.List.of(
				AdminSettingsEntry.group("general", "General", "Logging, reload, welcome, and invite behavior."),
				entry("logLevel", "Log level", "Controls DiscordConnect logging verbosity.", logLevel, "ALL",
						AdminSettingsType.STRING),
				entry("reloadOnChange", "Reload on change",
						"Documents that DiscordConnect settings reload when settings.properties changes.",
						reloadOnChange, "true", AdminSettingsType.BOOLEAN),
				entry("sendPluginWelcome", "Welcome message",
						"Shows a short DiscordConnect message when a player joins.", sendPluginWelcome, "false",
						AdminSettingsType.BOOLEAN),
				entry("joinDiscord", "Join Discord text", "Text or URL shown for Discord onboarding.",
						joinDiscord, "", AdminSettingsType.STRING),
				AdminSettingsEntry.group("bot", "Discord bot", "Discord bot identity, access, and command behavior."),
				entry("botEnable", "Bot enabled", "Enables the Discord bot.", botEnable, "false",
						AdminSettingsType.BOOLEAN),
				sensitiveEntry("botToken", "Bot token", "Discord bot token."),
				entry("botSecure", "Secure bot commands", "Restricts secure bot commands to configured admins.",
						botSecure, "true", AdminSettingsType.BOOLEAN),
				readOnlyEntry("botLang", "Bot language",
						"Language used for Discord bot commands because the bot cannot determine it automatically.",
						botLang, "en", AdminSettingsType.STRING),
				readOnlyEntry("botAdmins", "Bot admins",
						"Comma-separated Discord users allowed to execute secure commands.", botAdmins, "",
						AdminSettingsType.STRING),
				entry("botChatChannelName", "Bot chat channel",
						"Discord channel name from which bot chat messages are picked up ingame.",
						botChatChannelName, "server-chat", AdminSettingsType.STRING),
				AdminSettingsEntry.group("botCommands", "Bot commands",
						"Discord bot command access: 0 disabled, 1 everyone, 2 admin only."),
				entry("botCMDhelp", "Command: help", "Access level for the help command.",
						discordCommands.getOrDefault("help", (short) 1), "1", AdminSettingsType.INTEGER),
				entry("botCMDversion", "Command: version", "Access level for the version command.",
						discordCommands.getOrDefault("getversion", (short) 1), "1", AdminSettingsType.INTEGER),
				entry("botCMDonline", "Command: online", "Access level for the online command.",
						discordCommands.getOrDefault("online", (short) 1), "1", AdminSettingsType.INTEGER),
				entry("botCMDweather", "Command: weather", "Access level for the weather command.",
						discordCommands.getOrDefault("getweather", (short) 1), "1", AdminSettingsType.INTEGER),
				entry("botCMDtime", "Command: time", "Access level for the time command.",
						discordCommands.getOrDefault("gettime", (short) 1), "1", AdminSettingsType.INTEGER),
				entry("botCMDbanned", "Command: banned", "Access level for the banned command.",
						discordCommands.getOrDefault("getbanned", (short) 1), "1", AdminSettingsType.INTEGER),
				entry("botCMDrestart", "Command: restart", "Access level for the restart command.",
						discordCommands.getOrDefault("restart", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDsupport", "Command: support", "Access level for the support command.",
						discordCommands.getOrDefault("support", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDkick", "Command: kick", "Access level for the kick command.",
						discordCommands.getOrDefault("kick", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDban", "Command: ban", "Access level for the ban command.",
						discordCommands.getOrDefault("ban", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDgroup", "Command: group", "Access level for the group command.",
						discordCommands.getOrDefault("group", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDyell", "Command: yell", "Access level for the yell command.",
						discordCommands.getOrDefault("yell", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDbroadcast", "Command: broadcast", "Access level for the broadcast command.",
						discordCommands.getOrDefault("broadcast", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDbc", "Command: bc", "Access level for the bc command.",
						discordCommands.getOrDefault("bc", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDunban", "Command: unban", "Access level for the unban command.",
						discordCommands.getOrDefault("unban", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDtptp", "Command: teleport to player", "Access level for the teleport-to-player command.",
						discordCommands.getOrDefault("teleporttoplayer", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDmkadmin", "Command: make admin", "Access level for the make-admin command.",
						discordCommands.getOrDefault("makeadmin", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDunadmin", "Command: unadmin", "Access level for the unadmin command.",
						discordCommands.getOrDefault("unadmin", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDsetweather", "Command: set weather", "Access level for the set-weather command.",
						discordCommands.getOrDefault("setweather", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDsettime", "Command: set time", "Access level for the set-time command.",
						discordCommands.getOrDefault("settime", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDsethealth", "Command: set health", "Access level for the set-health command.",
						discordCommands.getOrDefault("sethealth", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDsethunger", "Command: set hunger", "Access level for the set-hunger command.",
						discordCommands.getOrDefault("sethunger", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDsetthirst", "Command: set thirst", "Access level for the set-thirst command.",
						discordCommands.getOrDefault("setthirst", (short) 2), "2", AdminSettingsType.INTEGER),
				entry("botCMDreloadplugins", "Command: reload plugins", "Access level for the reload-plugins command.",
						discordCommands.getOrDefault("reloadplugins", (short) 2), "2", AdminSettingsType.INTEGER),
				AdminSettingsEntry.group("webhooks", "Webhooks and channels",
						"Discord webhook URLs and direct channel targets."),
				sensitiveEntry("webHookEventUrl", "Event webhook", "Webhook URL for event messages."),
				sensitiveEntry("webHookStatusUrl", "Status webhook", "Webhook URL for status messages."),
				sensitiveEntry("webHookSupportUrl", "Support webhook", "Webhook URL for support messages."),
				sensitiveEntry("webHookChatUrl", "Chat webhook", "Webhook URL for chat messages."),
				entry("eventChannelId", "Event channel", "Discord channel id for event messages.", eventChannelId,
						"0", AdminSettingsType.STRING),
				entry("chatChannelId", "Chat channel", "Discord channel id for chat messages.", chatChannelId,
						"0", AdminSettingsType.STRING),
				entry("statusChannelId", "Status channel", "Discord channel id for status messages.",
						statusChannelId, "0", AdminSettingsType.STRING),
				entry("supportChannelId", "Support channel", "Discord channel id for support messages.",
						supportChannelId, "0", AdminSettingsType.STRING),
				entry("overrideAvatar", "Override avatar", "Uses player avatars for webhook messages.",
						overrideAvatar, "true", AdminSettingsType.BOOLEAN),
				entry("useServerName", "Use server name", "Uses the server name for status webhook messages.",
						useServerName, "false", AdminSettingsType.BOOLEAN),
				entry("statusUsername", "Status username", "Webhook username for status messages.", statusUsername,
						"", AdminSettingsType.STRING),
				AdminSettingsEntry.group("routing", "Message routing", "Discord chat, support, and status routing."),
				entry("postChat", "Post chat", "Forwards ingame chat to Discord.", postChat, "false",
						AdminSettingsType.BOOLEAN),
				entry("postSupport", "Post support", "Forwards support messages to Discord.", postSupport, "false",
						AdminSettingsType.BOOLEAN),
				entry("reportServerStatus", "Report server status", "Reports server status events to Discord.",
						reportServerStatus, "true", AdminSettingsType.BOOLEAN),
				entry("reportSettingsChanged", "Report settings changes", "Reports settings changes to Discord.",
						reportSettingsChanged, "true", AdminSettingsType.BOOLEAN),
				entry("reportJarChanged", "Report jar changes", "Reports plugin jar change events to Discord.",
						reportJarChanged, "true", AdminSettingsType.BOOLEAN),
				entry("supportScreenshot", "Support screenshots",
						"Adds screenshots to support messages by default.", supportScreenshot, "true",
						AdminSettingsType.BOOLEAN),
				entry("addTeleportCommand", "Support teleport command",
						"Adds a teleport command to support messages when coordinates are available.",
						addTeleportCommand, "true", AdminSettingsType.BOOLEAN),
				AdminSettingsEntry.group("restart", "Restart", "Ingame and scheduled restart behavior."),
				entry("allowRestart", "Allow restart", "Enables ingame restart commands.", allowRestart, "false",
						AdminSettingsType.BOOLEAN),
				entry("restartAdminOnly", "Restart admin only",
						"Restricts restart flag changes to admins.", restartAdminOnly, "false",
						AdminSettingsType.BOOLEAN),
				entry("restartMinimumTime", "Restart minimum time",
						"Minimum player playtime in seconds required to trigger restart.", restartMinimumTime,
						"86400", AdminSettingsType.INTEGER),
				entry("restartTimed", "Scheduled restart", "Enables scheduled restart handling.", restartTimed,
						"false", AdminSettingsType.BOOLEAN),
				readOnlyEntry("restartTimes", "Restart times",
						"Scheduled restart times separated by |.", restartTimesString, "00:00",
						AdminSettingsType.STRING),
				entry("forceRestartAfter", "Force restart after",
						"Minutes after restart request before forcing restart; 0 disables forced restart.",
						forceRestartAfter, "0", AdminSettingsType.INTEGER),
				entry("useShutdownNotRestart", "Shutdown instead of restart",
						"Sends shutdown instead of restart when enabled.", useShutdownNotRestart, "true",
						AdminSettingsType.BOOLEAN),
				AdminSettingsEntry.group("screenshots", "Screenshots", "Player screenshot forwarding behavior."),
				entry("allowScreenshots", "Allow screenshots",
						"Allows players to post screenshots through chat shortcuts.", allowScreenshots, "true",
						AdminSettingsType.BOOLEAN),
				entry("maxScreenWidth", "Max screenshot width", "Maximum screenshot width in pixels.",
						maxScreenWidth, "1920", AdminSettingsType.INTEGER),
				AdminSettingsEntry.group("chatFormat", "Chat format", "Ingame and Discord chat formatting."),
				entry("colorizeChat", "Colorize chat", "Enables colored ingame chat formatting.", colorizeChat,
						"true", AdminSettingsType.BOOLEAN),
				entry("showGroup", "Show group", "Includes group information in chat messages.", showGroup,
						"false", AdminSettingsType.BOOLEAN),
				entry("colorSupport", "Support color", "RichText color used for support messages.", colorSupport,
						"<color=#782d8e>", AdminSettingsType.STRING),
				entry("colorLocalSelf", "Local self color", "RichText color for own local chat messages.",
						colorLocalSelf, "<color=#ddffdd>", AdminSettingsType.STRING),
				entry("colorLocalAdmin", "Local admin color", "RichText color for local admin messages.",
						colorLocalAdmin, "<color=#db3208>", AdminSettingsType.STRING),
				entry("colorLocalOther", "Local other color", "RichText color for other local chat messages.",
						colorLocalOther, "<color=#dddddd>", AdminSettingsType.STRING),
				entry("colorLocalDiscord", "Local Discord color", "RichText color for Discord-origin local messages.",
						colorLocalDiscord, "<color=#ddddff>", AdminSettingsType.STRING),
				entry("defaultChatPrefix", "Default chat prefix", "Prefix used for ingame chat messages.",
						defaultChatPrefix, "[\uD83D\uDDE3\uFE0F:**PH_LANGUAGE**] ", AdminSettingsType.STRING),
				entry("discordChatSyntax", "Discord chat syntax", "Format used for chat messages sent to Discord.",
						discordChatSyntax, "[\uD83D\uDD79\uFE0F:**PH_LANGUAGE**] **PH_PLAYER**: **PH_MESSAGE**",
						AdminSettingsType.STRING));
	}

	private AdminSettingsEntry entry(String key, String label, String description, Object value, String defaultValue,
			AdminSettingsType type) {
		return new AdminSettingsEntry(
				key,
				label,
				description,
				String.valueOf(value),
				defaultValue,
				type,
				false,
				newValue -> SettingsFileEditor.writeValue(settingsPath(), key, newValue));
	}

	private AdminSettingsEntry readOnlyEntry(String key, String label, String description, Object value,
			String defaultValue, AdminSettingsType type) {
		return new AdminSettingsEntry(
				key,
				label,
				description,
				String.valueOf(value),
				defaultValue,
				type,
				false,
				null);
	}

	private AdminSettingsEntry sensitiveEntry(String key, String label, String description) {
		return new AdminSettingsEntry(key, label, description, "", "", AdminSettingsType.STRING, true, null);
	}

	private Path settingsPath() {
		return Paths.get((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
	}
}
