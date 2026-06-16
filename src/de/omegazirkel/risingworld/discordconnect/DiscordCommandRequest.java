package de.omegazirkel.risingworld.discordconnect;

import java.util.Map;

public record DiscordCommandRequest(
        String responseToken,
        String command,
        Map<String, String> options,
        String userId,
        String displayName,
        boolean admin,
        long guildId,
        String guildName) {
}
