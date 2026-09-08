# AxPlayerWarps

A Paper/Spigot plugin that lets players create and manage their own warp points, with categories, ratings, favorites, paid/whitelisted warps, and a fully async database backend (H2, MySQL, or PostgreSQL).

This is a **fork of [Artillex-Studios/AxPlayerWarps](https://github.com/Artillex-Studios/AxPlayerWarps)**, maintained for use on the [Globeworks](https://globeworks.uk) Minecraft network. It's built from upstream `1.17.1` with a handful of Globeworks-specific fixes and features layered on top (see below). It is **not** a drop-in replacement for the latest upstream release — upstream has since moved on to `1.17.2`/`1.17.3`, and this fork has not merged those changes in.

For the full feature set, configuration reference, and item-builder syntax, see [Artillex Studios' documentation](https://docs.artillex-studios.com/axplayerwarps.html). At a high level, the plugin gives players:

- Their own warp(s), gated by a permission-based (`axplayerwarps.warps.<amount>`) limit
- Categories, search, and multiple sort orders for browsing other players' warps
- Favoriting, a "recently visited" list, and 1–5 star ratings
- Public, private, and whitelisted/blacklisted access control per warp
- Optional pay-to-use warps with a per-warp bank, and multi-currency support via Vault/PlaceholderAPI-based economy hooks
- Folia support, with all database I/O run asynchronously

## Commands

The player and admin commands are two separate command trees, and both support multiple aliases so you can use whichever feels natural. Aliases are configured in `config.yml` and require a server restart to change.

### Player commands

Default aliases: `/axplayerwarps`, `/axpw`, `/pw`, `/playerwarps`, `/playerwarp`, `/pwarp`, `/pwarps`

| Command | What it does | Permission |
| --- | --- | --- |
| `/pwarp` | Opens the main warps menu (the categories GUI by default) | `axplayerwarps.open` |
| `/pwarp help` | Shows the plugin's help message | `axplayerwarps.help` |
| `/pwarp open [player]` | Opens the warps menu for yourself, or for another player if given | `axplayerwarps.open` (`axplayerwarps.open.other` to open it for someone else) |
| `/pwarp warp <name>` / `/pwarp go <name>` | Teleports you directly to a warp by name, skipping the GUI | `axplayerwarps.use` |
| `/pwarp create <name>` / `/pwarp set <name>` | Creates a new warp at your current location | `axplayerwarps.create` |
| `/pwarp delete <name>` | Deletes one of your own warps | `axplayerwarps.delete` |
| `/pwarp edit <name>` / `/pwarp settings <name>` | Opens the edit menu for one of your own warps (description, icon, access, price, etc.) | `axplayerwarps.edit` |
| `/pwarp info <name>` | Prints a warp's info (owner, rating, description, etc.) to chat | `axplayerwarps.info` |

Notes on the player commands:
- `delete` and `edit` only work on warps you own — trying to target someone else's warp returns a "not your warp" error rather than silently failing.
- Creating a warp is subject to your warp limit (see [Permissions](#permissions) below), any `disallowed-worlds` restriction, build permission checks from supported protection plugins (Towny, WorldGuard, GriefPrevention, etc.), and warp-name rules (length, no disallowed words, no spaces). If `warp-creation-cost` is enabled in `config.yml`, you'll also need to confirm and pay before the warp is created.
- Both `open` and `warp`/`go` route through the same unsafe-location and payment confirmation checks — if a warp is in an unloaded/unsafe spot, or is a paid warp you don't own, you'll be asked to confirm before teleporting (unless you hold a bypass permission).

### Admin commands

Default aliases: `/axplayerwarpsadmin`, `/axpwadmin`, `/pwadmin`, `/playerwarpsadmin`, `/playerwarpadmin`, `/pwarpadmin`, `/pwarpsadmin`

| Command | What it does | Permission |
| --- | --- | --- |
| `/pwadmin` / `/pwadmin help` | Shows the admin help message | `axplayerwarps.admin.help` |
| `/pwadmin reload` | Reloads `config.yml`, `lang.yml`, `currencies.yml`, `hooks.yml`, `input.yml`, and all of the GUI config files without a server restart | `axplayerwarps.admin.reload` |
| `/pwadmin delete <name>` | Force-deletes any warp by name, regardless of owner | `axplayerwarps.admin.delete` |
| `/pwadmin deleteid <id>` | Force-deletes a warp by its internal database ID (useful if a warp's name is broken/unparseable) | `axplayerwarps.admin.delete` |
| `/pwadmin setowner <name> <player>` | Transfers ownership of a warp to another player, and clears that player from the warp's whitelist/blacklist | `axplayerwarps.admin.setowner` |
| `/pwadmin converter <type>` | Runs a data converter to import warps from another plugin. Currently supports `PLAYER_WARPS` (the [PlayerWarps](https://github.com/Oribuin/PlayerWarps) plugin) | `axplayerwarps.admin.converter` |

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `axplayerwarps.open` | true | Open the main warps GUI (`/pwarp`) |
| `axplayerwarps.help` | *(undeclared — see note)* | Use `/pwarp help` |
| `axplayerwarps.open.other` | op | Open the warps GUI on another player's behalf |
| `axplayerwarps.use` | true | Teleport to a warp via `/pwarp warp`/`/pwarp go`, or via the GUI |
| `axplayerwarps.create` | true | Create a new warp |
| `axplayerwarps.delete` | true | Delete your own warp |
| `axplayerwarps.edit` | true | Edit your own warp's settings |
| `axplayerwarps.info` | true | View a warp's info |
| `axplayerwarps.warps.<amount>` | `axplayerwarps.warps.1` → true | Sets how many warps a player may own at once. Grant a rank e.g. `axplayerwarps.warps.5` for a 5-warp limit — the plugin takes the **highest** numbered node a player has, it isn't cumulative. Only `axplayerwarps.warps.1` is declared in `plugin.yml` by default; higher tiers (`.warps.3`, `.warps.10`, etc.) are meant to be granted through your permissions plugin as needed |
| `axplayerwarps.warps.*` (or server op) | — | Unlimited warps, bypassing the numbered limit entirely |
| `axplayerwarps.delay-bypass` | op | Skip the teleport delay/movement-cancels-warp queue when warping |
| `axplayerwarps.admin.bypass` | op | Skip unsafe-location and paid-warp confirmation checks when teleporting to any warp |
| `axplayerwarps.admin.help` | op | Use `/pwadmin` / `/pwadmin help` |
| `axplayerwarps.admin.reload` | op | Use `/pwadmin reload` |
| `axplayerwarps.admin.delete` | op | Use `/pwadmin delete` and `/pwadmin deleteid` |
| `axplayerwarps.admin.setowner` | op | Use `/pwadmin setowner` |
| `axplayerwarps.admin.converter` | op | Use `/pwadmin converter` |

A note on `axplayerwarps.help`: it's checked in code for the `help` subcommand but isn't declared in `plugin.yml`'s `permissions` block, unlike every other player permission. In practice this means most permission plugins will treat it as granted to everyone by default (Bukkit falls back to allowing undeclared permissions), but if your permissions setup denies-by-default for anything not explicitly listed, you'll want to grant `axplayerwarps.help` manually.

If you're using LuckPerms (recommended, and now a recognised soft-dependency in this fork), warp-limit permissions are read live from LuckPerms whenever a player's data recalculates — e.g. immediately after `/lp user <player> parent set <rank>` — rather than waiting for their next relog. See [Warp limit enforcement on rank change](#whats-different-from-upstream) below for what happens if that change lowers a player below their current warp count.

## What's different from upstream

Everything below is specific to this fork and is not present in the upstream `1.17.1` codebase it was branched from:

- **Warp limit enforcement on rank change.** If a player's warp limit shrinks — most commonly because a LuckPerms rank change removed an `axplayerwarps.warps.<n>` permission — the plugin now automatically deletes their newest warps until they're back within their new limit. Their oldest warps are always kept, and the player is notified which warps were removed. This also runs on join, to catch demotions that happened while a player was offline (e.g. via a web panel). It's fully configurable and can be disabled in `config.yml`:
  ```yaml
  warp-limit-enforcement:
    enabled: true
    check-on-join: true
  ```
- **More reliable warp-limit calculation.** `getWarpLimit()` previously only scanned Bukkit's effective permissions, which can be stale until a player relogs after a permission change. When LuckPerms is installed, the fork now resolves permission nodes directly through the LuckPerms API instead, giving an accurate limit immediately after a rank change. An optional debug-logging flag was added alongside this to make future permission issues easier to diagnose.
- **Fixed a duplicate-message bug on warp deletion.** The "warp deleted" confirmation message and the in-memory removal of the warp were previously done inside the async database-deletion callback. This could result in duplicate messages under concurrent deletion (e.g. warp-limit enforcement running at the same time as a player deleting a warp themselves). Both now happen synchronously before the async database call is fired.
- **LuckPerms is now a recognised soft dependency**, added to `plugin.yml`, and a `LuckPermsListener` was added that only registers itself if LuckPerms is actually installed — the plugin continues to work identically without it.
- **Rebranded GUI titles.** GUI headers across the plugin were changed from "AxPWarps" to "Player Warps" for consistency with how the plugin is referred to on the Globeworks network.
- **Trimmed unused third-party integrations.** Several `system`-scope dependencies pointing at local `libs/*.jar` files for economy plugins that aren't used on Globeworks (MobCoins, KingdomsX, CoinsEngine, ExcellentEconomy, RoyaleEconomy, UltraEconomyAPI, BeastTokens, EcoBits) were commented out in `pom.xml`, so the project builds cleanly from a fresh clone without needing those jars supplied locally.
- **Automated release pipeline.** A GitHub Actions workflow (`.github/workflows/release.yml`) builds the plugin on every push to `main` and publishes the shaded jar as a GitHub Release, tagging clean version bumps and timestamped snapshots separately.

## Building

```
mvn clean package
```

The shaded jar is produced under `target/`. See `.github/workflows/release.yml` for how CI builds and tags releases.

## License

This project is licensed under the **MIT License**, the same license as the upstream project it was forked from. The original copyright notice and permission notice are preserved unmodified in [`LICENSE`](./LICENSE):

> Copyright (c) 2024 BenceX100

In line with the MIT License's requirements, that notice must be kept in any copy or substantial portion of this software — it has not been altered or removed here, and shouldn't be if you redistribute this fork either. All of the modifications described above are additional changes made on top of the original MIT-licensed code and are provided under the same license.

- **Upstream project:** [Artillex-Studios/AxPlayerWarps](https://github.com/Artillex-Studios/AxPlayerWarps)
- **Upstream docs:** https://docs.artillex-studios.com/axplayerwarps.html
- **Upstream support:** https://dc.artillex-studios.com/
