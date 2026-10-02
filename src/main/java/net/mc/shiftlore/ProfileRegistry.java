package net.mc.shiftlore;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public final class ProfileRegistry {

    private final JavaPlugin plugin;
    private final CraftEngineBridge craftEngine;
    private Map<String, LoreProfile> byId = Map.of();

    public ProfileRegistry(JavaPlugin plugin, CraftEngineBridge craftEngine) {
        this.plugin = plugin;
        this.craftEngine = craftEngine;
    }

    public void reload() {
        Map<String, LoreProfile> loaded = new HashMap<>();
        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists() && !itemsDir.mkdirs()) {
            plugin.getLogger().warning("无法创建 items 配置目录");
        }
        saveDefaultItemFiles(itemsDir);

        File[] files = itemsDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files != null) {
            for (File file : files) {
                loadFile(file, loaded);
            }
        }

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("profiles");
        if (root != null) {
            loadProfilesSection(root, loaded, "config.yml");
        }

        byId = Collections.unmodifiableMap(loaded);
        plugin.getLogger().info("已加载 ShiftLore 配置 " + byId.size() + " 条物品说明。");
    }

    private void saveDefaultItemFiles(File itemsDir) {
        String[] defaults = {"rpg_ores.yml"};
        for (String name : defaults) {
            File target = new File(itemsDir, name);
            if (!target.exists()) {
                plugin.saveResource("items/" + name, false);
            }
        }
    }

    private void loadFile(File file, Map<String, LoreProfile> loaded) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection profiles = yaml.getConfigurationSection("profiles");
        if (profiles == null) {
            return;
        }
        loadProfilesSection(profiles, loaded, file.getName());
    }

    private void loadProfilesSection(ConfigurationSection profiles, Map<String, LoreProfile> loaded, String source) {
        for (String key : profiles.getKeys(false)) {
            ConfigurationSection sec = profiles.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            List<String> shortLines = sec.getStringList("short");
            List<String> detail = sec.getStringList("detail");
            if (shortLines.isEmpty()) {
                plugin.getLogger().warning("[" + source + "] profiles." + key + " 缺少 short");
                continue;
            }
            String id = normalizeProfileKey(key);
            loaded.put(id, new LoreProfile(id, List.copyOf(shortLines), List.copyOf(detail)));
            String alt = altKey(id);
            if (!loaded.containsKey(alt)) {
                loaded.put(alt, loaded.get(id));
            }
        }
    }

    public LoreProfile find(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        String ceId = craftEngine.resolveItemId(stack);
        if (ceId != null) {
            LoreProfile p = byId.get(normalizeProfileKey(ceId));
            if (p != null) {
                return p;
            }
            p = byId.get(altKey(normalizeProfileKey(ceId)));
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    public Map<String, LoreProfile> all() {
        return byId;
    }

    static String normalizeProfileKey(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }

    /** rpg:frost_crystal <-> frost_crystal when namespace is rpg */
    static String altKey(String normalized) {
        int idx = normalized.indexOf(':');
        if (idx > 0) {
            return normalized.substring(idx + 1);
        }
        return "rpg:" + normalized;
    }
}
