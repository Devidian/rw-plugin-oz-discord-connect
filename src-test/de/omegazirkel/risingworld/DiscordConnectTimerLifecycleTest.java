package de.omegazirkel.risingworld;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.Timer;
import java.util.TimerTask;

import org.junit.Test;

public class DiscordConnectTimerLifecycleTest {
    @Test
    public void lifecycleCreatesStopsAndRecreatesActivityTimer() {
        DiscordConnect plugin = new DiscordConnect();

        plugin.initializeTimers();
        Timer firstActivity = plugin.activityTimer;
        assertNotNull(firstActivity);

        plugin.shutdownTimers();
        assertNull(plugin.activityTimer);
        assertCancelled(firstActivity);

        plugin.initializeTimers();
        assertNotSame(firstActivity, plugin.activityTimer);
        plugin.shutdownTimers();
    }

    private static void assertCancelled(Timer timer) {
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                }
            }, 1);
            throw new AssertionError("cancelled timer accepted a new task");
        } catch (IllegalStateException expected) {
            // Expected for a cancelled timer.
        }
    }
}
