package net.mc.shiftlore;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Optional CraftEngine integration via reflection (softdepend).
 */
public final class CraftEngineBridge {

    private final Logger logger;
    private final Method byItemStack;
    private final Method definitionId;
    private final boolean available;

    public CraftEngineBridge(Logger logger) {
        this.logger = logger;
        Method byStack = null;
        Method idMethod = null;
        boolean ok = false;
        try {
            Class<?> itemsClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            byStack = itemsClass.getMethod("byItemStack", ItemStack.class);
            Class<?> defClass = Class.forName("net.momirealms.craftengine.bukkit.item.BukkitItemDefinition");
            idMethod = defClass.getMethod("id");
            ok = true;
        } catch (ReflectiveOperationException e) {
            logger.info("[ShiftLore] CraftEngine API 未找到，将仅使用配置里的 match 规则（若未配置则无法匹配 CE 物品）。");
        }
        this.byItemStack = byStack;
        this.definitionId = idMethod;
        this.available = ok;
    }

    public boolean isAvailable() {
        return available;
    }

    public @Nullable String resolveItemId(ItemStack stack) {
        if (!available || stack == null || stack.getType().isAir()) {
            return null;
        }
        try {
            Object definition = byItemStack.invoke(null, stack);
            if (definition == null) {
                return null;
            }
            Object key = definitionId.invoke(definition);
            if (key == null) {
                return null;
            }
            return normalizeKey(keyAsString(key));
        } catch (ReflectiveOperationException e) {
            logger.fine("[ShiftLore] CE lookup failed: " + e.getMessage());
            return null;
        }
    }

    private static String keyAsString(Object key) {
        try {
            Method asString = key.getClass().getMethod("asString");
            Object v = asString.invoke(key);
            if (v != null) {
                return v.toString();
            }
        } catch (ReflectiveOperationException ignored) {
            // fall through
        }
        return key.toString();
    }

    static String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String s = raw.trim();
        if (s.contains(":")) {
            return s;
        }
        return "minecraft:" + s;
    }
}
