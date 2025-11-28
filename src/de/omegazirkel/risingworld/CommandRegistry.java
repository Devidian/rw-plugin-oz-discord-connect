package de.omegazirkel.risingworld;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.ApplicationCommandBuilder;
import org.javacord.api.interaction.SlashCommandBuilder;

public class CommandRegistry {

    /**
     * syncronize commands for all servers the api is connected to
     */
    public static void syncCommandsForAllServers(DiscordApi api, List<CmdDef> commands) {
        api.getServers().forEach(server -> syncCommandsForServer(api, server, commands));
    }

    /**
     * syncronize commands for a single server
     */
    public static void syncCommandsForServer(DiscordApi api, Server server, List<CmdDef> requiredCommands) {
        JavaCordBot.logger().info("🔄 Syncing slash commands for server: " + server.getName());

        try {
            Set<ApplicationCommandBuilder<?, ?, ?>> builders = new HashSet<>();

            for (CmdDef def : requiredCommands) {

                SlashCommandBuilder builder = new SlashCommandBuilder()
                        .setName(def.name())
                        .setDescription(def.description());

                def.options().forEach(builder::addOption);

                builders.add(builder);
            }

            // Overwrite all server commands with exactly these
            api.bulkOverwriteServerApplicationCommands(server, builders);
            JavaCordBot.logger().info("✅ Command sync complete for server " + server.getName());
        } catch (Exception e) {
            JavaCordBot.logger().error("❌ Failed to sync commands: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
