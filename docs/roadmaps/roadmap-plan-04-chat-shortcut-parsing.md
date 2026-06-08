# Roadmap Plan 04 Chat Shortcut Parsing

## Objective
Fix Discord Connect chat shortcut parsing so `+s`, `+sng`, and `+t` work when they appear at the end of a message.

## Ownership
Primary repository: `rw-plugin-oz-discord-connect`

Supporting repositories:
- `rw-plugin-oz-tools` for shared player settings, i18n, persistence, and overlay behavior.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- Existing Discord webhook/bot configuration remains unchanged.

## Phases
- [x] Phase 1: Locate current chat shortcut parsing for `+s`, `+sng`, and `+t`.
- [x] Phase 2: Accept shortcuts followed by whitespace or end-of-message, preserving existing behavior when normal text follows.
- [x] Phase 3: Add focused parser coverage for shortcut-only messages and shortcuts at the end of text.
- [x] Phase 4: Add Plan 04 player shortcut visibility setting, document the Escape-close API limitation, verify i18n loading, and migration away from deprecated Tools `SQLite` usage if present.
- [x] Phase 5: Update README/HISTORY and validate.

## Implementation Notes
- Chat shortcut parsing is isolated in `ChatShortcutParser` and matches shortcuts only as chat tokens followed by whitespace or end-of-message.
- `+s`, `+sng`, and `+t` now work at the end of a message; existing long forms `+screen`, `+screennogui`, and `+tp` remain supported.
- Discord Connect now registers player-aware shortcut visibility and persists the setting through shared `PlayerSettings`.
- The plugin had no deprecated Tools `SQLite` usage; Plan 04 persistence uses `SQLiteConnectionFactory`.

## Risks
- Parser changes can accidentally match ordinary chat text; matching should stay token-aware.
- Discord routing behavior should not change beyond the shortcut boundary fix.

## Validation Strategy
- Run `mvn -B test` and `mvn -B -DskipTests package`.
- Runtime-smoke `+s`, `+sng`, `+t`, each with trailing whitespace, at end-of-message, and followed by normal text.

## Affected Repositories/Plugins
- `rw-plugin-oz-discord-connect`
- `rw-plugin-oz-tools`

## Rollback Considerations
Keep the parser change localized. If ambiguity appears, restore strict matching and add explicit command alternatives rather than changing Discord delivery paths.
