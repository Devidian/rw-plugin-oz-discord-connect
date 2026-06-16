package de.omegazirkel.risingworld.discordconnect;

import java.util.List;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public record CmdDef(String name, String description, List<OptionData> options) {
}
