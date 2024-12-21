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

public class EventListener implements Listener {
    private final WsSender sender;

    public EventListener(WsSender sender) {
        this.sender = sender;
    }

    // 当玩家退出游戏时触发
    @EventHandler
    public void PlayerQuit(PlayerQuitEvent event) {
        // 使用异步任务发送玩家离开消息
        Bukkit.getScheduler().runTaskAsync(this, () -> {
            this.sender.sendPlayerLeft(event.getPlayer().getName());
        });
    }

    // 当玩家加入游戏时触发
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        // 使用异步任务发送玩家加入消息
        Bukkit.getScheduler().runTaskAsync(this, () -> {
            this.sender.sendPlayerJoined(event.getPlayer().getName());
        });
    }

    // 当玩家聊天时触发
    @EventHandler
    public void playerChat(AsyncPlayerChatEvent event) {
        // 使用异步任务发送玩家聊天消息
        Bukkit.getScheduler().runTaskAsync(this, () -> {
            this.sender.sendPlayerChat(event.getPlayer().getName(), event.getMessage());
        });
    }

    // 当玩家死亡时触发
    @EventHandler
    public void playerDeath(PlayerDeathEvent event) {
        // 使用异步任务发送玩家死亡消息
        Bukkit.getScheduler().runTaskAsync(this, () -> {
            Player player = event.getEntity();
            this.sender.sendPlayerDeath(player.getName(), event.getDeathMessage());
        });
    }
}

