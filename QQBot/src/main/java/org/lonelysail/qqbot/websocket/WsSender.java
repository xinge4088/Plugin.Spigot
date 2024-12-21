package org.lonelysail.qqbot.websocket;

import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.lonelysail.qqbot.Utils;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class WsSender extends WebSocketClient {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WsSender.class);
    private String message;

    private final Logger logger;
    private final Utils utils = new Utils();
    private final ExecutorService executorService = Executors.newCachedThreadPool(); // 用于异步执行任务

    public WsSender(JavaPlugin plugin, Configuration config) {
        super(URI.create(config.getString("uri")).resolve("websocket/bot"));
        this.logger = plugin.getLogger();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("name", config.getString("name"));
        headers.put("token", config.getString("token"));
        this.addHeader("info", this.utils.encode(headers));
    }

    // 判断连接是否正常
    public boolean isConnected() {
        return this.isOpen() && !this.isClosed() && !this.isClosing();
    }

    // 异步重连
    public void tryReconnectAsync() {
        executorService.submit(() -> {
            for (int count = 0; count < 3; count++) {
                logger.warning("[Sender] 检测到与机器人的连接已断开！正在尝试重连……");
                this.reconnect();
                try {
                    Thread.sleep(1000);  // 暂停1秒钟等待重连
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (this.isConnected()) {
                    this.logger.info("[Sender] 与机器人连接成功！");
                    return;
                }
            }
            logger.warning("[Sender] 尝试重连三次失败！");
        });
    }

    // 异步发送数据
    public void sendDataAsync(String eventType, Object data) {
        executorService.submit(() -> {
            // 检查是否连接
            if (!this.isConnected()) {
                tryReconnectAsync(); // 异步重连
                return;
            }

            HashMap<String, Object> messageData = new HashMap<>();
            messageData.put("data", data);
            messageData.put("type", eventType);
            try {
                this.send(this.utils.encode(messageData));
            } catch (WebsocketNotConnectedException e) {
                logger.warning("[Sender] 发送数据失败！与机器人的连接已断开。");
                tryReconnectAsync(); // 异步重连
            }
        });
    }

    // 发送服务器启动信息
    public void sendServerStartup() {
        HashMap<String, Object> data = new HashMap<>();
        sendDataAsync("server_startup", data);
    }

    // 发送服务器关闭信息
    public void sendServerShutdown() {
        HashMap<String, Object> data = new HashMap<>();
        sendDataAsync("server_shutdown", data);
    }

    // 发送玩家离开信息
    public void sendPlayerLeft(String name) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("player_name", name);
        sendDataAsync("player_left", data);
    }

    // 处理 WebSocket 连接成功
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("[Sender] WebSocket 连接已建立");
    }

    // 处理 WebSocket 错误
    @Override
    public void onError(Exception ex) {
        logger.warning("[Sender] WebSocket 发生错误：" + ex.getMessage());
        tryReconnectAsync(); // 异步重连
    }

    // 处理 WebSocket 关闭
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warning("[Sender] WebSocket 连接已关闭：" + reason);
        tryReconnectAsync(); // 异步重连
    }

    // 处理 WebSocket 接收到的数据
    @Override
    public void onMessage(String message) {
        this.message = message; // 接收到的消息
    }

    // 停止 WebSocket 连接
    public void stop() {
        this.close();
        executorService.shutdown(); // 关闭线程池
    }
}
