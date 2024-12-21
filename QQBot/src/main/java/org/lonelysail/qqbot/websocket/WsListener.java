package org.lonelysail.qqbot.websocket;

import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.lonelysail.qqbot.Utils;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class WsListener extends WebSocketClient {
    public boolean serverRunning = true;
    private final Logger logger;
    private final Server server;
    private final JavaPlugin plugin;

    private final Utils utils = new Utils();
    private final OperatingSystemMXBean bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    public WsListener(JavaPlugin plugin, Configuration config) {
        super(URI.create(Objects.requireNonNull(config.getString("uri"))).resolve("websocket/minecraft"));
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.server = plugin.getServer();

        // 添加请求头信息
        HashMap<String, String> headers = new HashMap<>();
        headers.put("name", config.getString("name"));
        headers.put("token", config.getString("token"));
        this.addHeader("type", "Spigot");
        this.addHeader("info", this.utils.encode(headers));
    }

    // 处理命令请求
    private String command(String data) {
        // 确保命令在主线程执行
        Bukkit.getScheduler().runTask(this.plugin, () -> this.server.dispatchCommand(this.server.getConsoleSender(), data));
        return "命令已发送到服务器！当前插件不支持获取命令返回值。";
    }

    // 获取在线玩家列表
    private List<String> playerList(String data) {
        List<String> players = new ArrayList<>();
        for (Player player : this.server.getOnlinePlayers()) {
            players.add(player.getName());
        }
        return players;
    }

    // 获取服务器资源占用情况
    private List<Double> serverOccupation(String data) {
        Runtime runtime = Runtime.getRuntime();
        List<Double> serverOccupations = new ArrayList<>();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        serverOccupations.add(this.bean.getProcessCpuLoad() * 100);  // 获取 CPU 占用率
        serverOccupations.add(((double) ((totalMemory - freeMemory)) / totalMemory) * 100);  // 获取内存占用率
        return serverOccupations;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        this.logger.info("[Listener] 与机器人成功建立连接！");
    }

    @Override
    public void onMessage(String message) {
        HashMap<String, ?> map = this.utils.decode(message);
        Object data = map.get("data");
        String eventType = (String) map.get("type");
        this.logger.fine("收到机器人消息 " + map);
        Object response;
        HashMap<String, Object> responseMessage = new HashMap<>();

        // 根据不同事件类型处理相应的数据
        if (Objects.equals(eventType, "message")) {
            String broadcastMessage = this.utils.toStringMessage((List) data);
            this.server.broadcastMessage(broadcastMessage);
            this.logger.fine("[Listener] 收到广播消息: " + broadcastMessage);
        } else if (Objects.equals(eventType, "command")) {
            response = this.command((String) data);
        } else if (Objects.equals(eventType, "player_list")) {
            response = this.playerList((String) data);
        } else if (Objects.equals(eventType, "server_occupation")) {
            response = this.serverOccupation((String) data);
        } else {
            this.logger.warning("[Listener] 未知的事件类型: " + eventType);
            responseMessage.put("success", false);
            responseMessage.put("error", "未知的事件类型");
            this.send(this.utils.encode(responseMessage));  // 发送失败响应
            return;
        }

        responseMessage.put("success", true);
        responseMessage.put("data", response);
        this.logger.fine("发送响应消息: " + responseMessage);
        this.send(this.utils.encode(responseMessage));  // 发送成功响应
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        this.logger.warning("[Listener] 与机器人的链接已关闭！关闭原因: " + reason);
        if (this.serverRunning) {
            this.logger.info("[Listener] 正在尝试重新连接...");
            // 在主线程中延迟执行重连操作
            Bukkit.getScheduler().runTaskLater(this.plugin, this::reconnect, 100);
        }
    }

    @Override
    public void onError(Exception ex) {
        this.logger.warning("[Listener] 机器人连接发生错误: " + ex.getMessage());
        ex.printStackTrace();
    }

    // 重新连接方法
    private void reconnect() {
        try {
            this.reconnectBlocking();
        } catch (Exception e) {
            this.logger.severe("[Listener] 重连失败: " + e.getMessage());
        }
    }
}
