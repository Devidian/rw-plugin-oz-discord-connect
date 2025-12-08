package de.omegazirkel.risingworld.discordconnect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.ApplicationCommandBuilder;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

public class CommandRegistry {

    public static void syncCommandsForAllServers(DiscordApi api) {
        List<CmdDef> commands = getRequiredCommands();
        api.getServers().forEach(server -> syncCommandsForServer(api, server, commands));
    }

    /**
     * syncronize commands for all servers the api is connected to
     */
    public static void syncCommandsForAllServers(DiscordApi api, List<CmdDef> commands) {
        api.getServers().forEach(server -> syncCommandsForServer(api, server, commands));
    }

    public static List<CmdDef> getRequiredCommands() {

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

    public static void syncCommandsForServer(DiscordApi api, Server server) {
        syncCommandsForServer(api, server, getRequiredCommands());
    }

    /**
     * syncronize commands for a single server
     */
    public static void syncCommandsForServer(DiscordApi api, Server server, List<CmdDef> requiredCommands) {
        JavaCordBot.logger().info("🔄 Syncing slash commands for server: " + server.getName());

        try {
            Set<ApplicationCommandBuilder<?, ?, ?>> builders = new HashSet<>();

            for (CmdDef def : requiredCommands) {

                SlashCommandBuilder builder = new SlashCommandBuilder()
                        .setName(def.name())
                        .setDescription(def.description());

                def.options().forEach(builder::addOption);

                builders.add(builder);
            }

            // Overwrite all server commands with exactly these
            api.bulkOverwriteServerApplicationCommands(server, builders);
            JavaCordBot.logger().info("✅ Command sync complete for server " + server.getName());
        } catch (Exception e) {
            JavaCordBot.logger().error("❌ Failed to sync commands: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
