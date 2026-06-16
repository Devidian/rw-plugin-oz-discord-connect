package de.omegazirkel.risingworld;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiscordConnectStatusNotificationTest {
    @Test
    public void webhookStatusDoesNotRequireBot() {
        assertTrue(DiscordConnect.canSendStatusNotification(true, false, false));
    }

    @Test
    public void directStatusCanQueueWhileJdaIsStarting() {
        assertTrue(DiscordConnect.canSendStatusNotification(false, true, true));
    }

    @Test
    public void missingDestinationRejectsStatus() {
        assertFalse(DiscordConnect.canSendStatusNotification(false, true, false));
    }
}
