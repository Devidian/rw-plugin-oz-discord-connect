package de.omegazirkel.risingworld.discordconnect.ui;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class DiscordConnectPlayerPluginSettings extends PlayerPluginSettings {

    public DiscordConnectPlayerPluginSettings() {
        this.pluginLabel = DiscordConnect.name;
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                // TODO: implement actual settings content for Discord Connect plugin
                UILabel placeholderLabel = new UILabel("OZ - Discord Connect plugin settings will be here.");
                flexWrapper.addChild(placeholderLabel);
            }
        };
    }

}
