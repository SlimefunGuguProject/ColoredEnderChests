package io.github.thebusybiscuit.coloredenderchests;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.DataUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class SharedInventories {
    private static final Map<String, CachedInventory> INVENTORIES = new ConcurrentHashMap<>();
    private static final Map<String, Set<Location>> PLACED = new ConcurrentHashMap<>();

    private SharedInventories() {}

    public static void initialize(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onInventoryClose(InventoryCloseEvent event) {
                if (event.getInventory().getHolder() instanceof SharedHolder holder) save(holder.cached);
            }
        }, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> INVENTORIES.values().forEach(SharedInventories::save), 200L, 200L);
    }

    public static void register(String sfId, Location loc) {
        PLACED.computeIfAbsent(sfId, ignored -> ConcurrentHashMap.newKeySet()).add(loc);
    }

    public static void unregister(String sfId, Location loc) {
        Set<Location> locations = PLACED.get(sfId);
        if (locations != null) locations.remove(loc);
    }

    public static void open(String sfId, Player player, int size, String title) {
        CachedInventory cached = INVENTORIES.computeIfAbsent(sfId, id -> new CachedInventory(id, size, title));
        synchronized (cached) {
            if (cached.inventory != null) {
                player.openInventory(cached.inventory);
                return;
            }
            cached.waitingPlayers.add(player);
        }
        ensureLoaded(cached);
    }

    public static void writeRecordToMenu(String sfId, BlockMenu menu, int size) {
        CachedInventory cached = INVENTORIES.computeIfAbsent(sfId, id -> new CachedInventory(id, size, "末影箱"));
        synchronized (cached) {
            if (cached.data == null) {
                cached.pendingMenuPushes.add(menu);
            } else {
                pushRecordToMenu(cached.data, menu, size);
            }
        }
        ensureLoaded(cached);
    }

    public static void onMenuSlotChanged(String sfId, DirtyChestMenu menu, int slot, ItemStack next) {
        CachedInventory cached = INVENTORIES.computeIfAbsent(sfId, id -> new CachedInventory(id, sizeOf(id), "末影箱"));
        synchronized (cached) {
            if (cached.data == null) {
                cached.pendingChanges.add(new PendingChange(menu, slot, next == null ? null : next.clone()));
            } else {
                applyMenuChange(cached, menu, slot, next);
            }
        }
        ensureLoaded(cached);
    }

    public static void flushAll() {
        INVENTORIES.values().forEach(SharedInventories::save);
    }

    private static void ensureLoaded(CachedInventory cached) {
        synchronized (cached) {
            if (cached.loading) return;
            if (cached.data != null) {
                // 记录已加载但 GUI 未创建（例如放置时已预建记录）：此时不会再有回调，就地服务等待者
                serveWaiters(cached);
                return;
            }
            cached.loading = true;
        }
        UUID uuid = Utils.generateFixedUUID(cached.sfId);
        var controller = Slimefun.getDatabaseManager().getBlockDataController();
        SlimefunUniversalData data = controller.getUniversalData(uuid);
        if (data == null) data = controller.createUniversalData(uuid, cached.sfId);
        SlimefunUniversalData record = data;
        StorageCacheUtils.executeAfterLoad(record, () -> {
            synchronized (cached) {
                cached.loading = false;
                cached.data = record;
                for (PendingChange change : cached.pendingChanges) {
                    applyMenuChange(cached, change.menu, change.slot, change.item);
                }
                cached.pendingChanges.clear();
                for (BlockMenu menu : cached.pendingMenuPushes) pushRecordToMenu(record, menu, cached.size);
                cached.pendingMenuPushes.clear();
                serveWaiters(cached);
            }
        }, true);
    }

    private static void serveWaiters(CachedInventory cached) {
        synchronized (cached) {
            if (!cached.waitingPlayers.isEmpty()) {
                loadInventory(cached);
                for (Player player : cached.waitingPlayers) {
                    if (player.isOnline()) player.openInventory(cached.inventory);
                }
                cached.waitingPlayers.clear();
            }
        }
    }

    private static void loadInventory(CachedInventory cached) {
        cached.contents = new ItemStack[cached.size];
        Map<String, String> allData = cached.data.getAllData();
        for (int slot = 0; slot < cached.size; slot++) {
            String encoded = allData.get("slot_" + slot);
            if (encoded != null) cached.contents[slot] = DataUtils.deserializeItemStack(encoded);
        }
        cached.inventory = Bukkit.createInventory(new SharedHolder(cached), cached.size, cached.title);
        cached.inventory.setContents(cached.contents);
        cached.contents = cached.inventory.getContents();
        for (int slot = 0; slot < cached.contents.length; slot++) {
            if (cached.contents[slot] != null) cached.contents[slot] = cached.contents[slot].clone();
        }
    }

    private static void pushRecordToMenu(SlimefunUniversalData record, BlockMenu menu, int size) {
        Map<String, String> allData = record.getAllData();
        for (int slot = 0; slot < size; slot++) {
            String encoded = allData.get("slot_" + slot);
            ItemStack item = encoded == null ? null : DataUtils.deserializeItemStack(encoded);
            menu.replaceExistingItem(slot, item);
        }
    }

    private static void save(CachedInventory cached) {
        synchronized (cached) {
            if (cached.inventory == null || cached.data == null) return;
            ItemStack[] contents = cached.inventory.getContents();
            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack item = contents[slot];
                if (item == null || item.isEmpty()) {
                    if (cached.contents[slot] != null) applyMenuChange(cached, null, slot, null);
                } else if (!item.equals(cached.contents[slot])) {
                    applyMenuChange(cached, null, slot, item);
                }
            }
        }
    }

    private static void applyMenuChange(CachedInventory cached, DirtyChestMenu source, int slot, ItemStack next) {
        String key = "slot_" + slot;
        String nextValue = next == null || next.isEmpty() ? null : DataUtils.serializeItemStack(next);
        String current = cached.data.getData(key);
        if (java.util.Objects.equals(current, nextValue)) return;
        if (nextValue != null) cached.data.setData(key, nextValue);
        else cached.data.removeData(key);
        ItemStack copy = next == null || next.isEmpty() ? null : next.clone();
        if (cached.contents != null) cached.contents[slot] = copy;
        if (cached.inventory != null) cached.inventory.setItem(slot, copy == null ? null : copy.clone());
        for (Location location : PLACED.getOrDefault(cached.sfId, Set.of())) {
            BlockMenu other = StorageCacheUtils.getMenu(location);
            if (other != null && other != source) {
                ItemStack existing = other.getItemInSlot(slot);
                if (!java.util.Objects.equals(existing, copy)) other.replaceExistingItem(slot, copy == null ? null : copy.clone());
            }
        }
    }

    private record PendingChange(DirtyChestMenu menu, int slot, ItemStack item) {}

    private static final class CachedInventory {
        private final String sfId;
        private final String title;
        private final int size;
        private final Set<Player> waitingPlayers = new HashSet<>();
        private final List<PendingChange> pendingChanges = new ArrayList<>();
        private final Set<BlockMenu> pendingMenuPushes = new HashSet<>();
        private ItemStack[] contents;
        private Inventory inventory;
        private SlimefunUniversalData data;
        private boolean loading;

        private CachedInventory(String sfId, int size, String title) {
            this.sfId = sfId;
            this.size = size;
            this.title = title;
        }
    }

    private static int sizeOf(String sfId) {
        return sfId.contains("_BIG_") ? 54 : 27;
    }

    private static final class SharedHolder implements InventoryHolder {
        private final CachedInventory cached;

        private SharedHolder(CachedInventory cached) { this.cached = cached; }

        @Override
        public Inventory getInventory() { return cached.inventory; }
    }
}
