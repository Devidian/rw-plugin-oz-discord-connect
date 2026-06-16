package de.omegazirkel.risingworld.discordconnect;

public record DiscordCommandResult(String responseToken, String content, String fileName, byte[] fileContent) {
    public static DiscordCommandResult text(String token, String content) {
        return new DiscordCommandResult(token, content, null, null);
    }

    public static DiscordCommandResult textFile(String token, String content, String fileName, byte[] fileContent) {
        return new DiscordCommandResult(token, content, fileName, fileContent);
    }
}
