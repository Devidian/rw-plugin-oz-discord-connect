# Roadmap Plan 01 Compatibility

## Objective
Record that Roadmap Plan 01 introduces no direct Discord Connect implementation work unless later feature slices request Discord notifications.

## Ownership
Primary repository: `rw-plugin-oz-discord-connect`.

Supporting repositories:
- `rw-plugin-oz-tools` for shared settings reload/admin settings tab adoption if rolled out portfolio-wide.
- Feature plugins own their business events before any optional Discord delivery is added.

## Dependencies
- Hard dependency: `rw-plugin-oz-tools`.
- Optional event producers may include Admin Utils, LandClaim, Shop, Marketplace, GPS, or Rewards.

## Confirmed Decisions
- Roadmap Plan 01 feature events should support Discord messages through dedicated event channel ids where useful.
- Prison, Marketplace sale, and Claim sale events are desired candidates.
- Event channel ids should be separate by event category so admins can choose which events are sent.

## Work Packages
- [ ] Package 1: Adopt shared settings reload/admin settings tab metadata if the portfolio-wide prework is applied to all plugins.
- [ ] Package 2: Review optional notification requests only when a feature plugin explicitly needs Discord delivery.
- [ ] Package 3: Preserve the public API surface for sibling plugins that post status or event messages.

## Risks
- Discord Connect must not absorb Shop, Marketplace, prison, claim, or GPS business logic.
- Additional notifications can create config/key churn and should be grouped by feature owner.

## Validation Strategy
- Run existing Discord plugin validation after shared settings/admin-tab adoption.
- For future optional notifications, validate both missing/disabled Discord and configured Discord paths from the producing plugin.

## Affected Repositories/Plugins
- `rw-plugin-oz-discord-connect`
- `rw-plugin-oz-tools`
- optional future producing plugins

## Rollback Considerations
No Roadmap Plan 01 Discord behavior changes are planned. Future notification integrations must remain optional and removable by configuration.

## Open Questions
- None.
