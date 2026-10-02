package net.mc.shiftlore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShiftLoreListener implements Listener {

    private final ClientInventorySync sync;
    private final Set<UUID> containerOpen = ConcurrentHashMap.newKeySet();

    public ShiftLoreListener(ClientInventorySync sync) {
        this.sync = sync;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        containerOpen.add(player.getUniqueId());
        sync.refreshLater(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        containerOpen.remove(player.getUniqueId());
        sync.refreshFromServer(player);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!containerOpen.contains(player.getUniqueId())) {
            return;
        }
        sync.refreshLater(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        containerOpen.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        containerOpen.remove(event.getPlayer().getUniqueId());
    }
}
