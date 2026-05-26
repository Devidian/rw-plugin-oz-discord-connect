# Roadmap Plan 03 Menu Info Button

## Objective
Add a Discord Connect radial-main-menu button that opens the existing shared Tools Info/Status panel.

## Ownership
Primary repository: `rw-plugin-oz-discord-connect`

Supporting repository:
- `rw-plugin-oz-tools` for the shared Info/Status panel contract.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- No Discord webhook or bot contract changes are planned for this package.

## Phases
- [x] Phase 1: Add the Info/Status action to the plugin's main radial menu.
- [x] Phase 2: Reuse the existing Info/Status provider and command behavior.
- [x] Phase 3: Update README/HISTORY and validate.

## Risks
- Menu changes should not alter Discord command, webhook, or bot behavior.

## Validation Strategy
- Run `mvn -B -DskipTests package`.
- Run `mvn -B test`.
- Runtime-smoke the radial button and existing `/dc info` or `/dc status` behavior.

## Affected Repositories/Plugins
- `rw-plugin-oz-discord-connect`
- `rw-plugin-oz-tools`

## Rollback Considerations
The radial button can be removed without changing Discord runtime behavior.

## Progress Notes
- Phase 1 complete: Discord Connect registers a radial menu entry with the Tools-provided `icon-ki-info-status` icon.
- Phase 2 complete: the radial entry opens the existing shared Tools Info/Status provider; `/dc info` and `/dc status` are unchanged.
- Phase 3 complete: README/HISTORY were updated.
- Validation passed with `mvn -B test` and `mvn -B -DskipTests package`.
