# OmegaZirkel Discord-Plugin for Rising World

**Build baseline:** JDK 25 (`--release 25`) and the bundled Rising World PluginAPI 0.9.3.2 JAR.

## Current features

- post ingame chat to Discord with usernames
- post server status messages (startup and shutdown) to Discord
- post support messages using `/support [message]` to a Discord channel
- every Discord channel can have its own webHook (chat, support and status)
- `/dc restart` and Discord `/restart` forward restart requests to Admin Utils when installed
- plugin detects changes to settings.<world>.json and reloads them. A message can be sent to discord if you like.
- plugin can report detected jar changes to Discord and in-game chat
- players can type /joinDiscord to join your discord if you configure this
- support messages now have a screenshot attached
- normal chat messages which contain `+screen` or `+s` as a token have a screenshot uploaded to discord
- normal chat messages which contain `+screennogui` or `+sng` as a token have a screenshot without interface
- normal chat messages which contain `+tp` or `+t` as a token include the player's current coordinates
- Public API for other plugins (status/event channel)

Admin Utils owns restart scheduling, permissions, locking, and execution.
Copy the former restart settings manually into Admin Utils' world JSON settings
before enabling its schedule. Without Admin Utils, both restart commands report
that the service is unavailable; other Discord Connect functions remain usable.

The bot uses JDA 6.4.2. Enable the privileged `MESSAGE_CONTENT` intent in the
Discord Developer Portal. Slash commands are registered per guild. Webhooks
continue to work when `botEnable=false`.

## Planned features

- currently none

## Commands ingame

| Command         | Description                                                    |
| --------------- | -------------------------------------------------------------- |
| /support [text] | sends [text] as support message to Discord                     |
| /dc restart     | forward a restart request to Admin Utils                       |
| /joinDiscord    | join the servers Discord server                                |
| /dc info        | Open the shared Tools Info/Status panel                        |
| /dc help        | Show plugin commands                                           |
| /dc status      | Open the shared Tools Info/Status panel                        |

The plugin radial menu entry opens the same shared Tools Info/Status panel using the portfolio-wide Info/Status icon.

## Commands discord

| Command                                 | Description                                                    |
| --------------------------------------- | -------------------------------------------------------------- |
| **informative commands:**               |                                                                |
| /help                                   | shows a list of all available commands                         |
| /online                                 | show a list of all players currently online                    |
| /getversion                             | show current version installed                                 |
| /getweather                             | shows the current ingame weather                               |
| /gettime                                | shows the current ingame time                                  |
| /getbanned                              | shows a list of banned players with name, UID and reason       |
| **administrative commands:**            |                                                                |
| /restart                                | forward a restart request to Admin Utils                       |
| /support [PLAYERNAME] [TEXT]            | sends a text message to a player (must be online)              |
| /kick [PLAYERNAME] [REASON?]            | kick player with an optional reason                            |
| /ban [PLAYERNAME] [REASON?]             | ban a player with an optional reason                           |
| /group [PLAYERNAME] [GROUP]             | set player group                                               |
| /yell [TEXT]                            | send a text to the server as yell message                      |
| /bc [TYPE] [TEXT]                       | send a broadcast message with identifier [TYPE] to all players |
| /unban [UID]                            | unban a player by his uid                                      |
| /teleporttoplayer [PLAYER_A] [PLAYER_B] | teleport Player A to Player B if both are online               |
| /makeadmin [PLAYER]                     | grant player admin status                                      |
| /unadmin [PLAYER]                       | revoke player admin status                                     |
| /setweather [Weather]                   | set weather on the server                                      |
| /settime [HOUR] [MINUTE]                | set current ingame time                                        |
| /sethealth [PLAYER] [VALUE]             | set players health to value                                    |
| /sethunger [PLAYER] [VALUE]             | set players hunger to value                                    |
| /setthirst [PLAYER] [VALUE]             | set players thirst to value                                    |

## Installation (prebuild)

Download the latest zip files from [here (shared)](https://github.com/Devidian/oz_rw_plugin_tools/releases) and [here (Discord Plugin)](https://github.com/Devidian/oz_rw_plugin_discord/releases) and unpack them into your plugins folder.

### Filetree

Should look like this:

```css
    ── dedicated-server
        ├── Plugins
        │    ├── OZDiscordConnect
        │    │    ├── i18n
        │    │    │    └── ...
        │    │    ├── HISTORY.md
        │    │    ├── OZDiscordConnect.jar
        │    │    ├── README.md
        │    │    └── settings.<world>.json
        │    ├── OZTools
        │    │    ├── assets
        │    │    │    └── ...
        │    │    ├── lib
        │    │    │    └── \*.jar
        │    │    ├── HISTORY.md
        │    │    ├── OZTools.jar
        │    │    ├── README.md
        │    │    └── settings.<world>.json
        :    :
```

## Build (Maven)

TODO

### Installation after build

just copy `dist/OZDiscordConnect` folder after build into your plugin folder, thats it!

## Configuration

The settings.<world>.json contains all you need to configure this plugin

| setting                     | default         | description                                                                                                        |
| --------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------ |
| **General plugin settings** |                 |                                                                                                                    |
| logLevel                    | 0               | Logging to server console higher values means less output 0=all (debug)                                            |
| sendPluginWelcome           | false           | -                                                                                                                  |
| **JDA bot settings**        |                 |                                                                                                                    |
| botEnable                   | false           | Enables usage of DiscordBot                                                                                        |
| botSecure                   | true            | Only Bot owner can use commands if `true`                                                                          |
| botToken                    |                 | the token for your bot                                                                                             |
| botLang                     | en              | -                                                                                                                  |
| botChatChannelName          | server-chat     | -                                                                                                                  |
| botAdmins                   |                 | comma-separated exact Discord user snowflake IDs; names and partial IDs are rejected                              |
| **other plugin settings**   |                 |                                                                                                                    |
| allowScreenshots            | true            | -                                                                                                                  |
| joinDiscord                 |                 | the code to join discord (not the full url!)                                                                       |
| **Chat settings**           |                 |                                                                                                                    |
| postChat                    | false           | if true, chat is posted to the webHook for Chat                                                                    |
| webHookChatUrl              |                 | this is the webHook used for ingame chat                                                                           |
| overrideAvatar              | true            | -                                                                                                                  |
| **Status settings**         |                 |                                                                                                                    |
| useServerName               | false           | if true, the servername is used as username for status posts                                                       |
| reportStatusEnabled         | true            | if true, a message will be posted when the plugin is enabled (server boot)                                         |
| reportStatusDisabled        | true            | if true, a message will be posted when the plugin is disabled (server shutdown)                                    |
| reportSettingsChanged       | true            | if true, a message will be posted if settings.<world>.json has changed                                               |
| reportJarChanged            | true            | if true, a message will be posted if the jar file has changed (plugin update for example)                          |
| statusEnabledMessage        |                 | the message that will be posted to discord on plugin enable                                                        |
| statusDisabledMessage       |                 | the message that will be posted to discord on plugin disable                                                       |
| statusUsername              |                 | the fixed username to use for status messages                                                                      |
| webHookStatusUrl            |                 | this is the webHook used for status messages                                                                       |
| **Support settings**        |                 |                                                                                                                    |
| postSupport                 | false           | -                                                                                                                  |
| supportScreenshot           | true            | -                                                                                                                  |
| webHookSupportUrl           |                 | this is the webHook used for support messages                                                                      |
| addTeleportCommand          | true            | if true, a teleport command is added to the support message                                                        |
| **Color & Chat settings**   |                 |                                                                                                                    |
| maxScreenWidth              | 1920            | -                                                                                                                  |
| colorizeChat                | true            | -                                                                                                                  |
| showGroup                   | false           | -                                                                                                                  |
| colorSupport                | <color=#782d8e> | -                                                                                                                  |
| colorLocalSelf              | <color=#ddffdd> | -                                                                                                                  |
| colorLocalAdmin             | <color=#db3208> | -                                                                                                                  |
| colorLocalOther             | <color=#dddddd> | -                                                                                                                  |
| colorLocalDiscord           | <color=#ddddff> | -                                                                                                                  |

## Contributor Workflow

- Review `AGENTS.md`, `PLANS.md`, `.codex/agents.toml`, and `.codex/skills/` before making structural changes.
- Verify Rising World API usage with `scripts/verify-plugin-api.sh` when adding or changing API calls.
- Run `mvn -B -DskipTests package` and `mvn -B test` before release-facing changes are merged.
- Use `RUNTIME_TESTING.md` and `scripts/docker-runtime-smoke.sh <PluginFolderName>` for runtime smoke tests when behavior changes need server validation.
- Keep `README.md` and `HISTORY.md` current and use Conventional Commit titles for commits and PRs.

## JSON-only distribution

Settings defaults (`settings.default.json`) and translations (`i18n/*.json`)
are shipped only as JSON. Legacy default and translation `.properties` files
are no longer included. Runtime settings remain world-scoped as
`settings.<world>.json`; migration of an existing `settings.properties` and
its backup remains supported. Updating the package does not delete old files
already present on the server. Use `mvn clean package` for a fresh local
package; ZIP assembly also excludes stale legacy settings and translations.
