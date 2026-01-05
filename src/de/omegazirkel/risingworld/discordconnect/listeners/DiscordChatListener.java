package de.omegazirkel.risingworld.discordconnect.listeners;

import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.discordconnect.PluginSettings;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Server;

public class DiscordChatListener implements MessageCreateListener {
    static final Colors c = Colors.getInstance();
    static final PluginSettings s = PluginSettings.getInstance();
    static final I18n t = I18n.getInstance(DiscordConnect.instance.getDescription("name"));


    static DiscordConnect getPlugin() {
        return DiscordConnect.instance;
    }

    public DiscordChatListener() {
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {

        DiscordConnect.logger().debug("messageCreateEvent");
        String content = event.getMessageContent();
        MessageAuthor author = event.getMessageAuthor();
        boolean isUserNotBot = author.isUser() && !author.isYourself();
        if (!isUserNotBot) {
            return; // Do not react to Bot messages
        }

        boolean isAdmin = author.isBotOwner() || s.botAdmins.contains(author.getIdAsString());

        // Not a command, maybe chat? check channel
        String chName = event.getChannel().asServerChannel().map(ServerChannel::getName).orElse(null);
        long chId = event.getChannel().getId();
        if (chName.equalsIgnoreCase(s.botChatChannelName)
                || chId == s.chatChannelId) {
            String color = s.colorLocalDiscord;
            String group = "";
            if (isAdmin && s.showGroup) {
                color = s.colorLocalAdmin;
                group = " (discord/admin)";
            }
            String displayName = author.getDisplayName();
            Server.broadcastTextMessage(color + (s.defaultChatPrefix) + displayName + group + ": "
                    + c.endTag + content);
        }

    }

}
