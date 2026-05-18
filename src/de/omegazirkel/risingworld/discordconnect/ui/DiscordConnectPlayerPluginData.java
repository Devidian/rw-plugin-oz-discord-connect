package de.omegazirkel.risingworld.discordconnect.ui;

import de.omegazirkel.risingworld.DiscordConnect;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginDataPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginData;
import net.risingworld.api.objects.Player;

public class DiscordConnectPlayerPluginData extends PlayerPluginData {

    public DiscordConnectPlayerPluginData(String pluginVersion) {
        this.pluginLabel = DiscordConnect.name;
        this.pluginVersion = pluginVersion;
    }

    @Override
    public BasePlayerPluginDataPanel createPlayerPluginDataUIElement(Player uiPlayer) {
        return new BasePlayerPluginDataPanel(uiPlayer, pluginLabel) {
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                flexWrapper.addChild(defaultEmptyStateLabel());
            }
        };
    }
}
