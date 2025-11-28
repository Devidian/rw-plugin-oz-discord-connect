package de.omegazirkel.risingworld.listeners;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import de.omegazirkel.risingworld.DiscordWebHook;
import de.omegazirkel.risingworld.JavaCordBot;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Server;
import net.risingworld.api.database.WorldDatabase;
import net.risingworld.api.database.WorldDatabase.Target;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.definitions.WeatherDefs;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.Time.Unit;

public class DiscordSlashCommandListener implements SlashCommandCreateListener {

    static final Colors c = Colors.getInstance();

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();
        try {
            String commandName = interaction.getCommandName();
            Set<SlashCommand> commands = interaction.getServer().get().getSlashCommands().join();
            List<String> commandNames = commands.stream().map(SlashCommand::getName).toList();
            if (!commandNames.contains(commandName)) {
                JavaCordBot.logger().warn("User issued unknown slashCommand <name:" + commandName + "><id:"
                        + interaction.getCommandId() + "> Available commands: " + commandNames.toString());

                interaction.createImmediateResponder()
                        .setContent(t.get("CMD_ERR_UNKNOWN", lang).replace("PH_CMD", commandName))
                        .setFlags(MessageFlag.EPHEMERAL)
                        .respond().join();
                return;
            }
            if (!canUseCommand(interaction)) {
                return;
            }
            switch (commandName) {
                case "ban":
                    handleBanCommand(interaction);
                    break;
                case "unban":
                    handleUnbanCommand(interaction);
                    break;
                case "online":
                    handleOnlineCommand(interaction);
                    break;
                case "help":
                    handleHelpCommand(interaction);
                    break;
                case "broadcast":
                    handleBroadcastCommand(interaction);
                    break;
                case "getbanned":
                    handleGetBannedCommand(interaction);
                    break;
                case "gettime":
                    handleGetTimeCommand(interaction);
                    break;
                case "getversion":
                    handleGetVersionCommand(interaction);
                    break;
                case "getweather":
                    handleGetWeatherCommand(interaction);
                    break;
                case "group":
                    handleGroupCommand(interaction);
                    break;
                case "kick":
                    handleKickCommand(interaction);
                    break;
                case "makeadmin":
                    handleMakeAdminCommand(interaction);
                    break;
                case "restart":
                    handleRestartCommand(interaction);
                    break;
                case "reloadplugins":
                    handleReloadpluginsCommand(interaction);
                    break;
                case "sethealth":
                    handleSetHealthCommand(interaction);
                    break;
                case "sethunger":
                    handleSetHungerCommand(interaction);
                    break;
                case "setthirst":
                    handleSetThirstCommand(interaction);
                    break;
                case "settime":
                    handleSetTimeCommand(interaction);
                    break;
                case "setweather":
                    handleSetWeatherCommand(interaction);
                    break;
                case "support":
                    handleSupportCommand(interaction);
                    break;
                case "teleporttoplayer":
                    handleTeleportToPlayerCommand(interaction);
                    break;
                case "unadmin":
                    handleUnAdminCommand(interaction);
                    break;
                case "yell":
                    handleYellCommand(interaction);
                    break;
                default:
                    JavaCordBot.logger().warn("User issued unknown slashCommand <name:" + commandName + "><id:"
                            + interaction.getCommandId() + ">");
                    interaction.createImmediateResponder()
                            .setContent(t.get("CMD_ERR_UNKNOWN", lang).replace("PH_CMD", commandName))
                            .setFlags(MessageFlag.EPHEMERAL)
                            .respond().join();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            interaction.createImmediateResponder()
                    .setContent(e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }

    }

    public boolean canUseCommand(SlashCommandInteraction interaction) {
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();
        String commandName = interaction.getCommandName();
        if (!DiscordWebHook.discordCommands.containsKey(commandName)) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_NOLEVEL", lang).replace("PH_CMD", commandName))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return false;
        }
        Short commandLevel = DiscordWebHook.discordCommands.get(commandName);

        User user = interaction.getUser();

        boolean canExecuteSecureCommands = !plugin.getBotSecure() || user.isBotOwner()
                || plugin.getBotAdmins().contains(user.getIdAsString());

        if (commandLevel == 0) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_DISABLED", lang).replace("PH_CMD", commandName))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return false;
        } else if (commandLevel > 1 && !canExecuteSecureCommands) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_ADMIN_ONLY", lang).replace("PH_CMD", commandName))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return false;
        } else {
            return true;
        }
    }

    private void handleBanCommand(SlashCommandInteraction interaction) {
        Optional<Long> playerID = interaction.getArgumentLongValueByName("playerid64");
        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");
        // default to 7 days in seconds
        long duration = interaction.getArgumentLongValueByName("duration").orElse(604800L);
        String reason = interaction.getArgumentStringValueByName("reason").orElse("No reason");

        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        Player player;
        if (playerID.isPresent()) {
            player = Server.getPlayerByUID(playerID.toString());
        } else if (playerName.isPresent()) {
            player = Server.getPlayerByName(playerName.get());
        } else {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_BAN_ARGUMENTS", lang))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        if (player == null) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerID.toString()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        // cast duration to integer
        player.ban(reason, (int) duration);

        for (Player p : Server.getAllPlayers()) {
            String l = p.getSystemLanguage();
            p.sendTextMessage(c.warning + plugin.getName() + ":>" + c.text
                    + t.get("BC_BANNED", l).replace("PH_PLAYER", player.getName())
                            .replace("PH_DISCORDUSER",
                                    interaction.getUser().getDisplayName(interaction.getServer().get()))
                            .replace("PH_REASON", reason));
        }
        interaction.createImmediateResponder().setContent("✅").setFlags(MessageFlag.EPHEMERAL).respond().join();
    }

    private void handleUnbanCommand(SlashCommandInteraction interaction) {
        Optional<String> playerID = interaction.getArgumentStringValueByName("playerid64");
        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");

        if (playerName.isPresent())
            Server.unbanPlayer(playerName.get());
        else if (playerID.isPresent())
            Server.unbanPlayer(playerID.get());
        else {
            interaction.createImmediateResponder().setContent("❌ id or name must be set")
                    .setFlags(MessageFlag.EPHEMERAL).respond().join();
            return;
        }

        interaction.createImmediateResponder().setContent("✅").setFlags(MessageFlag.EPHEMERAL).respond().join();
    }

    private void handleOnlineCommand(SlashCommandInteraction interaction) {
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        int playersOnline = Server.getPlayerCount();
        if (playersOnline == 0) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_OUT_ONLINE_NOBODY", lang))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        List<String> list = Arrays.asList(t.get("CMD_OUT_ONLINE_LIST", lang) + "\n");
        StringBuilder sb = new StringBuilder();
        list.forEach(sb::append);
        for (Player p : Server.getAllPlayers()) {
            sb.append(p.getName()
                    + " uid:" + p.getUID()
                    + " g:" + p.getPermissionGroup()
                    + (p.isAdmin() ? " [A]" : "")
                    + "\n");
        }
        interaction.createImmediateResponder()
                .setContent(sb.toString())
                .setFlags(MessageFlag.EPHEMERAL)
                .respond().join();
    }

    private void handleHelpCommand(SlashCommandInteraction interaction) {
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();
        try {
            interaction.createImmediateResponder()
                    .setContent(t.get("DISCORD_HELP_SHORT", lang))
                    // TODO send long help as file?
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        } catch (Exception e) {

            interaction.createImmediateResponder().setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL).respond().join();
            DiscordWebHook.logger().error(e.getMessage());
        }

    }

    private void handleBroadcastCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String text = interaction.getArgumentStringValueByName("text").orElse("Hello World!");
        String type = interaction.getArgumentStringValueByName("channel").orElse("local");

        Server.broadcastTextMessage(
                plugin.getColorSupport() + "[" + type + "] " + user.getDisplayName(interaction.getServer().get()) + ": "
                        + plugin.getColorEndTag() + text);

        interaction.createImmediateResponder().setContent("✅").setFlags(MessageFlag.EPHEMERAL).respond().join();
    }

    private void handleGetBannedCommand(SlashCommandInteraction interaction) {
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        WorldDatabase db = plugin.getWorldDatabase(Target.Bans);
        try (ResultSet result = db.executeQuery("SELECT * FROM `Banlist`")) {
            List<String> list = Arrays.asList(t.get("CMD_OUT_BANNED_LIST", lang) + "\n");
            StringBuilder sb = new StringBuilder();
            list.forEach(sb::append);
            // FIXME find a way to display results as table in discord
            // Debugging Table
            // ResultSetMetaData meta = result.getMetaData();
            // for (int i = 1; i <= meta.getColumnCount(); i++) {
            // 1: UserId64
            // 2: UserName
            // 3: ?
            // 4: ?
            // 5: UserId64 (who banned)
            // 6: UserName (who banned)
            // 7: Reason
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=1:76561199118960452
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=2:絵の具入り水
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=3:1762866919
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=4:720000
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=5:76561197972223708
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=6:Devidian
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=7:Griefing is not allowed
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=1:76561198035762372
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=2:Merida
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=3:1764108713
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=4:604800
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=5:
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=6:API
            // [2025-11-25 22:55:14] DEBUG OZ.DiscordPlugin - i=7:No reason

            // }
            while (result.next()) {
                // for (int i = 1; i <= meta.getColumnCount(); i++) {
                // DiscordWebHook.logger().debug("i=" + i + ":" + result.getString(i));
                // }

                String name = result.getString(2);
                String reason = result.getString(7);
                long UID = result.getLong(1);
                sb.append(name + " ( " + UID + " ) banned for: " + reason + "\n");
                // sb.append("Row <" + result.getString(1) + ">:");
            }
            interaction.createImmediateResponder()
                    .setContent(sb.toString())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        } catch (SQLException e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        } catch (Exception e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    private void handleGetTimeCommand(SlashCommandInteraction interaction) {
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();
        String response = t.get("CMD_OUT_TIME", lang)
                .replace("PH_TIME", Server.getGameTime(Unit.Hours) + ":" + Server.getGameTime(Unit.Minutes))
                .replace("PH_SEASON", Server.getCurrentSeason().toString())
                .replace("PH_YEAR", Server.getGameTime(Unit.Years) + "")
                .replace("PH_DAY", Server.getGameTime(Unit.Days) + "");
        interaction.createImmediateResponder()
                .setContent(response)
                .setFlags(MessageFlag.EPHEMERAL)
                .respond().join();
    }

    private void handleGetVersionCommand(SlashCommandInteraction interaction) {
        interaction.createImmediateResponder()
                .setContent("Plugin version: " + JavaCordBot.pluginInstance.getDescription("version")
                        + "\nGame version: " + JavaCordBot.pluginInstance.getGameVersion())
                .setFlags(MessageFlag.EPHEMERAL)
                .respond().join();
    }

    private void handleGetWeatherCommand(SlashCommandInteraction interaction) {
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        WeatherDefs.Weather defCurrent = Server.getCurrentWeather();
        String currentWeatherName = defCurrent.name;
        interaction.createImmediateResponder()
                .setContent(t.get("CMD_OUT_WEATHER", lang).replace("PH_WEATHER", currentWeatherName))
                .setFlags(MessageFlag.EPHEMERAL)
                .respond().join();
    }

    private void handleGroupCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");
        Optional<String> groupName = interaction.getArgumentStringValueByName("groupname");
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        if (groupName == null || playerName == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_GROUP_ARGUMENTS", lang))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Player player = Server.getPlayerByName(playerName.get());
        if (player == null) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerName.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }
        player.setPermissionGroup(groupName.get());

        for (Player p : Server.getAllPlayers()) {
            String l = p.getSystemLanguage();
            p.sendTextMessage(c.warning + plugin.getName() + ":>" + c.text
                    + t.get("BC_GROUP", l)
                            .replace("PH_DISCORDUSER", user.getDisplayName(interaction.getServer().get()))
                            .replace("PH_PLAYER", playerName.get())
                            .replace("PH_GROUP", groupName.get()));
        }
        interaction.createImmediateResponder()
                .setContent(t.get("CMD_OUT_GROUP", lang).replace("PH_PLAYER", playerName.get()).replace("PH_GROUP",
                        groupName.get()))
                .setFlags(MessageFlag.EPHEMERAL)
                .respond().join();
    }

    private void handleKickCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");
        String reason = interaction.getArgumentStringValueByName("reason").orElse("No reason");
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        if (playerName == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_KICK_ARGUMENTS", lang))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Player player = Server.getPlayerByName(playerName.get());

        if (player == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerName.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        player.kick(reason);
        for (Player p : Server.getAllPlayers()) {

            String l = p.getSystemLanguage();
            p.sendTextMessage(c.warning + plugin.getName() + ":>" + c.text
                    + t.get("BC_KICKED", l)
                            .replace("PH_PLAYER", playerName.get())
                            .replace("PH_DISCORDUSER", user.getDisplayName(interaction.getServer().get()))
                            .replace("PH_REASON", reason));
        }

        interaction.createImmediateResponder()
                .setContent("Player " + playerName + " kicked!")
                .setFlags(MessageFlag.EPHEMERAL)
                .respond().join();
    }

    private void handleMakeAdminCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        if (playerName == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_ARGUMENT_LENGTH", lang)
                            .replace("PH_CMD", "/makeadmin [PLAYER]"))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Player player = Server.getPlayerByName(playerName.get());

        if (player == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerName.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        try {
            player.setAdmin(true);
            interaction.createImmediateResponder()
                    .setContent("✅")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        } catch (Exception e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    private void handleRestartCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();
        String responseMessage;

        int playersLeft = Server.getPlayerCount();
        if (playersLeft == 0) {
            responseMessage = t.get("CMD_OUT_RESTART_NOW", lang);

            interaction.createImmediateResponder()
                    .setContent(responseMessage)
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();

            DiscordWebHook.restart();

        } else {

            for (Player p : Server.getAllPlayers()) {
                String l = p.getSystemLanguage();
                p.sendTextMessage(c.warning + plugin.getName() + ":>" + c.text
                        + t.get("BC_RESTART", l)
                                .replace("PH_DISCORDUSER", user.getDisplayName(interaction.getServer().get())));
            }

            plugin.setFlagRestart(true);
            responseMessage = t.get("CMD_OUT_RESTART_DELAY", lang).replace("PH_PLAYERS", playersLeft + "");

            interaction.createImmediateResponder()
                    .setContent(responseMessage)
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    private void handleReloadpluginsCommand(SlashCommandInteraction interaction) {
        // User user = interaction.getUser();
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        // String lang = plugin.getBotLanguage();
        // I18n t = plugin.getTranslator();
        // String responseMessage;

        interaction.createImmediateResponder()
                .setContent("Plugins reloading...")
                .setFlags(MessageFlag.EPHEMERAL)
                .respond().join();

        plugin.executeDelayed(5, () -> {
            Server.sendInputCommand("reloadplugins");
        });
    }

    private void handleSetHealthCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");
        Integer health = interaction.getArgumentLongValueByName("intValue").orElse(100l).intValue();
        if (playerName == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_ARGUMENT_LENGTH", lang)
                            .replace("PH_CMD", "/sethealth [PLAYER] [INT]"))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Player player = Server.getPlayerByName(playerName.get());

        if (player == null) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerName.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        try {
            player.setHealth(health);
            interaction.createImmediateResponder()
                    .setContent("✅ Player health set to " + health)
                    .setFlags(MessageFlag.EPHEMERAL).respond().join();
            DiscordWebHook.logger().info("User " + user.getDisplayName(interaction.getServer().get())
                    + " has set health of " + player.getName() + " to " + health + "!");
        } catch (Exception e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    private void handleSetHungerCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");
        Integer hunger = interaction.getArgumentLongValueByName("intValue").orElse(100l).intValue();

        if (playerName == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_ARGUMENT_LENGTH", lang)
                            .replace("PH_CMD", "/sethunger [PLAYER] [INT]"))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Player player = Server.getPlayerByName(playerName.get());

        if (player == null) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerName.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        try {
            player.setHunger(hunger);
            interaction.createImmediateResponder()
                    .setContent("✅ Player hunger set to " + hunger)
                    .setFlags(MessageFlag.EPHEMERAL).respond().join();
            DiscordWebHook.logger().info("User " + user.getDisplayName(interaction.getServer().get())
                    + " has set hunger of " + player.getName() + " to " + hunger + "!");
        } catch (Exception e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    private void handleSetThirstCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");
        Integer thirst = interaction.getArgumentLongValueByName("intValue").orElse(100l).intValue();
        if (playerName == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_ARGUMENT_LENGTH", lang)
                            .replace("PH_CMD", "/setthirst [PLAYER] [INT]"))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Player player = Server.getPlayerByName(playerName.get());

        if (player == null) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerName.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        try {
            player.setThirst(thirst);
            interaction.createImmediateResponder()
                    .setContent("✅ Player thirst set to " + thirst)
                    .setFlags(MessageFlag.EPHEMERAL).respond().join();
            DiscordWebHook.logger().info("User " + user.getDisplayName(interaction.getServer().get())
                    + " has set thirst of " + player.getName() + " to " + thirst + "!");
        } catch (Exception e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    private void handleSetTimeCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();

        Integer hour = interaction.getArgumentLongValueByName("hour").orElse(0l).intValue();
        Integer minute = interaction.getArgumentLongValueByName("minute").orElse(0l).intValue();

        try {
            Server.setGameTime(hour, minute);
            interaction.createImmediateResponder()
                    .setContent("✅ Game time set to " + hour + ":" + minute)
                    .setFlags(MessageFlag.EPHEMERAL).respond().join();
            DiscordWebHook.logger().info("User " + user.getDisplayName(interaction.getServer().get())
                    + " has set time to " + hour + ":" + minute + "!");
        } catch (Exception e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    private void handleSetWeatherCommand(SlashCommandInteraction interaction) {
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();
        Optional<String> weatherToSet = interaction.getArgumentStringValueByName("weatherName");
        StringBuilder sb = new StringBuilder();
        for (WeatherDefs.Weather weather : Definitions.getAllWeathers()) {
            if (weather != null)
                sb.append(weather.name + "\n");
        }

        if (weatherToSet.isEmpty())
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_ILLEGAL_ARGUMENTS", lang).replace("PH_CMD", "/setweather [Weather]")
                            .replace("PH_ARGUMENT", "Weather").replace("PH_ARGS_AVAILABLE", sb.toString()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();

        try {

            WeatherDefs.Weather newWeatherDef = Definitions.getWeather(weatherToSet.get());
            Server.setWeather(newWeatherDef, false);
            interaction.createImmediateResponder()
                    .setContent("✅")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        } catch (Exception e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }

    }

    private void handleSupportCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");
        Optional<String> content = interaction.getArgumentStringValueByName("text");
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        if (playerName == null || content == null) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_SUPPORT_ARGUMENTS", lang))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Player player = Server.getPlayerByName(playerName.get());

        if (player == null) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerName.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        player.sendTextMessage(
                plugin.getColorSupport() + "[SUPPORT] " + user.getDisplayName(interaction.getServer().get()) + ": "
                        + plugin.getColorEndTag() + content.get());
        interaction.createImmediateResponder()
                .setContent("✅ Message sent to " + playerName.get() + ": " + content.get())
                .respond().join();
    }

    private void handleTeleportToPlayerCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        Optional<String> playerNameA = interaction.getArgumentStringValueByName("playername");
        Optional<String> playerNameB = interaction.getArgumentStringValueByName("targetPlayerName");
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        if (playerNameA == null || playerNameB == null) {
            interaction.createImmediateResponder()
                    .setContent(
                            t.get("CMD_ERR_ARGUMENT_LENGTH", lang).replace("PH_CMD",
                                    "/teleporttoplayer [PLAYERNAME] [PLAYERNAME]"))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Player player = Server.getPlayerByName(playerNameA.get());
        Player targetPlayer = Server.getPlayerByName(playerNameB.get());

        if (player == null) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerNameA.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }
        if (targetPlayer == null) {
            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerNameB.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        try {
            player.setPosition(targetPlayer.getPosition());
            interaction.createImmediateResponder()
                    .setContent("✅ Player " + playerNameA + " has been teleported to " + targetPlayer.getName())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        } catch (Exception e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    private void handleUnAdminCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        Optional<String> playerName = interaction.getArgumentStringValueByName("playername");
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        if (playerName == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_ARGUMENT_LENGTH", lang)
                            .replace("PH_CMD", "/unadmin [PLAYER]"))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Player player = Server.getPlayerByName(playerName.get());

        if (player == null) {

            interaction.createImmediateResponder()
                    .setContent(t.get("CMD_ERR_PLAYER_OFFLINE", lang).replace("PH_PLAYER", playerName.get()))
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        try {
            player.setAdmin(false);
            interaction.createImmediateResponder()
                    .setContent("✅")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        } catch (Exception e) {
            DiscordWebHook.logger().error(e.getMessage());
            interaction.createImmediateResponder()
                    .setContent("❌ " + e.getMessage())
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    private void handleYellCommand(SlashCommandInteraction interaction) {
        User user = interaction.getUser();
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        String text = interaction.getArgumentStringValueByName("text").orElse("Hello World!");
        String type = interaction.getArgumentStringValueByName("channel").orElse("local");

        Server.broadcastYellMessage(
                plugin.getColorSupport() + "[" + type + "] " + user.getDisplayName(interaction.getServer().get()) + ": "
                        + plugin.getColorEndTag() + text,
                10, false);

        interaction.createImmediateResponder().setContent("✅").setFlags(MessageFlag.EPHEMERAL).respond().join();
    }

}
