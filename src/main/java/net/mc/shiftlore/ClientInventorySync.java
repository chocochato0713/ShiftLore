package net.mc.shiftlore;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Sends container contents with per-player lore without mutating server items.
 */
public final class ClientInventorySync {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final ProfileRegistry registry;
    private final ProtocolManager protocol;
    private final boolean protocolAvailable;

    public ClientInventorySync(JavaPlugin plugin, ProfileRegistry registry) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.registry = registry;
        ProtocolManager mgr = null;
        boolean ok = false;
        try {
            if (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
                mgr = ProtocolLibrary.getProtocolManager();
                ok = true;
            }
        } catch (Throwable t) {
            logger.warning("ProtocolLib 初始化失败: " + t.getMessage());
        }
        this.protocol = mgr;
        this.protocolAvailable = ok;
    }

    public boolean isProtocolAvailable() {
        return protocolAvailable;
    }

    public void refreshLater(Player player) {
        int delay = Math.max(0, plugin.getConfig().getInt("settings.refresh-delay-ticks", 1));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> refresh(player), delay);
    }

    public void refresh(Player player) {
        if (!protocolAvailable || player == null || !player.isOnline()) {
            return;
        }
        InventoryView view = player.getOpenInventory();
        boolean expand = player.isSneaking();
        int windowId = containerWindowId(player);
        int stateId = containerStateId(player);
        int slotCount = view.countSlots();
        List<ItemStack> items = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            ItemStack server = view.getItem(i);
            items.add(decorate(server, expand));
        }

        try {
            PacketContainer packet = protocol.createPacket(PacketType.Play.Server.WINDOW_ITEMS);
            packet.getIntegers().write(0, windowId);
            packet.getIntegers().write(1, stateId);
            packet.getItemListModifier().write(0, items);
            protocol.sendServerPacket(player, packet);
        } catch (Exception e) {
            logger.fine("WINDOW_ITEMS 失败，尝试逐格 SET_SLOT: " + e.getMessage());
            sendSlotBySlot(player, windowId, stateId, items);
        }
    }

    private void sendSlotBySlot(Player player, int windowId, int stateId, List<ItemStack> items) {
        for (int slot = 0; slot < items.size(); slot++) {
            try {
                PacketContainer packet = protocol.createPacket(PacketType.Play.Server.SET_SLOT);
                packet.getIntegers().write(0, windowId);
                packet.getIntegers().write(1, stateId);
                packet.getIntegers().write(2, slot);
                packet.getItemModifier().write(0, items.get(slot));
                protocol.sendServerPacket(player, packet);
            } catch (Exception ex) {
                logger.warning("SET_SLOT 失败 slot=" + slot + ": " + ex.getMessage());
                return;
            }
        }
    }

    /** Restore vanilla server stacks to client (e.g. on close / release shift). */
    public void refreshFromServer(Player player) {
        if (!protocolAvailable || player == null || !player.isOnline()) {
            return;
        }
        InventoryView view = player.getOpenInventory();
        int windowId = containerWindowId(player);
        int stateId = containerStateId(player);
        int slotCount = view.countSlots();
        List<ItemStack> items = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            ItemStack server = view.getItem(i);
            items.add(server == null ? ItemStack.empty() : server.clone());
        }
        try {
            PacketContainer packet = protocol.createPacket(PacketType.Play.Server.WINDOW_ITEMS);
            packet.getIntegers().write(0, windowId);
            packet.getIntegers().write(1, stateId);
            packet.getItemListModifier().write(0, items);
            protocol.sendServerPacket(player, packet);
        } catch (Exception e) {
            logger.warning("恢复客户端物品显示失败: " + e.getMessage());
        }
    }

    private static Object containerMenu(Player player) throws ReflectiveOperationException {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        return handle.getClass().getField("containerMenu").get(handle);
    }

    private static int containerWindowId(Player player) {
        try {
            Object menu = containerMenu(player);
            Object id = menu.getClass().getField("containerId").get(menu);
            if (id instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Paper mapping may differ
        }
        return 0;
    }

    private static int containerStateId(Player player) {
        try {
            Object menu = containerMenu(player);
            Object state = menu.getClass().getMethod("getStateId").invoke(menu);
            if (state instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Paper mapping may differ
        }
        return 0;
    }

    private ItemStack decorate(ItemStack server, boolean expand) {
        if (server == null || server.getType().isAir()) {
            return ItemStack.empty();
        }
        LoreProfile profile = registry.find(server);
        if (profile == null) {
            return server.clone();
        }
        ItemStack display = LoreComposer.forDisplay(server, profile, expand);
        return display != null ? display : server.clone();
    }
}
