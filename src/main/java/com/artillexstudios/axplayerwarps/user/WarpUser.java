package com.artillexstudios.axplayerwarps.user;

import com.artillexstudios.axguiframework.GuiFrame;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.category.Category;
import com.artillexstudios.axplayerwarps.category.CategoryManager;
import com.artillexstudios.axplayerwarps.sorting.Sort;
import com.artillexstudios.axplayerwarps.sorting.SortingManager;
import com.artillexstudios.axplayerwarps.warps.Warp;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WarpUser {
    public static boolean DEBUG = false;

    private final Player player;
    private int sortingIdx = 0;
    private int categoryIdx = -1;
    private final CircularFifoQueue<GuiFrame<?>> lastGuis = new CircularFifoQueue<>(5);
    private List<Warp> favorites = Collections.synchronizedList(new ArrayList<>());

    public WarpUser(Player player) {
        this.player = player;

        AxPlayerWarps.getThreadedQueue().submit(() -> {
            AxPlayerWarps.getDatabase().loadOrUpdate(player);
            favorites = Collections.synchronizedList(AxPlayerWarps.getDatabase().getFavoriteWarps(player));
        });
    }

    public Player getPlayer() {
        return player;
    }

    public void resetSorting() {
        sortingIdx = 0;
    }

    public void changeSorting(int am) {
        sortingIdx += am;
    }

    public Sort getSorting() {
        int a = sortingIdx;
        int b = SortingManager.getEnabledSorting().size();
        return SortingManager.getEnabledSorting().get((a % b + b) % b);
    }

    public void resetCategory() {
        categoryIdx = -1;
    }

    public void changeCategory(int am) {
        categoryIdx += am;
    }

    public Category getCategory() {
        int a = categoryIdx;
        int b = CategoryManager.getCategories().size();
        return CategoryManager.getCategories().values().stream().toList().get((a % b + b) % b);
    }

    public CircularFifoQueue<GuiFrame<?>> getLastGuis() {
        return lastGuis;
    }

    public void addGui(GuiFrame<?> guiFrame) {
        lastGuis.add(guiFrame);
    }

    public List<Warp> getFavorites() {
        return favorites;
    }

    public int getWarpLimit() {
        boolean bypass = hasBypass(player);
        if (DEBUG) Bukkit.getLogger().info("[AxPlayerWarps] [debug] getWarpLimit() for " + player.getName()
                + " - op=" + player.isOp()
                + " hasStar=" + player.hasPermission("*")
                + " hasWarpsStar=" + player.hasPermission("axplayerwarps.warps.*")
                + " bypass=" + bypass);
        if (bypass) return Integer.MAX_VALUE;

        int am = 0;

        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            LuckPerms lp = LuckPermsProvider.get();
            User lpUser = lp.getUserManager().getUser(player.getUniqueId());
            if (lpUser != null) {
                QueryOptions opts = lp.getContextManager().getQueryOptions(player);
                if (DEBUG) Bukkit.getLogger().info("[AxPlayerWarps] [debug] scanning LP resolved nodes for axplayerwarps.warps.*");
                for (Node node : lpUser.resolveInheritedNodes(opts)) {
                    if (!node.getValue()) continue;
                    String key = node.getKey();
                    if (!key.startsWith("axplayerwarps.warps.")) continue;
                    if (DEBUG) Bukkit.getLogger().info("[AxPlayerWarps] [debug]   node=" + key + " value=true");
                    String suffix = key.substring("axplayerwarps.warps.".length());
                    try {
                        int value = Integer.parseInt(suffix);
                        if (value > am) am = value;
                    } catch (NumberFormatException ignored) {
                        if (DEBUG) Bukkit.getLogger().info("[AxPlayerWarps] [debug]   skipping non-numeric node: " + key);
                    }
                }
            }
        } else {
            boolean hasWarps1 = player.hasPermission("axplayerwarps.warps.1");
            am = hasWarps1 ? 1 : 0;
            if (DEBUG) Bukkit.getLogger().info("[AxPlayerWarps] [debug] hasPermission(axplayerwarps.warps.1)=" + hasWarps1 + ", starting am=" + am);

            if (DEBUG) Bukkit.getLogger().info("[AxPlayerWarps] [debug] scanning effective permissions matching axplayerwarps.warps.*");
            for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
                if (!pai.getPermission().startsWith("axplayerwarps.warps.")) continue;

                if (DEBUG) Bukkit.getLogger().info("[AxPlayerWarps] [debug]   node=" + pai.getPermission()
                        + " value=" + pai.getValue()
                        + " attachment=" + (pai.getAttachment() != null ? pai.getAttachment().getPlugin().getName() : "null"));

                if (!pai.getValue()) continue;

                try {
                    int value = Integer.parseInt(pai.getPermission().substring(pai.getPermission().lastIndexOf('.') + 1));
                    if (value > am) am = value;
                } catch (NumberFormatException e) {
                    if (DEBUG) Bukkit.getLogger().info("[AxPlayerWarps] [debug]   skipping non-numeric node: " + pai.getPermission());
                }
            }
        }

        if (DEBUG) Bukkit.getLogger().info("[AxPlayerWarps] [debug] final computed limit for " + player.getName() + " = " + am);
        return am;
    }

    private static boolean hasBypass(OfflinePlayer offlinePlayer) {
        if (offlinePlayer.isOp()) return true;
        final Player player = offlinePlayer.getPlayer();
        if (player == null) return false;
        if (player.hasPermission("*")) return true;
        if (player.hasPermission("axplayerwarps.warps.*")) return true;
        return false;
    }
}
