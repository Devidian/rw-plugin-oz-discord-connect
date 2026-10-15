package de.omegazirkel.risingworld.discordconnect;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/** Persistent mapping owned by Discord Connect, keyed by parent channel and topic. */
public final class DiscordThreadRegistry {
    private static final Gson JSON = new Gson();
    private final Path file;
    private final Map<String, Long> threadIds = new HashMap<>();

    public DiscordThreadRegistry(Path file) throws IOException {
        this.file = file;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, Long> saved = JSON.fromJson(reader, new TypeToken<Map<String, Long>>() {}.getType());
                if (saved != null) threadIds.putAll(saved);
            } catch (RuntimeException ex) {
                throw new IOException("Invalid Discord thread registry: " + file, ex);
            }
        }
    }

    public static String normalize(String topic) {
        if (topic == null) return null;
        String name = topic.trim();
        if (name.isEmpty()) return null;
        if (name.codePointCount(0, name.length()) > 100 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Discord thread topic must contain 1 to 100 visible characters");
        }
        return name;
    }

    private static String key(long parentChannelId, String topic) {
        return parentChannelId + ":" + topic;
    }

    public synchronized Long get(long parentChannelId, String topic) {
        return threadIds.get(key(parentChannelId, topic));
    }

    public synchronized void put(long parentChannelId, String topic, long threadId) throws IOException {
        String key = key(parentChannelId, topic);
        Long previous = threadIds.put(key, threadId);
        try {
            save();
        } catch (IOException | RuntimeException ex) {
            if (previous == null) threadIds.remove(key);
            else threadIds.put(key, previous);
            throw ex;
        }
    }

    public synchronized void remove(long parentChannelId, String topic) throws IOException {
        String key = key(parentChannelId, topic);
        Long previous = threadIds.remove(key);
        try {
            save();
        } catch (IOException | RuntimeException ex) {
            if (previous != null) threadIds.put(key, previous);
            throw ex;
        }
    }

    private void save() throws IOException {
        Path directory = file.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "discord-threads-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                JSON.toJson(threadIds, writer);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
