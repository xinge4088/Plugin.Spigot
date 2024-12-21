package org.lonelysail.qqbot;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.lonelysail.qqbot.server.EventListener;
import org.lonelysail.qqbot.server.commands.QQCommand;
import org.lonelysail.qqbot.websocket.WsListener;
import org.lonelysail.qqbot.websocket.WsSender;

import java.util.Objects;

public final class QQBot extends JavaPlugin {
    public Configuration config;

    private WsListener websocketListener;
    private WsSender websocketSender;

    // 插件加载时调用的方法，初始化配置文件
    @Override
    public void onLoad() {
        this.saveDefaultConfig();
        this.config = this.getConfig();
    }

    // 插件启用时调用的方法，初始化并启动各种服务
    @Override
    public void onEnable() {
        this.getLogger().info("正在初始化与机器人的连接……");

        // 使用异步线程来连接 WebSocket，避免阻塞主线程
        Bukkit.getScheduler().runTask(this, () -> {
            // 初始化 WebSocket 发送器和监听器
            this.websocketSender = new WsSender(this, this.config);
            this.websocketListener = new WsListener(this, this.config);

            // 异步连接 WebSocket
            Bukkit.getScheduler().runTask(this, () -> {
                this.websocketSender.connect();
                this.websocketListener.connect();
            });

            // 注册事件监听器和命令
            EventListener eventListener = new EventListener(this.websocketSender);
            QQCommand command = new QQCommand(this.websocketSender, this.config.getString("name"));
            Objects.requireNonNull(this.getCommand("qq")).setExecutor(command);
            this.getServer().getPluginManager().registerEvents(eventListener, this);

            // 延迟执行 WebSocket 启动通知，避免阻塞
            Bukkit.getScheduler().runTaskLater(this, this.websocketSender::sendServerStartup, 20);
        });
    }

    // 插件禁用时调用的方法，关闭各种服务
    @Override
    public void onDisable() {
        // 使用异步线程关闭 WebSocket 连接
        Bukkit.getScheduler().runTask(this, () -> {
            this.websocketSender.sendServerShutdown();
            this.websocketSender.close();
            this.websocketListener.serverRunning = false;
            this.websocketListener.close();
        });
    }
}
