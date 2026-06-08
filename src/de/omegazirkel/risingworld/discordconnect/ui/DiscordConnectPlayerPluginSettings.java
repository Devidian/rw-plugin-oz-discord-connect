package de.omegazirkel.risingworld.discordconnect.ui;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import net.risingworld.api.objects.Player;

public class DiscordConnectPlayerPluginSettings extends PlayerPluginSettings {

    public DiscordConnectPlayerPluginSettings(String pluginVersion) {
        this.pluginLabel = DiscordConnect.name;
        this.pluginVersion = pluginVersion;
    }

    private I18n t() {
        return I18n.getInstance(DiscordConnect.name);
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                flexWrapper.addChild(shortcutSetting(uiPlayer));
            }

            protected OZUIElement shortcutSetting(Player uiPlayer) {
                OZUIElement element = defaultSettingsContainer();
                element.addChild(defaultSettingsLabel(t().get("TC_LABEL_DISCORD_CONNECT_SHORTCUT", uiPlayer)));
                boolean visible = shortcutVisible(uiPlayer);
                element.addChild(switchButtons(uiPlayer, visible, event -> {
                    if (DiscordConnect.playerSettings != null) {
                        DiscordConnect.playerSettings.setBoolean(uiPlayer.getDbID(), shortcutKey(), !visible);
                    }
                    redrawContent();
                }));
                return element;
            }
        };
    }

    public static boolean shortcutVisible(Player player) {
        return DiscordConnect.playerSettings == null
                || DiscordConnect.playerSettings.getBoolean(player.getDbID(), shortcutKey()).orElse(true);
    }

    private static String shortcutKey() {
        return PluginShortcutVisibility.playerSettingKey(DiscordConnect.name);
    }
}
