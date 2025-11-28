package de.omegazirkel.risingworld;

import java.util.List;
import org.javacord.api.interaction.SlashCommandOption;

public record CmdDef(
    String name,
    String description,
    List<SlashCommandOption> options
) {}
