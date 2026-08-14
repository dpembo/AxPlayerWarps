package com.artillexstudios.axplayerwarps.hooks.rank;

import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

/**
 * Ensures a player never owns more warps than their current permissions allow.
 * Called after any event that could shrink a player's warp limit (LuckPerms
 * rank change, or on join to catch changes made while offline). If they own
 * more warps than they're entitled to, the most recently created ones are
 * removed until they're back within their limit; oldest warps are kept.
 */
public class WarpLimitEnforcer {

    public static void enforce(Player player) {
        if (!CONFIG.getBoolean("warp-limit-enforcement.enabled", true)) {
            Bukkit.getLogger().info("[AxPlayerWarps] [debug] enforcement disabled in config, skipping " + player.getName());
            return;
        }

        WarpUser user = Users.get(player);
        int limit = user.getWarpLimit();

        // newest first
        List<Warp> owned = WarpManager.getWarps().stream()
                .filter(warp -> warp.getOwner().equals(player.getUniqueId()))
                .sorted(Comparator.comparingLong(Warp::getCreated).reversed())
                .toList();

        Bukkit.getLogger().info("[AxPlayerWarps] [debug] " + player.getName() + " owns " + owned.size() + " warp(s), computed limit=" + limit);

        if (owned.size() <= limit) {
            Bukkit.getLogger().info("[AxPlayerWarps] [debug] " + player.getName() + " is within limit, nothing to do");
            return;
        }

        int excess = owned.size() - limit;
        List<Warp> toRemove = owned.subList(0, excess);
        String names = toRemove.stream().map(Warp::getName).collect(Collectors.joining(", "));

        Bukkit.getLogger().info("[AxPlayerWarps] [debug] " + player.getName() + " is over limit by " + excess + ", removing: " + names);

        MESSAGEUTILS.sendLang(player, "limit-enforcement.warps-removed", Map.of(
                "%limit%", "" + limit,
                "%removed%", "" + excess,
                "%warps%", names
        ));

        // copy before iterating - Warp#delete() mutates WarpManager's backing list
        for (Warp warp : List.copyOf(toRemove)) {
            warp.delete();
        }
    }
}

