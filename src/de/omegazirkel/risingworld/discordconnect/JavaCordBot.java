package de.omegazirkel.risingworld.discordconnect;

import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.GloballyAttachableListener;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.listeners.DiscordChatListener;
import de.omegazirkel.risingworld.listeners.DiscordSlashCommandListener;
import de.omegazirkel.risingworld.tools.OZLogger;

public class JavaCordBot {

    public static DiscordConnect pluginInstance = null;
    public static DiscordApi api = null;
    private static boolean initialized = false;
    private static PluginSettings s = PluginSettings.getInstance();


    public static OZLogger logger() {
        return OZLogger.getInstance("OZ.DiscordConnect.JavaCord");
    }

    public JavaCordBot(final DiscordConnect plugin) {
        pluginInstance = plugin;
        logger().setLevel(s.logLevel);
    }

    public static void disconnect() {
        if (!initialized)
            return;
        logger().debug("DiscordBot is now disconnecting");
        api.updateActivity("Shutting down...");
        api.getListeners().forEach((GloballyAttachableListener entry, List<Class<GloballyAttachableListener>> list) -> {
            api.removeListener(entry);
        });
        api.disconnect().join();
        logger().info("ℹ️ DiscordBot is now disconnected!");
    }

    public void init() {
        if (initialized) {
            return; // do not execute more than once
        }
        initialized = true;
        api = new DiscordApiBuilder()
                .setToken(s.botToken)
                // .setAllIntents()
                .addIntents(Intent.MESSAGE_CONTENT)
                .login()
                .join();

        api.updateActivity("Running, waiting for players!");

        CommandRegistry.syncCommandsForAllServers(api, getRequiredCommands());

        api.addSlashCommandCreateListener(new DiscordSlashCommandListener());

        api.addMessageCreateListener(new DiscordChatListener());
        api.addLostConnectionListener(event -> {
            logger().warn("⚠️ Lost connection to Discord");
        });
        api.addReconnectListener(event -> {
            logger().info("ℹ️ Reconnect");
        });
        api.addServerBecomesUnavailableListener(event -> {
            logger().warn("⚠️ Server becomes unavailable... " + event.getServer().getName());
        });
        api.addServerBecomesAvailableListener(event -> {
            logger().info("ℹ️ Server becomes available... " + event.getServer().getName());
        });
        api.addServerJoinListener(event -> {
            logger().warn("⚠️ Server joined... " + event.getServer().getName());
        });
        logger().info("ℹ️ JavaCordBot is now initialized");
    }

    public List<CmdDef> getRequiredCommands() {

        SlashCommandOption playerIdOption = SlashCommandOption.create(
                SlashCommandOptionType.STRING,
                "playerid64",
                "Steam64 ID of the player",
                false);

        SlashCommandOption playerNameOption = SlashCommandOption.create(
                SlashCommandOptionType.STRING,
                "playername",
                "Name of the player",
                false);
        SlashCommandOption playerBNameOption = SlashCommandOption.create(
                SlashCommandOptionType.STRING,
                "targetPlayerName",
                "Name of the target player",
                false);
        SlashCommandOption durationInSec = SlashCommandOption.create(SlashCommandOptionType.LONG, "duration",
                "The duration in seconds");
        SlashCommandOption intValueOption = SlashCommandOption.create(SlashCommandOptionType.LONG, "intValue",
                "The value to set to");
        SlashCommandOption hourOption = SlashCommandOption.create(SlashCommandOptionType.LONG, "hourValue",
                "The hour value to set to");
        SlashCommandOption minuteOption = SlashCommandOption.create(SlashCommandOptionType.LONG, "minuteValue",
                "The minute value to set to");
        SlashCommandOption reasonOption = SlashCommandOption.create(SlashCommandOptionType.STRING, "reason",
                "The reason for the ban/kick");
        SlashCommandOption textOption = SlashCommandOption.create(SlashCommandOptionType.STRING, "text", "Text input");
        SlashCommandOption groupOption = SlashCommandOption.create(SlashCommandOptionType.STRING, "groupName",
                "Group name");
        SlashCommandOption weatherOption = SlashCommandOption.create(SlashCommandOptionType.STRING, "weatherName",
                "Weather name");
        SlashCommandOption channelOption = SlashCommandOption.create(SlashCommandOptionType.STRING, "channel",
                "Channel to send the message to default: local");
        return List.of(
                new CmdDef("getversion", "Show the current DiscordConnect version", List.of()),
                new CmdDef("ban", "Ban a player",
                        List.of(playerIdOption, playerNameOption, durationInSec, reasonOption)),
                new CmdDef("restart", "Trigger server restart", List.of()),
                new CmdDef("reloadplugins", "Trigger plugin reload", List.of()),
                new CmdDef("unban", "Remove a player from ban", List.of(playerIdOption)),
                new CmdDef("online", "List players online", List.of()),
                new CmdDef("help", "Show help", List.of()),
                new CmdDef("getbanned", "Show banned players", List.of()),
                new CmdDef("gettime", "Show current game time", List.of()),
                new CmdDef("getweather", "Show current weather", List.of()),
                new CmdDef("broadcast", "Broadcast message", List.of(textOption, channelOption)),
                new CmdDef("group", "Group management", List.of(playerNameOption, groupOption)),
                new CmdDef("kick", "Kick a player", List.of(playerNameOption, reasonOption)),
                new CmdDef("makeadmin", "Make player admin", List.of(playerNameOption)),
                new CmdDef("sethealth", "Set health", List.of(playerNameOption, intValueOption)),
                new CmdDef("sethunger", "Set hunger", List.of(playerNameOption, intValueOption)),
                new CmdDef("setthirst", "Set thirst", List.of(playerNameOption, intValueOption)),
                new CmdDef("settime", "Set time", List.of(hourOption, minuteOption)),
                new CmdDef("setweather", "Set weather", List.of(weatherOption)),
                new CmdDef("support", "Support commands", List.of(playerNameOption, textOption)),
                new CmdDef("teleporttoplayer", "Teleport to a player", List.of(playerNameOption, playerBNameOption)),
                new CmdDef("unadmin", "Remove admin rights", List.of(playerNameOption)),
                new CmdDef("yell", "Yell a message", List.of(textOption, channelOption)));
    }

}
