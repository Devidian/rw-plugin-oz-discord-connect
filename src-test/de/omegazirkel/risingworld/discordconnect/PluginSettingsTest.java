package de.omegazirkel.risingworld.discordconnect;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

public class PluginSettingsTest {
    @Test
    public void parsesOnlyExactDiscordSnowflakeIds() {
        assertEquals(
                Set.of("123456789012345678", "234567890123456789"),
                PluginSettings.parseDiscordSnowflakeIds(
                        "123456789012345678, invalid, 234567890123456789,123456789012345678"));
    }

    @Test
    public void emptyConfigurationProducesEmptySet() {
        assertEquals(Set.of(), PluginSettings.parseDiscordSnowflakeIds(" "));
    }
}
