# Forward Discord restart requests to Admin Utils and report status
<!-- codemap-id: rw-plugin-oz-discord-connect/admin-restart -->

## Responsibility

Forward Discord restart requests to Admin Utils and report status. Admin Utils owns restart execution. This adapter invokes its facade reflectively.

## Source and tests

- [`AdminRestartBridge`](../../src/de/omegazirkel/risingworld/discordconnect/AdminRestartBridge.java)
- [`DiscordCommandService`](../../src/de/omegazirkel/risingworld/discordconnect/DiscordCommandService.java)
- [`RestartStatusContractTest`](../../src-test/de/omegazirkel/risingworld/discordconnect/RestartStatusContractTest.java)

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.
