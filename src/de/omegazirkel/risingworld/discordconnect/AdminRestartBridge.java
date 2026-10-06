package de.omegazirkel.risingworld.discordconnect;

import java.lang.reflect.InvocationTargetException;

import de.omegazirkel.risingworld.DiscordConnect;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;

/** Optional connection to the restart owner in Admin Utils. */
public final class AdminRestartBridge {
    private final Plugin owner;

    public AdminRestartBridge(Plugin owner) {
        this.owner = owner;
    }

    public String requestFromDiscord() {
        return invoke("requestRestartFromDiscord", new Class<?>[0]);
    }

    public String requestFromPlayer(Player player) {
        return invoke("requestRestartFromPlayer", new Class<?>[] { Player.class }, player);
    }

    private String invoke(String method, Class<?>[] types, Object... args) {
        Plugin adminUtils = owner.getPluginByName("OZ - Admin Utils");
        if (adminUtils == null) return "unavailable";
        try {
            Object value = adminUtils.getClass().getMethod(method, types).invoke(adminUtils, args);
            return value instanceof String result ? result : "unavailable";
        } catch (ReflectiveOperationException ex) {
            Throwable cause = ex instanceof InvocationTargetException invocation ? invocation.getCause() : ex;
            DiscordConnect.logger().warn("Admin Utils restart request failed: " + cause.getMessage());
            return "unavailable";
        }
    }
}
