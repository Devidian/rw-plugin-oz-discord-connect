package de.omegazirkel.risingworld.listeners;

import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import de.omegazirkel.risingworld.DiscordWebHook;
import de.omegazirkel.risingworld.JavaCordBot;
import de.omegazirkel.risingworld.tools.Colors;
import net.risingworld.api.Server;

public class DiscordChatListener implements MessageCreateListener {
    private static DiscordWebHook pluginInstance = null;
    static final Colors c = Colors.getInstance();

    public DiscordChatListener() {
        pluginInstance = JavaCordBot.pluginInstance;
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {

        DiscordWebHook.logger().debug("messageCreateEvent");
        String content = event.getMessageContent();
        MessageAuthor author = event.getMessageAuthor();
        boolean isUserNotBot = author.isUser() && !author.isYourself();
        if (!isUserNotBot) {
            return; // Do not react to Bot messages
        }

        boolean isAdmin = author.isBotOwner() || pluginInstance.getBotAdmins().contains(author.getIdAsString());

        // Not a command, maybe chat? check channel
        String chName = event.getChannel().asServerChannel().map(ServerChannel::getName).orElse(null);
        long chId = event.getChannel().getId();
        if (chName.equalsIgnoreCase(pluginInstance.getBotChatChannelName())
                || chId == pluginInstance.getBotChatChannelId()) {
            String color = pluginInstance.getColorLocalDiscord();
            String group = "";
            if (isAdmin && pluginInstance.getShowGroupSetting()) {
                color = pluginInstance.getColorLocalAdmin();
                group = " (discord/admin)";
            }
            String displayName = author.getDisplayName();
            Server.broadcastTextMessage(color + "[LOCAL] " + displayName + group + ": "
                    + pluginInstance.getColorEndTag() + content);
        }

    }

}
