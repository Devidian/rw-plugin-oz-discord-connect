package de.omegazirkel.risingworld.discordconnect;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.io.IOException;
import java.util.List;

import de.omegazirkel.risingworld.DiscordConnect;

import org.junit.Test;

public class DiscordThreadRegistryTest {
    @Test
    public void persistsChannelScopedTopicsAndRecoversRemoval() throws Exception {
        Path file = Files.createTempDirectory("discord-thread-registry-").resolve("threads.json");
        DiscordThreadRegistry registry = new DiscordThreadRegistry(file);
        registry.put(11L, "Shared", 101L);
        registry.put(12L, "Shared", 102L);

        DiscordThreadRegistry reopened = new DiscordThreadRegistry(file);
        assertEquals(Long.valueOf(101L), reopened.get(11L, "Shared"));
        assertEquals(Long.valueOf(102L), reopened.get(12L, "Shared"));
        reopened.remove(11L, "Shared");
        assertNull(new DiscordThreadRegistry(file).get(11L, "Shared"));
        assertEquals(Long.valueOf(102L), new DiscordThreadRegistry(file).get(12L, "Shared"));
    }

    @Test
    public void failedPersistenceDoesNotRetainUncommittedMapping() throws Exception {
        Path file = Files.createTempDirectory("discord-thread-write-failure-").resolve("threads.json");
        DiscordThreadRegistry registry = new DiscordThreadRegistry(file);
        Files.createDirectory(file);
        assertThrows(IOException.class, () -> registry.put(11L, "Shared", 101L));
        assertNull(registry.get(11L, "Shared"));
    }

    @Test
    public void normalizesAndRejectsInvalidTopics() {
        assertNull(DiscordThreadRegistry.normalize("  "));
        assertEquals("Shared", DiscordThreadRegistry.normalize(" Shared "));
        assertEquals("😀".repeat(100), DiscordThreadRegistry.normalize("😀".repeat(100)));
        assertThrows(IllegalArgumentException.class, () -> DiscordThreadRegistry.normalize("x".repeat(101)));
        assertThrows(IllegalArgumentException.class, () -> DiscordThreadRegistry.normalize("bad\nname"));
    }

    @Test
    public void retainsDirectChannelMethodsAndAddsTopicMethod() throws Exception {
        assertNotNull(DiscordConnect.class.getMethod("sendDiscordMessageToTextChannel", String.class, long.class));
        assertNotNull(DiscordConnect.class.getMethod("sendDiscordMessageToTextChannel", String.class, long.class,
                byte[].class));
        assertNotNull(DiscordConnect.class.getMethod("sendDiscordMessageToTextChannel", String.class, long.class,
                byte[].class, String.class));
        assertNotNull(DiscordConnect.class.getMethod("sendDiscordMessageToWebHook", String.class, String.class,
                URI.class, byte[].class, long.class, String.class));
    }

    private record Candidate(long id, String name) {}

    @Test
    public void selectorPrefersSavedIdAndRejectsAmbiguousManualNames() {
        List<Candidate> candidates = List.of(new Candidate(101L, "Renamed"),
                new Candidate(102L, "Shared"), new Candidate(103L, "Shared"));
        assertEquals(101L, DiscordThreadSelector.select(candidates, 101L, "Shared",
                Candidate::id, Candidate::name).id());
        assertThrows(IllegalStateException.class, () -> DiscordThreadSelector.select(candidates, null,
                "Shared", Candidate::id, Candidate::name));
        assertEquals(101L, DiscordThreadSelector.select(candidates, 999L, "Renamed",
                Candidate::id, Candidate::name).id());
        assertNull(DiscordThreadSelector.select(candidates, null, "Missing", Candidate::id, Candidate::name));
    }
}
