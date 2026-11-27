# Discord connection, commands, forwarding and public integration surface
<!-- codemap-id: rw-plugin-oz-discord-connect/discord -->

## Responsibility

Discord connection, commands, forwarding and public integration surface. JDA transports and feature-plugin calls meet in the runtime. Review permissions, consent and external message boundaries when changing forwarding.

## Source and tests

- [`DiscordConnectRuntime`](../../src/de/omegazirkel/risingworld/DiscordConnectRuntime.java)
- [`JdaDiscordClient`](../../src/de/omegazirkel/risingworld/discordconnect/JdaDiscordClient.java)
- [`DiscordCommandService`](../../src/de/omegazirkel/risingworld/discordconnect/DiscordCommandService.java)
- [`DiscordThreadRegistry`](../../src/de/omegazirkel/risingworld/discordconnect/DiscordThreadRegistry.java)
- [`DiscordBoundaryTest`](../../src-test/de/omegazirkel/risingworld/discordconnect/DiscordBoundaryTest.java)
- [`DiscordWebhookTransportTest`](../../src-test/de/omegazirkel/risingworld/DiscordWebhookTransportTest.java) covers failed image submission and worker shutdown.

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.
