package de.omegazirkel.risingworld.guards;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;

import de.omegazirkel.risingworld.DiscordWebHook;
import de.omegazirkel.risingworld.JavaCordBot;
import de.omegazirkel.risingworld.tools.I18n;

public class RisingWorldCommandGuard {

    public static Boolean canUseCommand(String command, Message message) {
        Short commandLevel = DiscordWebHook.discordCommands.get(command);
        MessageAuthor author = message.getAuthor();
        DiscordWebHook plugin = JavaCordBot.pluginInstance;
        boolean canExecuteSecureCommands = !plugin.getBotSecure() || author.isBotOwner()
                || plugin.getBotAdmins().contains(author.getDiscriminatedName());
        TextChannel ch = message.getChannel();
        String lang = plugin.getBotLanguage();
        I18n t = plugin.getTranslator();

        
        if (commandLevel == 0) {
            ch.sendMessage(t.get("CMD_ERR_DISABLED", lang).replace("PH_CMD", command));
            message.addReaction("✋");
            return false;
        } else if (commandLevel > 1 && !canExecuteSecureCommands) {
            ch.sendMessage(t.get("CMD_ERR_ADMIN_ONLY", lang).replace("PH_CMD", command));
            message.addReaction("✋");
            return false;
        } else {
            return true;
        }

    }
}
