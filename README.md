# OmegaZirkel Discord-Plugin for Rising World

## Current features

- post ingame chat to Discord with usernames
- post server status messages such as login, logout, startup and shutdown to Discord
- post support messages using `/support [message]` to a Discord channel
- every Discord channel can have its own webHook (chat, support and status)
- admins can trigger server restart with `/ozrestart` that is executed after the last player left the server (it causes server shutdown, you need to be sure that server comes up automatically after that)
- player can trigger server restart too (if you confgure it), but only if they have spent an amount of real-time on your server that you can define (default:1 day, if you play 2h a day you need 12 days to achive this)
- plugin detects changes to settings.properties and reloads them. A message can be sent to discord if you like.
- plugin detects changes to jar files and sets restart flag if you like. Can also report this to discord and ingame chat
- players can type /joinDiscord to join your discord if you configure this
- support messages now have a screenshot attached
- normal chat messages wich contain `+screen` or `+s ` (include space) have a screenshot uploaded to discord
- normal chat messages wich contain `+screennogui` or `+sng ` (include space) have a screenshot without interface
- `Badass Tracker` tracks kills (mounts, non hostile animals) and remove/destroy of objects
- Public API for other plugins (status/event channel)
- Multiple server restarts can be scheduled (sets restart flag and restarts if all players have left, sends message to all players to inform them)
- Scheduled restart can be forced after atleast one minute time, players will be kicked before server restarts

## Planned features

- currently none

## Commands ingame

| Command         | Description                                                    |
| --------------- | -------------------------------------------------------------- |
| /support [text] | sends [text] as support message to Discord                     |
| /dc restart     | set restart flag to shutdown server after last player has left |
| /joinDiscord    | join the servers Discord server                                |
| /dc info        | Show plugin description                                        |
| /dc help        | Show plugin commands                                           |
| /dc status      | Show current plugin status (version, connection, language,...) |

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
| /restart                                | set restart flag to shutdown server after last player has left |
| /support [PLAYERNAME] [TEXT]            | sends a text message to a player (must be online)              |
| /kick [PLAYERNAME] [REASON?]            | kick player with an optional reason                            |
| /ban [PLAYERNAME] [REASON?]             | ban a player with an optional reason                           |
| /group [PLAYERNAME] [GROUP]             | set player group                                               |
| /yell [TEXT]                            | send a text to the server as yell message                      |
| /bc [TYPE] [TEXT]                       | send a broadcast message with identifier [TYPE] to all players |
| /unban [UID]                            | unban a player by his uid                                      |
| /teleporttoplayer [PLAYER_A] [PLAYER_B] | teleport Player A to Player B if both are online               |
| /mkadmin [PLAYER]                       | grant player admin status                                      |
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
        │    │    └── settings.properties
        │    ├── OZTools
        │    │    ├── assets
        │    │    │    └── ...
        │    │    ├── lib
        │    │    │    └── \*.jar
        │    │    ├── HISTORY.md
        │    │    ├── OZTools.jar
        │    │    ├── README.md
        │    │    └── settings.properties
        :    :
```

## Build (Maven)

TODO

### Installation after build

just copy `dist/OZDiscordConnect` folder after build into your plugin folder, thats it!

## Configuration

The settings.properties contains all you need to configure this plugin

| setting                           | default         | description                                                                                                        |
| --------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------ |
| **General plugin settings**       |                 |                                                                                                                    |
| logLevel                          | 0               | Logging to server console higher values means less output 0=all (debug)                                            |
| restartOnUpdate                   | false           | if true, restart flag is set automatically if plugin file has changed on the server                                |
| sendPluginWelcome                 | false           | -                                                                                                                  |
| **JavaCord settings**             |                 |                                                                                                                    |
| botEnable                         | false           | Enables usage of DiscordBot                                                                                        |
| botSecure                         | true            | Only Bot owner can use commands if `true`                                                                          |
| botToken                          |                 | the token for your bot                                                                                             |
| botLang                           | en              | -                                                                                                                  |
| botChatChannelName                | server-chat     | -                                                                                                                  |
| botAdmins                         |                 | -                                                                                                                  |
| **other plugin settings**         |                 |                                                                                                                    |
| allowRestart                      | false           | if true, normal players are allowed to use /ozrestart after they played `restartMinimumTime` seconds on the server |
| restartMinimumTime                | 86400           | player must play at least this time in seconds to use restart feature                                              |
| allowScreenshots                  | true            | -                                                                                                                  |
| joinDiscord                       |                 | the code to join discord (not the full url!)                                                                       |
| restartTimed                      | false           | enable or disable scheduled restart feature                                                                        |
| restartTimes                      | 00:00           | set restart times for scheduled restart use hh:mm to add multiple values split them with a pipe                    |
| forceRestartAfter                 | 0               | force restart after this time (minutes) (0 if you do not want to force restart)                                    |
| **Chat settings**                 |                 |                                                                                                                    |
| postChat                          | false           | if true, chat is posted to the webHook for Chat                                                                    |
| webHookChatUrl                    |                 | this is the webHook used for ingame chat                                                                           |
| overrideAvatar                    | true            | -                                                                                                                  |
| **Status settings**               |                 |                                                                                                                    |
| postStatus                        | false           | if true, server status messages like player login/out is send to the webHook for Status                            |
| useServerName                     | false           | if true, the servername is used as username for status posts                                                       |
| reportStatusEnabled               | true            | if true, a message will be posted when the plugin is enabled (server boot)                                         |
| reportStatusDisabled              | true            | if true, a message will be posted when the plugin is disabled (server shutdown)                                    |
| reportSettingsChanged             | true            | if true, a message will be posted if settings.properties has changed                                               |
| reportJarChanged                  | true            | if true, a message will be posted if the jar file has changed (plugin update for example)                          |
| statusEnabledMessage              |                 | the message that will be posted to discord on plugin enable                                                        |
| statusDisabledMessage             |                 | the message that will be posted to discord on plugin disable                                                       |
| statusUsername                    |                 | the fixed username to use for status messages                                                                      |
| webHookStatusUrl                  |                 | this is the webHook used for status messages                                                                       |
| **Support settings**              |                 |                                                                                                                    |
| postSupport                       | false           | -                                                                                                                  |
| supportScreenshot                 | true            | -                                                                                                                  |
| webHookSupportUrl                 |                 | this is the webHook used for support messages                                                                      |
| addTeleportCommand                | true            | if true, a teleport command is added to the support message                                                        |
| **Color & Chat settings**         |                 |                                                                                                                    |
| maxScreenWidth                    | 1920            | -                                                                                                                  |
| colorizeChat                      | true            | -                                                                                                                  |
| showGroup                         | false           | -                                                                                                                  |
| colorSupport                      | <color=#782d8e> | -                                                                                                                  |
| colorLocalSelf                    | <color=#ddffdd> | -                                                                                                                  |
| colorLocalAdmin                   | <color=#db3208> | -                                                                                                                  |
| colorLocalOther                   | <color=#dddddd> | -                                                                                                                  |
| colorLocalDiscord                 | <color=#ddddff> | -                                                                                                                  |
| **Badass/Event Tracker settings** |                 |                                                                                                                    |
| postTrackedEvents                 | false           | -                                                                                                                  |
| webHookEventUrl                   |                 | -                                                                                                                  |
| trackServerLogLevel               | 100             | -                                                                                                                  |
| trackMountKill                    | false           | -                                                                                                                  |
| trackNonHostileAnimalKill         | false           | -                                                                                                                  |
| trackPickupables                  | false           | -                                                                                                                  |
