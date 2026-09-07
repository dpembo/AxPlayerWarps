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
