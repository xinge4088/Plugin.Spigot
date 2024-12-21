package org.lonelysail.qqbot;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.lonelysail.qqbot.server.EventListener;
import org.lonelysail.qqbot.server.commands.QQCommand;
import org.lonelysail.qqbot.websocket.WsListener;
import org.lonelysail.qqbot.websocket.WsSender;

import java.util.logging.Level;

public final class QQBot extends JavaPlugin {
    private Configuration config;

    private WsListener websocketListener;
    private WsSender websocketSender;

    // 插件加载时调用的方法，初始化配置文件
    @Override
    public void onEnable() {
        // 加载配置文件
        this.saveDefaultConfig();
        this.config = this.getConfig();

        // 初始化 WebSocket 连接
        this.getLogger().info("正在初始化与机器人的连接……");
        initWebSocketConnections();

        // 注册事件监听器
        EventListener eventListener = new EventListener(this.websocketSender);
        this.getServer().getPluginManager().registerEvents(eventListener, this);

        // 注册插件命令
        registerCommands();

        // 延迟发送服务器启动信息，给 WebSocket 连接一些时间
        Bukkit.getScheduler().runTaskLater(this, () -> websocketSender.sendServerStartup(), 20);
    }

    private void initWebSocketConnections() {
        try {
            // 初始化 WebSocket 发送器并连接
            this.websocketSender = new WsSender(this, this.config);
            this.websocketSender.connect();

            // 初始化 WebSocket 监听器并连接
            this.websocketListener = new WsListener(this, this.config);
            this.websocketListener.connect();
        } catch (Exception e) {
            this.getLogger().log(Level.SEVERE, "初始化 WebSocket 连接失败！", e);
            // 如果初始化失败，可以考虑停止插件
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerCommands() {
        QQCommand command = new QQCommand(this.websocketSender, this.config.getString("name"));
        if (getCommand("qq") != null) {
            getCommand("qq").setExecutor(command);
        } else {
            this.getLogger().warning("命令 /qq 没有注册成功！");
        }
    }

    // 插件禁用时调用的方法，关闭各种服务
    @Override
    public void onDisable() {
        // 关闭 WebSocket 连接并发送服务器关闭消息
        if (websocketSender != null) {
            websocketSender.sendServerShutdown();
            websocketSender.close();
        }
        if (websocketListener != null) {
            websocketListener.setServerRunning(false);
            websocketListener.close();
        }
    }
}


