package org.lonelysail.qqbot.server;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.lonelysail.qqbot.websocket.WsSender;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class EventListener implements Listener {
    private final JavaPlugin plugin;  // 插件实例
    private final WsSender sender;   // WebSocket 发送器

    // 构造函数接收插件实例和发送器
    public EventListener(JavaPlugin plugin, WsSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    // 当玩家退出游戏时触发
    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        // 使用插件实例来调度异步任务
        plugin.getServer().getScheduler().runTaskAsync(plugin, () -> {
            this.sender.sendPlayerLeft(event.getPlayer().getName());
        });
    }

    // 当玩家加入游戏时触发
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        // 使用插件实例来调度异步任务
        plugin.getServer().getScheduler().runTaskAsync(plugin, () -> {
            this.sender.sendPlayerJoined(event.getPlayer().getName());
        });
    }

    // 当玩家聊天时触发
    @EventHandler
    public void playerChat(AsyncPlayerChatEvent event) {
        // 使用插件实例来调度异步任务
        plugin.getServer().getScheduler().runTaskAsync(plugin, () -> {
            this.sender.sendPlayerChat(event.getPlayer().getName(), event.getMessage());
        });
    }

    // 当玩家死亡时触发
    @EventHandler
    public void playerDeath(PlayerDeathEvent event) {
        // 使用插件实例来调度异步任务
        plugin.getServer().getScheduler().runTaskAsync(plugin, () -> {
            Player player = event.getEntity();
            this.sender.sendPlayerDeath(player.getName(), event.getDeathMessage());
        });
    }
}

