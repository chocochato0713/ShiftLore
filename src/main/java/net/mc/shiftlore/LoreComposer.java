package net.mc.shiftlore;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class LoreComposer {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private LoreComposer() {
    }

    public static @Nullable ItemStack forDisplay(ItemStack original, LoreProfile profile, boolean expanded) {
        if (original == null || original.getType().isAir() || profile == null) {
            return original;
        }
        ItemStack clone = original.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return clone;
        }
        List<String> raw = profile.linesFor(expanded);
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>(raw.size());
        for (String line : raw) {
            lore.add(MINI.deserialize(line));
        }
        meta.lore(lore);
        clone.setItemMeta(meta);
        return clone;
    }
}
