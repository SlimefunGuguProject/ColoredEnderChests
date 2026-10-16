package io.github.thebusybiscuit.coloredenderchests;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.utils.ColoredMaterial;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.EnderChest;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ColoredEnderChest extends SlimefunItem {

    public ColoredEnderChest(ColoredEnderChests plugin, int size, int c1, int c2, int c3) {
        // @formatter:off
        super(plugin.itemGroup, new SlimefunItemStack("COLORED_ENDER_CHEST_" + (size == 27 ? "SMALL" : "BIG") + "_" + c1 + "_" + c2 + "_" + c3, Material.ENDER_CHEST, "&e彩色末影箱 &7(" + (size == 27 ? "小" : "大") + ")", "", "&7大小: &e" + size, "", "&7- " + plugin.colors.get(c1), "&7- " + plugin.colors.get(c2), "&7- "
                + plugin.colors.get(c3)), RecipeType.ANCIENT_ALTAR,
                (size == 27) ? new ItemStack[] {
                        new ItemStack(ColoredMaterial.WOOL.get(c1)), new ItemStack(ColoredMaterial.WOOL.get(c2)), new ItemStack(ColoredMaterial.WOOL.get(c3)),
                        new ItemStack(Material.OBSIDIAN), new ItemStack(Material.CHEST), new ItemStack(Material.OBSIDIAN),
                        SlimefunItems.ENDER_RUNE, new ItemStack(Material.OBSIDIAN), SlimefunItems.ENDER_RUNE }
                : new ItemStack[] {
                        new ItemStack(ColoredMaterial.WOOL.get(c1)), new ItemStack(ColoredMaterial.WOOL.get(c2)), new ItemStack(ColoredMaterial.WOOL.get(c3)),
                        SlimefunItems.WITHER_PROOF_OBSIDIAN, getSmallerEnderChest(c1, c2, c3), SlimefunItems.WITHER_PROOF_OBSIDIAN,
                        SlimefunItems.ENDER_RUNE, SlimefunItems.GOLD_24K, SlimefunItems.ENDER_RUNE
                }
        );
        // @formatter:on

        new SharedMenuPreset(getId(), size);

        addItemHandler(onBlockBreak());
        addItemHandler(onBlockPlace(c1, c2, c3));
        addItemHandler((BlockUseHandler) event -> {
            var clickedBlock = event.getClickedBlock();
            if (clickedBlock.isEmpty()) {
                return;
            }
            var block = clickedBlock.get();

            int yaw;
            try {
                yaw = yaw(((EnderChest) block.getBlockData()).getFacing());
            } catch (IllegalArgumentException | ClassCastException ex) {
                yaw = 0;
            }
            ColorIndicator.updateIndicator(block, c1, c2, c3, yaw + 45);
            // reproduce core openInventory guard: sneaking with a non-empty hand is reserved for placing against the block
            if (!event.getPlayer().isSneaking() || event.getItem().getType() == Material.AIR) {
                SharedInventories.open(getId(), event.getPlayer(), size, "末影箱");
                event.setUseBlock(Event.Result.DENY);
            }
        });
    }

    private BlockBreakHandler onBlockBreak() {
        return new SimpleBlockBreakHandler() {
            @Override
            public void onBlockBreak(org.bukkit.block.Block b) {
                ColorIndicator.removeIndicator(b);
                SharedInventories.unregister(getId(), b.getLocation());
            }
        };
    }

    private BlockPlaceHandler onBlockPlace(int c1, int c2, int c3) {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                int yaw = yaw(((EnderChest) e.getBlock().getBlockData()).getFacing());
                ColorIndicator.updateIndicator(e.getBlock(), c1, c2, c3, yaw + 45);
                SharedInventories.register(getId(), e.getBlock().getLocation());
            }
        };
    }

    private static int yaw(BlockFace facing) {
        return switch (facing) {
            case NORTH -> 180;
            case SOUTH -> 0;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
    }

    private static ItemStack getSmallerEnderChest(int c1, int c2, int c3) {
        SlimefunItem enderChest = SlimefunItem.getById("COLORED_ENDER_CHEST_SMALL_" + c1 + "_" + c2 + "_" + c3);
        return enderChest != null ? enderChest.getItem() : null;
    }

    private static final class SharedMenuPreset extends BlockMenuPreset {
        private final int size;
        private final String sfId;

        private SharedMenuPreset(String id, int size) {
            super(id, "&e末影箱");
            this.sfId = id;
            this.size = size;
        }

        @Override
        public void init() {
            setSize(size);
        }

        @Override
        public boolean canOpen(Block block, Player player) {
            return true;
        }

        @Override
        public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
            int[] slots = new int[size];
            for (int i = 0; i < size; i++) slots[i] = i;
            return slots;
        }

        @Override
        public void newInstance(BlockMenu menu, Block block) {
            SharedInventories.writeRecordToMenu(sfId, menu, size);
        }

        @Override
        protected ItemStack onItemStackChange(DirtyChestMenu menu, int slot, ItemStack previous, ItemStack next) {
            SharedInventories.onMenuSlotChanged(sfId, menu, slot, next);
            return next;
        }
    }
}
