package com.artillexstudios.axplayerwarps.listeners;

import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.hooks.rank.WarpLimitEnforcer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;

/**
 * Only registers if LuckPerms is actually installed - AxPlayerWarps must
 * keep working without it, same as every other soft-dependency integration.
 */
public class LuckPermsListener {

    public static void register() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) return;
        if (!CONFIG.getBoolean("warp-limit-enforcement.enabled", true)) return;

        LuckPerms luckPerms = LuckPermsProvider.get();
        luckPerms.getEventBus().subscribe(AxPlayerWarps.getInstance(), UserDataRecalculateEvent.class, event -> {
            Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
            if (player == null) return; // offline - caught by the join check instead

            // give Bukkit's permission attachments a tick to catch up with LuckPerms' own cache
            Bukkit.getScheduler().runTask(AxPlayerWarps.getInstance(), () -> WarpLimitEnforcer.enforce(player));
        });
    }
}
