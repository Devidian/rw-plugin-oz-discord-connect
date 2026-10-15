# Discord topic threads for plugin events

## Objective

Route event notifications from multiple Rising World plugins into named Discord threads within a shared text channel. By default, each plugin uses its `plugin.yml` display name as its thread name (for example, `OZ - GPS`). Per-route `PluginSettings` fields may override that name. The same configured name in two plugins and the same parent channel must resolve to the same thread.

The existing channel-message API remains valid: calls without `topic` continue to post directly to the channel. Event consumers opt into threads by passing their resolved topic.

## Ownership

Owning repository/plugin: `rw-plugin-oz-discord-connect` (public API, thread routing, transport, persisted thread registry).

Supporting repositories/plugins: `rw-plugin-oz-tools` (optional reflection bridge); Admin Utils, Bosses, GPS, Land Claim, Mail, Player Factions, Rewards, and Stargate (settings and event calls). Inspect Service NPC's dormant Discord bridge before including it in the migration. Keep each consumer's event classification and settings in its own repository.

## Contract and configuration

Implementation decision: topic identity is case-sensitive after trimming outer whitespace. The registry is `discord-threads.json` in the Discord Connect plugin data directory and is written atomically. A saved ID remains authoritative after a rename. On deletion, one exact-name public thread is adopted or a replacement is created; duplicate exact names fail. A send that reports a deleted channel fails and clears the mapping, so the next send recovers. Archived threads are reopened, and locked or inaccessible threads fail. Discord Connect never routes a topic event to the parent channel as a fallback.

The root Development upload script now bootstraps the current OZ Tools source for Stargate builds; its former pinned Tools 0.26.2 archive would not satisfy the topic bridge dependency. It also preserves `discord-threads.json` during Discord Connect updates and uploads Mail's new default settings while preserving world settings.

Admin Utils has unrelated satellite-map edits in its checkout. A clean temporary package containing only this Discord change passed 46 tests at `/home/maik/.tmp/discord-topic-admin-utils-isolated/dist/OZAdminUtils`; use it through `ADMIN_UTILS_PREBUILT_DIR` if that plugin is later selected for a Development upload.

- Add overloads such as `sendDiscordMessageToTextChannel(String message, long channelId, byte[] image, String topic)` and corresponding `DiscordBridge.sendTextMessage(...)` overloads. Keep the old signatures and behavior for binary and runtime compatibility. If an older Discord Connect lacks the new overload, the bridge returns failure and logs it; it must not silently send a topic-routed event to the parent channel.
- `topic == null` or blank means direct-channel delivery at the Discord Connect API boundary. Consumer plugins resolve their route's blank/unset override to their own `plugin.yml` display name before calling the new overload. This keeps the public API independent of the caller's identity.
- Add one optional string topic setting per existing event route in each consuming plugin's `PluginSettings`, alongside its channel ID and Admin Settings entry. Suggested key pattern: the existing channel key with `ChannelId` replaced by `ThreadTopic`, or `<route>.threadTopic` where dotted keys are already used. An empty value means the plugin-name default. A nonempty value is the exact shared thread name. Document an example with two plugins using one parent channel and the same topic.
- Scope identity by `(parentChannelId, normalizedTopic)`; do not merge threads across channels. Trim and validate topic names against Discord's thread-name limits. Specify case handling consistently and preserve the chosen display name. Reject invalid values with a clear log entry rather than silently posting to the parent channel.
- Discord Connect owns a persistent `(parentChannelId, topic) -> threadId` registry in its own runtime data. Serialize creation through the existing transport worker to prevent duplicate threads when different plugins post simultaneously. On restart, resolve saved IDs; if a thread was deleted, recover explicitly. If adopting a manually created thread by name, accept only an unambiguous match and persist its ID. Define behavior for archived, locked, inaccessible, and duplicate-name threads; log failures without a parent-channel fallback.
- Use a bot-created public thread for ordinary text channels. Existing webhook routes need an explicit mapping to the same thread ID and a `thread_id` query parameter; `thread_name` is not a reusable thread selector for ordinary text channels. Preserve webhook username, avatar, and attachment handling. Keep chat/support/status routing out of the first consumer migration unless deliberately configured for threads.

## Dependencies

- Runtime: Discord bot enabled for thread creation and lookup, with permissions to create public threads and send messages in threads; webhook credentials where used. Discord's archive/lock behavior and channel limits must be handled.
- Build: Java 25, current JDA supplied by OZ Tools, existing Maven baselines. No new library is planned.
- Optional integrations: consumers use reflection and must continue working when Discord Connect is absent or older.

## Affected Repositories/Plugins

Service NPC currently has only a dormant bridge class and no active Discord event sender. Its two existing channel settings now have matching localized thread-topic settings; message delivery remains a separate Service NPC task once its notification events are defined.

- `rw-plugin-oz-discord-connect`: public overloads, thread registry/resolver, JDA and webhook dispatch, settings/docs.
- `rw-plugin-oz-tools`: additive `DiscordBridge` overloads with a compatibility check for older Discord Connect versions.
- `rw-plugin-oz-admin-utils`, `rw-plugin-oz-bosses`, `rw-plugin-oz-gps`, `rw-plugin-oz-land-claim`, `rw-plugin-oz-mail`, `rw-plugin-oz-player-factions`, `rw-plugin-oz-rewards`, `rw-plugin-oz-stargate`: route settings and opt-in event calls.
- `rw-plugin-oz-service-npc`: inspect whether its existing bridge/settings have an active event sender; migrate only active paths.

## Risks

- The consumer migration changes where existing events appear. Keep old API calls intact and roll out consumers only after Discord Connect and OZ Tools are installed and validated.
- Names alone are not stable identifiers: duplicate names, manual renames, deletion, restart, and concurrent sends need deterministic registry and recovery rules.
- Archived or locked threads, missing permissions, and Discord rate limits may block delivery. Report the failing channel/topic and API error; create a replacement only under the documented deletion-recovery rule, and never spill events into the parent channel.
- Webhook and bot paths differ. Verify both before claiming parity, including image attachments and webhook identity.

## Validation Strategy

Local validation on 2026-10-15: Java 25 with a writable task-local Maven repository passed `test package` for Tools, Discord Connect, Bosses, GPS, Land Claim, Mail, Player Factions, Rewards, Stargate, and Admin Utils. Discord Connect passed 18 tests; Tools passed 36. Mail and Player Factions used writable temporary source copies because their generated output directories were not writable. The isolated Admin Utils package passed 46 tests. All changed JSON files parse, `git diff --check` passed in each changed checkout, and `bash -n dev-upload.sh` passed.

Development upload on 2026-10-15: `dev-upload.sh` installed Tools, Discord Connect, and all eight active event consumers. Admin Utils used the isolated Discord-only package. A pre-upload archive is on the Development host at `/tmp/discord-topic-preupload-20261015.tar.gz`. The Development server reported `RELOADED ALL PLUGINS` after the final upload, Discord Connect 0.25.2 reported connection ready and JDA ready with the separate Development bot, and `rw-development` was healthy. All ten JAR hashes changed while all ten `settings.New_World.json` hashes matched the pre-upload archive. No topic event has been triggered yet, so live thread destination and failure-case checks remain open.

Development correction on 2026-10-15: nine consumer packages were uploaded again after adding DE/EN topic labels, a dedicated Land Claim Discord settings group, and two Service NPC topic fields. The last automatic reload completed, the Development bot was ready, and the container remained healthy. All nine German locale files contain their topic keys. Land Claim, Mail, Factions, Rewards, and Stargate world settings still matched the pre-upload backup; Admin Utils, Bosses, and GPS had intervening Discord configuration changes and the upload script excluded their world settings. Service NPC world settings retained their pre-upload modification time. The Service NPC fields remain configuration-only because the plugin has no active Discord sender.

- [x] Unit tests for topic normalization, channel-scoped identity, persisted IDs, duplicate prevention, saved-ID recovery selection, and old API signatures.
- [x] `mvn -B test` and `mvn -B -DskipTests package` in each changed repository; consumer builds use the updated OZ Tools API. Mail and Factions were built from temporary source copies because existing generated output was not writable.
- [ ] Development Discord test: first send creates one thread; second send and a different plugin using the same channel/topic reuse it; different channel or topic creates a separate thread.
- [ ] Development Discord test: restart, archive/unarchive, deletion, lock, missing permissions, bot unavailable, and webhook text/image delivery. Verify that failures do not post into the parent channel.
- [ ] Compare Discord message destinations and runtime logs with the configured route settings. No Production rollout is included in this plan.

## Rollback Considerations

Preserve existing channel IDs, webhook URLs, and method signatures. Roll a consumer back to the old call to restore parent-channel delivery without deleting Discord threads or the registry. Back up the registry before format changes; any release/deployment must follow the workspace's separate authorization process.

## Implementation Checklist

- [x] Finalize topic normalization, registry storage, and recovery rules in Discord Connect documentation.
- [x] Implement additive Discord Connect API, thread creation/reuse, persistent mapping, and transport error reporting.
- [x] Extend webhook routing by thread ID while retaining current payload and attachment behavior.
- [x] Extend OZ Tools' reflection bridge with an additive topic overload and old-version handling; update the Bosses-local bridge as needed.
- [x] Add per-route topic fields and Admin Settings entries to active consumer plugins; default unset/blank overrides to each plugin's display name.
- [x] Add German and English labels/descriptions for every new topic setting, group Land Claim's Discord controls, and add matching Service NPC topic settings.
- [x] Update active event senders in the eight consumer plugins; Service NPC has no active event sender.
- [ ] Validate cross-plugin topic sharing and failure cases in Development Discord.
- [x] Update each changed repository's README, HISTORY, and PLANS or active docs according to its local rules.
- [x] Upload the ten changed plugins to Development with `dev-upload.sh`, preserve world settings, and verify reload and Development bot readiness.
- [x] Review release order: OZ Tools 0.28.2, Discord Connect 0.25.2, then consumers; release and deployment remain separately authorized.
