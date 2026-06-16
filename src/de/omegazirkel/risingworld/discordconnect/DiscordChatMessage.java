package de.omegazirkel.risingworld.discordconnect;

public record DiscordChatMessage(
        String content,
        String userId,
        String displayName,
        boolean admin,
        long guildId,
        String guildName,
        long channelId,
        String channelName) {
}
