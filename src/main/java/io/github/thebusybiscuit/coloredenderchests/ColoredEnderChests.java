package io.github.thebusybiscuit.coloredenderchests;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.updater.GitHubBuildsUpdater;
import net.guizhanss.minecraft.chineselib.minecraft.DyeColors;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ColoredEnderChests extends JavaPlugin implements SlimefunAddon {

    protected Config cfg;
    protected Map<Integer, String> colors = new HashMap<>();
    protected ItemGroup itemGroup;

    @Override
    public void onEnable() {
        cfg = new Config(this);

        // Setting up bStats
        new Metrics(this, 4907);

        // Setting up the Auto-Updater
        if (cfg.getBoolean("options.auto-update") && getDescription().getVersion().startsWith("DEV - ")) {
            new GitHubBuildsUpdater(this, getFile(), "ybw0014/ColoredEnderChests-CN/master").start();
        }

        Research enderChestsResearch = new Research(new NamespacedKey(this, "colored_enderchests"), 2610, "彩色末影箱", 20);
        Research bigEnderChestsResearch = new Research(new NamespacedKey(this, "big_colored_enderchests"), 2611, "大型彩色末影箱", 30);

        enderChestsResearch.register();
        bigEnderChestsResearch.register();

        colors.put(0, ChatColor.WHITE + DyeColors.WHITE.toString());
        colors.put(1, ChatColor.GOLD + DyeColors.ORANGE.toString());
        colors.put(2, ChatColor.LIGHT_PURPLE + DyeColors.MAGENTA.toString());
        colors.put(3, ChatColor.AQUA + DyeColors.LIGHT_BLUE.toString());
        colors.put(4, ChatColor.YELLOW + DyeColors.YELLOW.toString());
        colors.put(5, ChatColor.GREEN + DyeColors.LIME.toString());
        colors.put(6, ChatColor.LIGHT_PURPLE + DyeColors.PINK.toString());
        colors.put(7, ChatColor.DARK_GRAY + DyeColors.GRAY.toString());
        colors.put(8, ChatColor.GRAY + DyeColors.LIGHT_GRAY.toString());
        colors.put(9, ChatColor.DARK_AQUA + DyeColors.CYAN.toString());
        colors.put(10, ChatColor.DARK_PURPLE + DyeColors.PURPLE.toString());
        colors.put(11, ChatColor.BLUE + DyeColors.BLUE.toString());
        colors.put(12, ChatColor.GOLD + DyeColors.BROWN.toString());
        colors.put(13, ChatColor.DARK_GREEN + DyeColors.GREEN.toString());
        colors.put(14, ChatColor.RED + DyeColors.RED.toString());
        colors.put(15, ChatColor.BLACK + DyeColors.BLACK.toString());

        itemGroup = new ItemGroup(new NamespacedKey(this, "colored_enderchests"), new CustomItemStack(Material.ENDER_CHEST, "&5彩色末影箱"), 2);

        for (int c1 = 0; c1 < 16; c1++) {
            for (int c2 = 0; c2 < 16; c2++) {
                for (int c3 = 0; c3 < 16; c3++) {
                    registerEnderChest(enderChestsResearch, bigEnderChestsResearch, c1, c2, c3);
                }
            }
        }

    }

    private void registerEnderChest(Research smallResearch, Research bigResearch, final int c1, final int c2, final int c3) {
        if (cfg.getBoolean("small_chests")) {
            ColoredEnderChest item = new ColoredEnderChest(this, 27, c1, c2, c3);
            item.register(this);
            smallResearch.addItems(item);
        }

        if (cfg.getBoolean("big_chests")) {
            ColoredEnderChest item = new ColoredEnderChest(this, 54, c1, c2, c3);
            item.register(this);
            bigResearch.addItems(item);
        }
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/ybw0014/ColoredEnderChests-CN/issues";
    }
}
