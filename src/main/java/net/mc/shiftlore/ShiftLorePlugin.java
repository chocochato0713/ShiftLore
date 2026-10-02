package net.mc.shiftlore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ShiftLorePlugin extends JavaPlugin {

    private ProfileRegistry registry;
    private ClientInventorySync sync;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        CraftEngineBridge ce = new CraftEngineBridge(getLogger());
        registry = new ProfileRegistry(this, ce);
        registry.reload();

        sync = new ClientInventorySync(this, registry);
        if (!sync.isProtocolAvailable()) {
            getLogger().severe("未检测到 ProtocolLib，ShiftLore 无法工作（请安装 ProtocolLib 后重启）。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (ce.isAvailable()) {
            getLogger().info("已挂钩 CraftEngine 物品 ID。");
        }

        getServer().getPluginManager().registerEvents(new ShiftLoreListener(sync), this);
        getLogger().info("ShiftLore 已启用：打开容器/背包后按住 Shift 悬停物品可展开 lore。");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("shiftlore")) {
            return false;
        }
        if (!sender.hasPermission("shiftlore.admin")) {
            sender.sendMessage("§c无权限");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            registry.reload();
            sender.sendMessage("§aShiftLore 配置已重载。");
            return true;
        }
        sender.sendMessage("§e/shiftlore reload");
        return true;
    }
}
