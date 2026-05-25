# Roadmap Plan 02 Plugin Standardization

## Objective
Adopt Roadmap Plan 02 portfolio standards for logger naming, admin settings visibility, localized settings text, and standardized plugin info/status panels.

## Ownership
Primary repository: `rw-plugin-oz-discord-connect`.

Supporting repository: `rw-plugin-oz-tools`.

## Work Packages
- [x] Package 1: Collapse specialized loggers into one main Discord Connect logger.
- [x] Package 2: Verify every safe `settings.default.properties` key appears in the admin `PluginSettings` tab.
- [x] Package 3: Mark list/enum settings as read-only where editing is not yet supported.
- [x] Package 4: Add missing English and German i18n labels/descriptions for settings.
- [x] Package 5: Group related settings with labels such as general settings, Discord bot settings, webhook/channel settings, and restart settings.
- [x] Package 6: Add Discord Connect info/status panel content and redirect existing info/status commands to the shared Tools panel.

## Validation Strategy
- Run Maven package and tests.
- Verify secrets, tokens, and sensitive webhook values remain hidden.
- Verify info/status panel opens from radial menu and commands.

## Progress Notes
- Package 1 is complete: Discord Connect helper loggers already route to `OZ.DiscordConnect`, and settings reload now sets the main logger level once.
- Packages 2-5 are complete for Root Step 9: Discord Connect admin settings cover every safe default key, grouped separators are present, sensitive tokens/webhooks stay hidden, list/enum settings without validated editing are read-only, and English/German setting labels are available.
- Package 6 is complete for Root Step 10: Discord Connect now registers a shared Tools Info/Status provider and routes `/dc info` and `/dc status` to the shared panel without exposing secret values.

## Affected Repositories/Plugins
- `rw-plugin-oz-discord-connect`
- `rw-plugin-oz-tools`
