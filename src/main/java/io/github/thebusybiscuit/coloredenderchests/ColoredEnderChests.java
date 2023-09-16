package io.github.thebusybiscuit.coloredenderchests;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import net.guizhanss.guizhanlib.minecraft.helper.DyeColorHelper;
import net.guizhanss.guizhanlibplugin.updater.GuizhanUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ColoredEnderChests extends JavaPlugin implements SlimefunAddon {

    protected Config cfg;
    protected Map<Integer, String> colors = new HashMap<>();
    protected ItemGroup itemGroup;

    @Override
    public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("GuizhanLibPlugin")) {
            getLogger().log(Level.SEVERE, "本插件需要 鬼斩前置库插件(GuizhanLibPlugin) 才能运行!");
            getLogger().log(Level.SEVERE, "从此处下载: https://50L.cc/gzlib");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        cfg = new Config(this);

        // Setting up bStats
        new Metrics(this, 4907);

        // Setting up the Auto-Updater
        if (cfg.getBoolean("options.auto-update") && getDescription().getVersion().startsWith("Build")) {
            GuizhanUpdater.start(this, getFile(), "SlimefunGuguProject", "ColoredEnderChests", "master");
        }

        Research enderChestsResearch = new Research(new NamespacedKey(this, "colored_enderchests"), 2610, "彩色末影箱", 20);
        Research bigEnderChestsResearch = new Research(new NamespacedKey(this, "big_colored_enderchests"), 2611, "大型彩色末影箱", 30);

        enderChestsResearch.register();
        bigEnderChestsResearch.register();

        colors.put(0, ChatColor.WHITE + DyeColorHelper.getName(DyeColor.WHITE));
        colors.put(1, ChatColor.GOLD + DyeColorHelper.getName(DyeColor.ORANGE));
        colors.put(2, ChatColor.LIGHT_PURPLE + DyeColorHelper.getName(DyeColor.MAGENTA));
        colors.put(3, ChatColor.AQUA + DyeColorHelper.getName(DyeColor.LIGHT_BLUE));
        colors.put(4, ChatColor.YELLOW + DyeColorHelper.getName(DyeColor.YELLOW));
        colors.put(5, ChatColor.GREEN + DyeColorHelper.getName(DyeColor.LIME));
        colors.put(6, ChatColor.LIGHT_PURPLE + DyeColorHelper.getName(DyeColor.PINK));
        colors.put(7, ChatColor.DARK_GRAY + DyeColorHelper.getName(DyeColor.GRAY));
        colors.put(8, ChatColor.GRAY + DyeColorHelper.getName(DyeColor.LIGHT_GRAY));
        colors.put(9, ChatColor.DARK_AQUA + DyeColorHelper.getName(DyeColor.CYAN));
        colors.put(10, ChatColor.DARK_PURPLE + DyeColorHelper.getName(DyeColor.PURPLE));
        colors.put(11, ChatColor.BLUE + DyeColorHelper.getName(DyeColor.BLUE));
        colors.put(12, ChatColor.GOLD + DyeColorHelper.getName(DyeColor.BROWN));
        colors.put(13, ChatColor.DARK_GREEN + DyeColorHelper.getName(DyeColor.GREEN));
        colors.put(14, ChatColor.RED + DyeColorHelper.getName(DyeColor.RED));
        colors.put(15, ChatColor.BLACK + DyeColorHelper.getName(DyeColor.BLACK));

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
        return "https://github.com/SlimefunGuguProject/ColoredEnderChests/issues";
    }
}
