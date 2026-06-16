package de.omegazirkel.risingworld.discordconnect;

import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public final class CommandRegistry {
    private CommandRegistry() {
    }

    public static List<CmdDef> getRequiredCommands() {
        OptionData playerId = option(OptionType.STRING, "playerid64", "Steam64 ID of the player", false);
        OptionData player = option(OptionType.STRING, "playername", "Name of the player", false);
        OptionData target = option(OptionType.STRING, "targetplayername", "Name of the target player", false);
        OptionData duration = option(OptionType.INTEGER, "duration", "Duration in seconds", false);
        OptionData value = option(OptionType.INTEGER, "intvalue", "Value to set", false);
        OptionData hour = option(OptionType.INTEGER, "hourvalue", "Hour to set", false);
        OptionData minute = option(OptionType.INTEGER, "minutevalue", "Minute to set", false);
        OptionData reason = option(OptionType.STRING, "reason", "Reason", false);
        OptionData text = option(OptionType.STRING, "text", "Text input", false);
        OptionData group = option(OptionType.STRING, "groupname", "Group name", false);
        OptionData weather = option(OptionType.STRING, "weathername", "Weather name", false);
        OptionData channel = option(OptionType.STRING, "channel", "Message channel, default local", false);
        return List.of(
                command("getversion", "Show the current DiscordConnect version"),
                command("ban", "Ban a player", playerId, player, duration, reason),
                command("restart", "Trigger server restart"),
                command("reloadplugins", "Trigger plugin reload"),
                command("unban", "Remove a player from ban", playerId, player),
                command("online", "List players online"),
                command("help", "Show help"),
                command("getbanned", "Show banned players"),
                command("gettime", "Show current game time"),
                command("getweather", "Show current weather"),
                command("broadcast", "Broadcast message", text, channel),
                command("group", "Group management", player, group),
                command("kick", "Kick a player", player, reason),
                command("makeadmin", "Make player admin", player),
                command("sethealth", "Set health", player, value),
                command("sethunger", "Set hunger", player, value),
                command("setthirst", "Set thirst", player, value),
                command("settime", "Set time", hour, minute),
                command("setweather", "Set weather", weather),
                command("support", "Support commands", player, text),
                command("teleporttoplayer", "Teleport to a player", player, target),
                command("unadmin", "Remove admin rights", player),
                command("yell", "Yell a message", text, channel));
    }

    public static void syncCommands(Guild guild) {
        List<SlashCommandData> commands = getRequiredCommands().stream().map(def -> {
            SlashCommandData command = Commands.slash(def.name(), def.description());
            def.options().forEach(command::addOptions);
            return command;
        }).toList();
        guild.updateCommands().addCommands(commands).queue(
                ignored -> de.omegazirkel.risingworld.DiscordConnect.logger()
                        .info("Synced slash commands for guild " + guild.getName()),
                error -> de.omegazirkel.risingworld.DiscordConnect.logger()
                        .error("Failed to sync commands for guild " + guild.getName() + ": " + error.getMessage()));
    }

    private static OptionData option(OptionType type, String name, String description, boolean required) {
        return new OptionData(type, name, description, required);
    }

    private static CmdDef command(String name, String description, OptionData... options) {
        return new CmdDef(name, description, List.of(options));
    }
}
