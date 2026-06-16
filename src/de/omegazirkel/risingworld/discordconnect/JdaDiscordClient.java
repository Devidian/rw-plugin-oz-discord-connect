package de.omegazirkel.risingworld.discordconnect;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import de.omegazirkel.risingworld.DiscordConnect;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildUnavailableEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;

public final class JdaDiscordClient implements EventListener, AutoCloseable {
    private final DiscordConnect plugin;
    private final PluginSettings settings;
    private final Map<String, InteractionHook> responses = new ConcurrentHashMap<>();
    private volatile JDA jda;
    private volatile String ownerId;
    private volatile boolean accepting;

    public JdaDiscordClient(DiscordConnect plugin) {
        this.plugin = plugin;
        this.settings = PluginSettings.getInstance();
    }

    public void start() throws InterruptedException {
        accepting = true;
        JDA connection = JDABuilder.createLight(settings.botToken,
                GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build();
        jda = connection;
        connection.awaitReady();
        ownerId = connection.retrieveApplicationInfo().complete().getOwner().getId();
        connection.getGuilds().forEach(CommandRegistry::syncCommands);
        updateActivity("Running, waiting for players!");
        DiscordConnect.logger().info("JDA Discord client is ready");
    }

    public boolean isReady() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    public void stopAccepting() {
        accepting = false;
    }

    public void updateActivity(String activity) {
        JDA connection = jda;
        if (connection != null) {
            connection.getPresence().setActivity(Activity.playing(activity));
        }
    }

    public void sendChannelMessage(long channelId, String message, byte[] image) {
        JDA connection = jda;
        if (connection == null) {
            DiscordConnect.logger().warn("Cannot send Discord channel message while JDA is stopped");
            return;
        }
        TextChannel channel = connection.getTextChannelById(channelId);
        if (channel == null) {
            DiscordConnect.logger().error("Discord text channel " + channelId + " was not found");
            return;
        }
        if (image == null) {
            channel.sendMessage(message).complete();
        } else {
            channel.sendMessage(message).addFiles(FileUpload.fromData(image, "screenshot.jpg")).complete();
        }
    }

    public void sendResult(DiscordCommandResult result) {
        InteractionHook hook = responses.remove(result.responseToken());
        if (hook == null) {
            DiscordConnect.logger().warn("Discord interaction expired or was already answered");
            return;
        }
        try {
            if (result.fileContent() == null) {
                hook.editOriginal(result.content()).complete();
            } else {
                hook.editOriginal(result.content())
                        .setFiles(FileUpload.fromData(result.fileContent(), result.fileName()))
                        .complete();
            }
        } catch (RuntimeException ex) {
            DiscordConnect.logger().warn("Failed to answer Discord interaction: " + ex.getMessage());
        }
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof MessageReceivedEvent message) {
            onMessage(message);
        } else if (event instanceof SlashCommandInteractionEvent command) {
            onCommand(command);
        } else if (event instanceof GuildReadyEvent ready && accepting) {
            CommandRegistry.syncCommands(ready.getGuild());
            DiscordConnect.logger().info("Discord guild available: " + ready.getGuild().getName());
        } else if (event instanceof GuildUnavailableEvent unavailable) {
            DiscordConnect.logger().warn("Discord guild unavailable: " + unavailable.getGuild().getName());
        } else if (event instanceof ReadyEvent) {
            DiscordConnect.logger().info("Discord connection ready");
        } else if (event instanceof SessionResumeEvent) {
            DiscordConnect.logger().info("Discord connection reconnected");
        } else if (event instanceof SessionDisconnectEvent) {
            DiscordConnect.logger().warn("Discord connection disconnected");
        } else if (event instanceof ExceptionEvent exception) {
            Throwable cause = exception.getCause();
            DiscordConnect.logger().warn("JDA callback exception: " + (cause == null ? "unknown" : cause.getMessage()));
        }
    }

    private void onMessage(MessageReceivedEvent event) {
        if (!accepting || !event.isFromGuild() || event.getAuthor().isBot()) {
            return;
        }
        Guild guild = event.getGuild();
        DiscordChatMessage message = new DiscordChatMessage(
                event.getMessage().getContentDisplay(),
                event.getAuthor().getId(),
                event.getMember() == null ? event.getAuthor().getName() : event.getMember().getEffectiveName(),
                isAdmin(event.getAuthor().getId()),
                guild.getIdLong(),
                guild.getName(),
                event.getChannel().getIdLong(),
                event.getChannel().getName());
        plugin.dispatchDiscordChat(message);
    }

    private void onCommand(SlashCommandInteractionEvent event) {
        if (!accepting) {
            event.reply("Plugin is disabled").setEphemeral(true).queue();
            return;
        }
        if (!event.isFromGuild()) {
            event.reply("Commands are only available in a server").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue(hook -> {
            String token = UUID.randomUUID().toString();
            responses.put(token, hook);
            Map<String, String> options = event.getOptions().stream()
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            option -> option.getName().toLowerCase(),
                            option -> option.getAsString()));
            DiscordCommandRequest request = new DiscordCommandRequest(
                    token,
                    event.getName().toLowerCase(),
                    options,
                    event.getUser().getId(),
                    event.getMember() == null ? event.getUser().getName() : event.getMember().getEffectiveName(),
                    isAdmin(event.getUser().getId()),
                    event.getGuild().getIdLong(),
                    event.getGuild().getName());
            if (!plugin.dispatchDiscordCommand(request)) {
                sendResult(DiscordCommandResult.text(token, "Plugin is shutting down or its command queue is full"));
            }
        }, error -> DiscordConnect.logger().warn("Failed to defer Discord command: " + error.getMessage()));
    }

    private boolean isAdmin(String userId) {
        return userId.equals(ownerId) || settings.botAdminIds.contains(userId);
    }

    @Override
    public void close() {
        stopAccepting();
        responses.values().forEach(hook -> hook.editOriginal("Plugin was disabled before the command completed").queue());
        responses.clear();
        JDA connection = jda;
        jda = null;
        if (connection == null) {
            return;
        }
        connection.getPresence().setActivity(Activity.playing("Shutting down..."));
        connection.shutdown();
        try {
            if (!connection.awaitShutdown(Duration.ofSeconds(5))) {
                connection.shutdownNow();
                connection.awaitShutdown(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            connection.shutdownNow();
            Thread.currentThread().interrupt();
        }
        DiscordConnect.logger().info("JDA Discord client stopped");
    }
}
