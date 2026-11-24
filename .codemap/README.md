# rw-plugin-oz-discord-connect
<!-- codemap-id: rw-plugin-oz-discord-connect -->

## Purpose

Discord bot, webhook and in-game message integration. The [plugin descriptor](../src/resources/plugin.yml) names [DiscordConnect](../src/de/omegazirkel/risingworld/DiscordConnect.java) as its lifecycle entry.

## Modules

- [discord](modules/discord.md) — Discord connection, commands, forwarding and public integration surface.
- [admin-restart](modules/admin-restart.md) — Forward Discord restart requests to Admin Utils and report status.

## Build and tests

[Maven](../pom.xml) defines the Java build. Follow [repository validation](../AGENTS.md) for packaging, API checks and runtime validation. Relevant automated tests are under [src-test](../src-test/).

## Dependencies

- `rw-plugin-oz-tools/shared-api` — build and runtime shared infrastructure. [Evidence](../pom.xml)
- `rw-plugin-oz-admin-utils/moderation` — optional restart integration. [Evidence](../src/de/omegazirkel/risingworld/discordconnect/AdminRestartBridge.java)
- `rw-plugin-oz-global-intercom/channels` — optional duplicate-chat filtering through `isGIMessage`. [Call site](../src/de/omegazirkel/risingworld/DiscordConnectRuntime.java)
