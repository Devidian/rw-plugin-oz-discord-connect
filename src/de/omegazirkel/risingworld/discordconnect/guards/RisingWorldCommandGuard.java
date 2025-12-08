package de.omegazirkel.risingworld.discordconnect.guards;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.discordconnect.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;

public class RisingWorldCommandGuard {

    private static PluginSettings s = PluginSettings.getInstance();

    public static Boolean canUseCommand(String command, Message message) {
        Short commandLevel = s.discordCommands.get(command);
        MessageAuthor author = message.getAuthor();
        boolean canExecuteSecureCommands = !s.botSecure || author.isBotOwner()
        || s.botAdmins.contains(author.getDiscriminatedName());
        TextChannel ch = message.getChannel();
        String lang = s.botLang;
        I18n t = DiscordConnect.instance.getTranslator();

        
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
