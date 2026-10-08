# LastLineControl

PaperMC plugin for Minecraft 1.21+ that provides fly and god mode with persistent state and fine-grained action restrictions.

## Features

- `/fly` and `/god` — toggle per-player with state saved across restarts
- Restrictions in fly/god — block break, PVP, item pickup, projectiles, vehicles, containers and more — all configurable
- Bypass permissions — grant specific players the ability to ignore certain restrictions
- Admin commands — toggle fly/god for any online player
- Update checker — automatically notifies admins about new versions via Modrinth

## Commands

| Command | Description | Permission |
|---|---|---|
| `/fly` | Toggle fly for yourself | `lastline.fly` |
| `/fly <nick>` | Toggle fly for another player | `lastline.admin` |
| `/god` | Toggle god mode for yourself | `lastline.god` |
| `/god <nick>` | Toggle god mode for another player | `lastline.admin` |
| `/llc reload` | Reload config | `lastline.admin` |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `lastline.admin` | Full admin access | op |
| `lastline.fly` | Use /fly for yourself | false |
| `lastline.god` | Use /god for yourself | false |
| `lastline.bypass.<action>` | Bypass a specific restriction | false |

Available bypass actions: `pickup`, `block-break`, `block-place`, `drop-item`, `pvp`, `hit-mob`, `chest`, `containers`, `interact-bow`, `interact-crystal`, `interact-flint`, `interact-enderchest`, `interact-shulker`, `interact-entities`, `projectiles`, `vehicles`, `spawn-eggs`

## Installation

1. Drop `lastlinecontrol-x.x.x.jar` into your server's `plugins/` folder
2. Restart the server
3. Edit `plugins/LastLineControl/config.yml` if needed
4. `/llc reload` to apply changes without restarting

## Building from source

```bash
git clone https://github.com/KreziBro/lastlinecontrol
cd lastlinecontrol
mvn package
```

The compiled jar will be in `target/`.

## Stack

- [PaperMC](https://papermc.io/) - server API
- Java 21
- Maven
