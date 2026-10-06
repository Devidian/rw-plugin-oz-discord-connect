package de.omegazirkel.risingworld.discordconnect;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.omegazirkel.risingworld.DiscordConnect;

public class RestartStatusContractTest {
    @Test public void notifierIsPubliclyReachableByAdminUtils() throws Exception {
        assertEquals(DiscordConnect.class,
                DiscordConnect.class.getMethod("notifyRestartStatus", String.class).getDeclaringClass());
    }
}
