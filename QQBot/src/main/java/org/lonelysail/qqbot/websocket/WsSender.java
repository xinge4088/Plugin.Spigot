package org.lonelysail.qqbot.websocket;

import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.client.WebSocketClient;
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
        // 设置请求头
        HashMap<String, String> headers = new HashMap<>();
        headers.put("name", config.getString("name"));
        headers.put("token", config.getString("token"));
        // 可以在此处设置自定义请求头
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
                try {
                    this.reconnectBlocking();  // 使用阻塞式重连
                } catch (InterruptedException e) {
                    logger.error("[Sender] 重连时发生错误", e);
                    Thread.currentThread().interrupt();
                    return;
                }
                if (this.isConnected()) {
                    logger.info("[Sender] 与机器人连接成功！");
                    return;
                }
                try {
                    Thread.sleep(1000);  // 暂停1秒钟等待重连
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            logger.warning("[Sender] 尝试重连三次失败！");
        });
    }

    // 异步发送数据
    public void sendDataAsync(String eventType, Object data) {
        executorService.submit(() -> {
            if (!this.isConnected()) {
                logger.warning("[Sender] WebSocket 当前未连接，尝试重连...");
                tryReconnectAsync(); // 如果未连接，则尝试重连
                return;
            }

            HashMap<String, Object> messageData = new HashMap<>();
            messageData.put("data", data);
            messageData.put("type", eventType);

            try {
                String encodedMessage = this.utils.encode(messageData);  // 编码消息
                this.send(encodedMessage);  // 发送数据
                log.debug("[Sender] 发送数据: {}", messageData);
            } catch (Exception e) {
                logger.warning("[Sender] 发送数据失败！" + e.getMessage());
                tryReconnectAsync(); // 如果发送失败，则尝试重连
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
        log.debug("[Sender] 接收到消息: {}", message);
    }

    // 停止 WebSocket 连接
    public void stop() {
        this.close();
        executorService.shutdownNow();  // 强制关闭线程池
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warning("[Sender] 线程池在30秒内未正常终止");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("[Sender] 停止过程中线程池中断");
        }
    }
}
