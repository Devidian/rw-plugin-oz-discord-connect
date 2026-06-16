package de.omegazirkel.risingworld.discordconnect;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.StringJoiner;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.tools.Colors;
import net.risingworld.api.Server;
import net.risingworld.api.database.WorldDatabase.Target;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.definitions.WeatherDefs;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.Time.Unit;

public final class DiscordCommandService {
    private final DiscordConnect plugin;
    private final PluginSettings settings;
    private final Colors colors = Colors.getInstance();

    public DiscordCommandService(DiscordConnect plugin) {
        this.plugin = plugin;
        this.settings = PluginSettings.getInstance();
    }

    public DiscordCommandResult execute(DiscordCommandRequest request) {
        String denial = authorizationError(request);
        if (denial != null) {
            return text(request, denial);
        }
        try {
            return switch (request.command()) {
                case "getversion" -> text(request, "Plugin version: " + plugin.getDescription("version")
                        + "\nGame version: " + plugin.getGameVersion());
                case "online" -> online(request);
                case "help" -> longText(request, plugin.getTranslator().get("TC_DISCORD_HELP_SHORT", settings.botLang),
                        "discord-help.txt");
                case "getbanned" -> banned(request);
                case "gettime" -> gameTime(request);
                case "getweather" -> weather(request);
                case "ban" -> ban(request);
                case "unban" -> unban(request);
                case "kick" -> kick(request);
                case "makeadmin" -> setAdmin(request, true);
                case "unadmin" -> setAdmin(request, false);
                case "group" -> group(request);
                case "sethealth" -> setStat(request, "health");
                case "sethunger" -> setStat(request, "hunger");
                case "setthirst" -> setStat(request, "thirst");
                case "settime" -> setTime(request);
                case "setweather" -> setWeather(request);
                case "support" -> support(request);
                case "teleporttoplayer" -> teleport(request);
                case "broadcast" -> broadcast(request, false);
                case "yell" -> broadcast(request, true);
                case "restart" -> restart(request);
                case "reloadplugins" -> reloadPlugins(request);
                default -> text(request, "Unknown command: " + request.command());
            };
        } catch (Exception ex) {
            DiscordConnect.logger().error("Discord command " + request.command() + " failed: " + ex.getMessage());
            return text(request, "Command failed: " + ex.getMessage());
        }
    }

    private String authorizationError(DiscordCommandRequest request) {
        Short level = settings.discordCommands.get(request.command());
        if (level == null) {
            return "No permission level configured for /" + request.command();
        }
        if (level == 0) {
            return "Command /" + request.command() + " is disabled";
        }
        if (level > 1 && settings.botSecure && !request.admin()) {
            return "Command /" + request.command() + " is restricted to bot administrators";
        }
        return null;
    }

    private DiscordCommandResult online(DiscordCommandRequest request) {
        if (Server.getPlayerCount() == 0) {
            return text(request, plugin.getTranslator().get("TC_CMD_OUT_ONLINE_NOBODY", settings.botLang));
        }
        StringJoiner players = new StringJoiner("\n",
                plugin.getTranslator().get("TC_CMD_OUT_ONLINE_LIST", settings.botLang) + "\n", "");
        for (Player player : Server.getAllPlayers()) {
            players.add(player.getName() + " uid:" + player.getUID() + " g:" + player.getPermissionGroup()
                    + (player.isAdmin() ? " [A]" : ""));
        }
        return longText(request, players.toString(), "online.txt");
    }

    private DiscordCommandResult banned(DiscordCommandRequest request) throws Exception {
        StringJoiner resultText = new StringJoiner("\n",
                plugin.getTranslator().get("TC_CMD_OUT_BANNED_LIST", settings.botLang) + "\n", "");
        try (ResultSet result = plugin.getWorldDatabase(Target.Bans).executeQuery("SELECT * FROM `Banlist`")) {
            while (result.next()) {
                resultText.add(result.getString(2) + " (" + result.getLong(1) + ") banned for: " + result.getString(7));
            }
        }
        return longText(request, resultText.toString(), "banned-players.txt");
    }

    private DiscordCommandResult gameTime(DiscordCommandRequest request) {
        String season = plugin.getTranslator().get(
                "TC_SEASON_" + Server.getCurrentSeason().toString().toUpperCase(), settings.botLang);
        return text(request, plugin.getTranslator().get("TC_CMD_OUT_TIME", settings.botLang)
                .replace("PH_TIME", Server.getGameTime(Unit.Hours) + ":" + Server.getGameTime(Unit.Minutes))
                .replace("PH_SEASON", season)
                .replace("PH_YEAR", String.valueOf(Server.getGameTime(Unit.Years)))
                .replace("PH_DAY", String.valueOf(Server.getGameTime(Unit.Days))));
    }

    private DiscordCommandResult weather(DiscordCommandRequest request) {
        WeatherDefs.Weather current = Server.getCurrentWeather();
        return text(request, plugin.getTranslator().get("TC_CMD_OUT_WEATHER", settings.botLang)
                .replace("PH_WEATHER", plugin.getTranslator().get(
                        "TC_WEATHER_" + current.name.toUpperCase(), settings.botLang)));
    }

    private DiscordCommandResult ban(DiscordCommandRequest request) {
        Player player = requestedPlayer(request);
        if (player == null) {
            return text(request, "Player must be online and selected by playername or playerid64");
        }
        player.ban(option(request, "reason", "No reason"), integer(request, "duration", 604800));
        return text(request, "Banned " + player.getName());
    }

    private DiscordCommandResult unban(DiscordCommandRequest request) {
        String player = option(request, "playername", option(request, "playerid64", ""));
        if (player.isBlank()) {
            return text(request, "playername or playerid64 must be set");
        }
        Server.unbanPlayer(player);
        return text(request, "Unbanned " + player);
    }

    private DiscordCommandResult kick(DiscordCommandRequest request) {
        Player player = requestedPlayer(request);
        if (player == null) {
            return text(request, "Player is not online");
        }
        player.kick(option(request, "reason", "No reason"));
        return text(request, "Kicked " + player.getName());
    }

    private DiscordCommandResult setAdmin(DiscordCommandRequest request, boolean admin) {
        Player player = requestedPlayer(request);
        if (player == null) {
            return text(request, "Player is not online");
        }
        player.setAdmin(admin);
        return text(request, admin ? "Admin rights granted" : "Admin rights removed");
    }

    private DiscordCommandResult group(DiscordCommandRequest request) {
        Player player = requestedPlayer(request);
        String group = option(request, "groupname", "");
        if (player == null || group.isBlank()) {
            return text(request, "playername and groupname must be set");
        }
        player.setPermissionGroup(group);
        return text(request, "Set " + player.getName() + " group to " + group);
    }

    private DiscordCommandResult setStat(DiscordCommandRequest request, String stat) {
        Player player = requestedPlayer(request);
        if (player == null) {
            return text(request, "Player is not online");
        }
        int value = integer(request, "intvalue", 100);
        switch (stat) {
            case "health" -> player.setHealth(value);
            case "hunger" -> player.setHunger(value);
            case "thirst" -> player.setThirst(value);
            default -> throw new IllegalArgumentException("Unknown stat " + stat);
        }
        return text(request, "Set " + player.getName() + " " + stat + " to " + value);
    }

    private DiscordCommandResult setTime(DiscordCommandRequest request) {
        int hour = integer(request, "hourvalue", 0);
        int minute = integer(request, "minutevalue", 0);
        Server.setGameTime(hour, minute);
        return text(request, "Game time set to " + hour + ":" + minute);
    }

    private DiscordCommandResult setWeather(DiscordCommandRequest request) {
        String weather = option(request, "weathername", "");
        WeatherDefs.Weather definition = Definitions.getWeather(weather);
        if (definition == null) {
            return text(request, "Unknown weather: " + weather);
        }
        Server.setWeather(definition, false);
        return text(request, "Weather set to " + weather);
    }

    private DiscordCommandResult support(DiscordCommandRequest request) {
        Player player = requestedPlayer(request);
        String message = option(request, "text", "");
        if (player == null || message.isBlank()) {
            return text(request, "playername and text must be set");
        }
        player.sendTextMessage(settings.colorSupport + "[SUPPORT] " + request.displayName() + ": "
                + colors.endTag + message);
        return text(request, "Message sent to " + player.getName());
    }

    private DiscordCommandResult teleport(DiscordCommandRequest request) {
        Player player = requestedPlayer(request);
        Player target = Server.getPlayerByName(option(request, "targetplayername", ""));
        if (player == null || target == null) {
            return text(request, "Both players must be online");
        }
        player.setPosition(target.getPosition());
        return text(request, "Teleported " + player.getName() + " to " + target.getName());
    }

    private DiscordCommandResult broadcast(DiscordCommandRequest request, boolean yell) {
        String message = settings.colorSupport + "[" + option(request, "channel", "local") + "] "
                + request.displayName() + ": " + colors.endTag + option(request, "text", "Hello World!");
        if (yell) {
            Server.broadcastYellMessage(message, 10, false);
        } else {
            Server.broadcastTextMessage(message);
        }
        return text(request, "Message sent");
    }

    private DiscordCommandResult restart(DiscordCommandRequest request) {
        if (Server.getPlayerCount() == 0) {
            DiscordConnect.restart();
            return text(request, "Server restart initiated");
        }
        plugin.setFlagRestart(true);
        return text(request, "Restart queued until all players disconnect");
    }

    private DiscordCommandResult reloadPlugins(DiscordCommandRequest request) {
        plugin.executeDelayed(5, () -> Server.sendInputCommand("reloadplugins"));
        return text(request, "Plugins reloading...");
    }

    private Player requestedPlayer(DiscordCommandRequest request) {
        String name = option(request, "playername", "");
        if (!name.isBlank()) {
            return Server.getPlayerByName(name);
        }
        String id = option(request, "playerid64", "");
        return id.isBlank() ? null : Server.getPlayerByUID(id);
    }

    private static String option(DiscordCommandRequest request, String name, String fallback) {
        return request.options().getOrDefault(name, fallback);
    }

    private static int integer(DiscordCommandRequest request, String name, int fallback) {
        String value = request.options().get(name);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static DiscordCommandResult text(DiscordCommandRequest request, String content) {
        return DiscordCommandResult.text(request.responseToken(), content);
    }

    private static DiscordCommandResult longText(DiscordCommandRequest request, String content, String fileName) {
        if (content.length() <= 1900) {
            return text(request, content);
        }
        return DiscordCommandResult.textFile(request.responseToken(), "Result attached", fileName,
                content.getBytes(StandardCharsets.UTF_8));
    }
}
