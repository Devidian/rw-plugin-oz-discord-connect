package de.omegazirkel.risingworld.discordconnect;

import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.listener.GloballyAttachableListener;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.listeners.DiscordChatListener;
import de.omegazirkel.risingworld.listeners.DiscordSlashCommandListener;
import de.omegazirkel.risingworld.tools.OZLogger;

public class JavaCordBot {

    public static DiscordConnect pluginInstance = null;
    public static DiscordApi api = null;
    private static boolean initialized = false;
    private static PluginSettings s = PluginSettings.getInstance();


    public static OZLogger logger() {
        return OZLogger.getInstance("OZ.DiscordConnect.JavaCord");
    }

    public JavaCordBot(final DiscordConnect plugin) {
        pluginInstance = plugin;
        logger().setLevel(s.logLevel);
    }

    public static void disconnect() {
        if (!initialized)
            return;
        logger().debug("DiscordBot is now disconnecting");
        api.updateActivity("Shutting down...");
        api.getListeners().forEach((GloballyAttachableListener entry, List<Class<GloballyAttachableListener>> list) -> {
            api.removeListener(entry);
        });
        api.disconnect().join();
        logger().info("ℹ️ DiscordBot is now disconnected!");
    }

    public void init() {
        if (initialized) {
            return; // do not execute more than once
        }
        initialized = true;
        api = new DiscordApiBuilder()
                .setToken(s.botToken)
                // .setAllIntents()
                .addIntents(Intent.MESSAGE_CONTENT)
                .login()
                .join();

        api.updateActivity("Running, waiting for players!");

        CommandRegistry.syncCommandsForAllServers(api);

        api.addSlashCommandCreateListener(new DiscordSlashCommandListener());

        api.addMessageCreateListener(new DiscordChatListener());
        api.addLostConnectionListener(event -> {
            logger().warn("⚠️ Lost connection to Discord");
        });
        api.addReconnectListener(event -> {
            logger().info("ℹ️ Reconnect");
        });
        api.addServerBecomesUnavailableListener(event -> {
            logger().warn("⚠️ Server becomes unavailable... " + event.getServer().getName());
        });
        api.addServerBecomesAvailableListener(event -> {
            logger().info("ℹ️ Server becomes available... " + event.getServer().getName());
        });
        api.addServerJoinListener(event -> {
            logger().warn("⚠️ Server joined... " + event.getServer().getName());
        });
        logger().info("ℹ️ JavaCordBot is now initialized");
    }

}
