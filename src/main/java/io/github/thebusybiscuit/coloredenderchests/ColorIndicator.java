package io.github.thebusybiscuit.coloredenderchests;

import io.github.thebusybiscuit.slimefun4.utils.ColoredMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@SuppressWarnings("deprecation")
final class ColorIndicator {

    // exact per-chest ownership of display entities; avoids touching a neighbor chest's indicators
    private static final NamespacedKey OWNER = new NamespacedKey(
            Bukkit.getPluginManager().getPlugin("ColoredEnderChests"), "indicator_block");
    // original armor-stand look: main-hand item rendered as a small "dropped item" resting on the chest lid
    private static final float SCALE = 0.2f;
    private static final double OFFSET = -0.08;

    private ColorIndicator() {}

    static void updateIndicator(Block b, int c1, int c2, int c3, int yaw) {
        removeIndicator(b);
        long owner = ownerKey(b);
        Location l = b.getLocation().add(0.5D, 0.5D + OFFSET, 0.5D);
        translocate(l, -1.0, yaw);
        createDisplay(owner, l, new ItemStack(getWool(c1)));
        createDisplay(owner, translocate(l, 1, yaw), new ItemStack(getWool(c2)));
        createDisplay(owner, translocate(l, 1, yaw), new ItemStack(getWool(c3)));
    }

    private static long ownerKey(Block b) {
        return ((b.getLocation().getBlockX() & 0x3FFFFFFL) << 38)
                | ((b.getLocation().getBlockZ() & 0x3FFFFFFL) << 12)
                | (b.getLocation().getBlockY() & 0xFFFL);
    }

    static void removeIndicator(Block b) {
        long owner = ownerKey(b);
        var center = b.getLocation().add(0.5D, 0.5D, 0.5D);
        for (Entity n : b.getChunk().getEntities()) {
            boolean remove = false;
            if (n instanceof ItemDisplay d) {
                remove = isOwnedBy(d, owner)
                    // unt-tagged displays from pre-PDC versions of this plugin
                    || d.getCustomName() == null && center.distanceSquared(d.getLocation()) < 0.25D && isOurWool(d);
            } else if (n instanceof ArmorStand as) {
                remove = isLegacyMarker(as, center);
            }
            if (remove) n.remove();
        }
    }

    private static boolean isOwnedBy(ItemDisplay d, long owner) {
        var pdc = d.getPersistentDataContainer();
        return pdc.has(OWNER, PersistentDataType.LONG) && pdc.get(OWNER, PersistentDataType.LONG) == owner;
    }

    private static boolean isOurWool(ItemDisplay d) {
        var item = d.getItemStack();
        return item != null && item.getType().name().endsWith("_WOOL");
    }

    /**
     * pre-display armor stands created by this plugin: small, arms, no base plate, holding wool.
     * legacy stands render their item to the stand's side (hand offset), so the entity can sit up
     * to ~1 block off-center; limit wool-in-hand matching to a 1.5-block radius.
     */
    private static boolean isLegacyMarker(ArmorStand as, Location center) {
        return as.isSmall() && as.hasArms() && !as.hasBasePlate()
            && as.getCustomName() == null
            && center.distanceSquared(as.getLocation()) < 2.25D
            && as.getEquipment().getItemInMainHand().getType().name().endsWith("WOOL");
    }

    private static void createDisplay(long owner, Location l, ItemStack item) {
        l.getWorld().spawn(l, ItemDisplay.class, display -> {
            display.setItemStack(item);
            display.getPersistentDataContainer().set(OWNER, PersistentDataType.LONG, owner);
            // display anchors render at the item center; legacy armor-stand hand sat ~0.42 above its feet anchor
            display.setTransformation(new Transformation(new Vector3f(0, 0.42f, 0), new Quaternionf(), new Vector3f(SCALE, SCALE, SCALE), new Quaternionf()));
            display.setBillboard(Display.Billboard.FIXED);
        });
    }

    private static Material getWool(int index) {
        return ColoredMaterial.WOOL.get(index);
    }

    private static Location translocate(Location l, double direction, int yaw) {
        return switch (yaw) {
            // 0
            case 45 -> l.add(0.275 * direction, 0, 0);
            // 180
            case 225 -> l.add(-0.275 * direction, 0, 0);
            // -90
            case -45 -> l.add(0, 0, -0.275 * direction);
            // 90
            default -> l.add(0, 0, 0.275 * direction);
        };
    }

}
