# Bugs 05 Discord Threading

## Objective
Complete the remaining Discord callback separation and runtime validation after
removing foreign-thread Rising World API access.

## Ownership
Owning repository/plugin: `rw-plugin-oz-discord-connect`
Supporting repositories/plugins: `rw-plugin-oz-tools`

## Dependencies
- Runtime: Rising World development server, Discord bot, and configured webhooks
- Build: OZTools `0.21.1`, Java 20, and Maven
- Optional integrations: Global Intercom

## Risks
- Discord slash-command response timing and restart commands require controlled
  development-server validation.

## Validation Strategy
- [x] `mvn -B test`
- [x] `mvn -B -DskipTests package`
- [x] Runtime-test game-to-Discord messages and screenshots
- [x] Runtime-test Discord-to-game messages
- [ ] Runtime-test bot commands, screenshots, webhooks, restart warnings, and
  reload/shutdown behavior
- [ ] Run sustained Discord activity during the native-crash soak

## Affected Repositories/Plugins
- `rw-plugin-oz-discord-connect`
- `rw-plugin-oz-tools`

## Rollback Considerations
Do not restore direct JavaCord/timer-thread game API calls. Revert individual
command DTO changes only if response compatibility regresses.

## Implementation Checklist
- [x] Dispatch JavaCord listeners and timer game operations
- [x] Remove retained Player access from screenshot callbacks
- [x] Remove blocking JavaCord response waits from the server thread
- [x] Move webhook and text-channel transport onto a bounded lifecycle-owned worker
- [ ] Split slash-command input and output into immutable request/result DTOs
- [ ] Add focused command-boundary regression tests
- [ ] Complete development-server runtime validation

The DTO split and its focused tests are deferred until after the patch-release
soak. The release boundary already dispatches all slash-command game operations
onto the server thread and avoids blocking waits on Discord responses.
