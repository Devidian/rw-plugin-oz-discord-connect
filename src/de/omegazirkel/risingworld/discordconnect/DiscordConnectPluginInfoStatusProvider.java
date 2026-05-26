package de.omegazirkel.risingworld.discordconnect;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProvider;
import net.risingworld.api.objects.Player;

public class DiscordConnectPluginInfoStatusProvider implements PluginInfoStatusProvider {
    private final DiscordConnect plugin;
    private final String pluginName;
    private final String version;

    public DiscordConnectPluginInfoStatusProvider(DiscordConnect plugin, String version) {
        this.plugin = plugin;
        this.pluginName = DiscordConnect.name == null || DiscordConnect.name.isBlank()
                ? "OZ - Discord Connect"
                : DiscordConnect.name;
        this.version = version == null ? "" : version;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public String getInfo(Player player) {
        return t().get("TC_DISCORD_CONNECT_INFO_PANEL_INFO", player)
                .replace("PH_PLUGIN_NAME", pluginName)
                .replace("PH_VERSION", version)
                .replace("PH_PLUGIN_CMD", "dc");
    }

    @Override
    public String getStatus(Player player) {
        PluginSettings settings = PluginSettings.getInstance();
        return t().get("TC_DISCORD_CONNECT_INFO_PANEL_STATUS", player)
                .replace("PH_BOT_ENABLED", String.valueOf(settings.botEnable))
                .replace("PH_POST_CHAT", String.valueOf(settings.postChat))
                .replace("PH_POST_SUPPORT", String.valueOf(settings.postSupport))
                .replace("PH_REPORT_STATUS", String.valueOf(settings.reportServerStatus))
                .replace("PH_REPORT_SETTINGS", String.valueOf(settings.reportSettingsChanged))
                .replace("PH_REPORT_JAR", String.valueOf(settings.reportJarChanged))
                .replace("PH_ALLOW_RESTART", String.valueOf(settings.allowRestart))
                .replace("PH_RESTART_TIMED", String.valueOf(settings.restartTimed))
                .replace("PH_JOIN_DISCORD", String.valueOf(settings.joinDiscord != null && !settings.joinDiscord.isBlank()))
                .replace("PH_LANGUAGE", player.getLanguage() + " / " + player.getSystemLanguage())
                .replace("PH_USEDLANG", t().getLanguageUsed(player.getSystemLanguage()))
                .replace("PH_LANG_AVAILABLE", t().getLanguageAvailable());
    }

    private I18n t() {
        return I18n.getInstance(plugin);
    }
}
