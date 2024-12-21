package org.lonelysail.qqbot.server.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.lonelysail.qqbot.websocket.WsSender;
import org.bukkit.Bukkit;

public class QQCommand implements CommandExecutor {
    private final String name;
    private final WsSender sender;

    public QQCommand(WsSender sender, String name) {
        this.name = name;
        this.sender = sender;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return false;
        }
        String message = String.format("[%s] <%s> %s", this.name, sender.getName(), args[0]);

        // 将发送消息的操作放到异步线程中
        Bukkit.getScheduler().runTaskAsync(YourPluginInstance, () -> {
            boolean success = this.sender.sendSynchronousMessage(message);
            // 使用主线程反馈消息
            Bukkit.getScheduler().runTask(YourPluginInstance, () -> {
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

