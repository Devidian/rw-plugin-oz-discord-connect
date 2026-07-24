package de.omegazirkel.risingworld;

import java.nio.file.Path;

import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChatEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;

/** Rising World entry point; Discord behavior lives in {@link DiscordConnectRuntime}. */
public final class DiscordConnect extends DiscordConnectRuntime implements Listener, FileChangeListener {
    public static DiscordConnect instance;

    public static OZLogger logger() { return DiscordConnectRuntime.logger(); }
    public static void forceRestart() { DiscordConnectRuntime.forceRestart(); }
    public static void restart() { DiscordConnectRuntime.restart(); }

    @Override
    public void onEnable() {
        super.onEnable();
        registerEventListener(this);
    }

    @Override public void onDisable() { super.onDisable(); }
    @Override public void onSettingsChanged(Path settingsPath) { super.onSettingsChanged(settingsPath); }

    @Override @EventMethod
    public void onPlayerCommand(PlayerCommandEvent event) { super.onPlayerCommand(event); }
    @Override @EventMethod
    public void onPlayerChat(PlayerChatEvent event) { super.onPlayerChat(event); }
    @Override @EventMethod
    public void onPlayerSpawn(PlayerSpawnEvent event) { super.onPlayerSpawn(event); }
    @Override @EventMethod
    public void onPlayerDisconnect(PlayerDisconnectEvent event) { super.onPlayerDisconnect(event); }
}
