package org.lonelysail.qqbot.server.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.lonelysail.qqbot.websocket.WsSender;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class QQCommand implements CommandExecutor {
    private final String name;
    private final WsSender sender;
    private final JavaPlugin plugin;  // 插件实例

    // 修改构造函数，传入插件实例
    public QQCommand(JavaPlugin plugin, WsSender sender, String name) {
        this.name = name;
        this.sender = sender;
        this.plugin = plugin;  // 保存插件实例
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return false;
        }
        String message = String.format("[%s] <%s> %s", this.name, sender.getName(), args[0]);

        // 将发送消息的操作放到异步线程中
        plugin.getServer().getScheduler().runTaskAsync(plugin, () -> {
            boolean success = this.sender.sendSynchronousMessage(message);
            // 使用主线程反馈消息
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    sender.sendMessage("§a发送消息成功！");
                } else {
                    sender.sendMessage("§c发送消息失败！");
                }
            });
        });

        return true;
    }
}
