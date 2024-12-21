package org.lonelysail.qqbot;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class Utils {
    private final HashMap<String, ChatColor> mapping = new HashMap<>();
    private final Gson gson = new Gson();
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Type type = new TypeToken<HashMap<String, Object>>() {}.getType();

    public Utils() {
        this.mapping.put("black", ChatColor.BLACK);
        this.mapping.put("dark_blue", ChatColor.DARK_BLUE);
        this.mapping.put("dark_green", ChatColor.DARK_GREEN);
        this.mapping.put("dark_aqua", ChatColor.DARK_AQUA);
        this.mapping.put("dark_red", ChatColor.DARK_RED);
        this.mapping.put("dark_purple", ChatColor.DARK_PURPLE);
        this.mapping.put("gold", ChatColor.GOLD);
        this.mapping.put("gray", ChatColor.GRAY);
        this.mapping.put("dark_gray", ChatColor.DARK_GRAY);
        this.mapping.put("blue", ChatColor.BLUE);
        this.mapping.put("green", ChatColor.GREEN);
        this.mapping.put("aqua", ChatColor.AQUA);
        this.mapping.put("red", ChatColor.RED);
        this.mapping.put("light_purple", ChatColor.LIGHT_PURPLE);
        this.mapping.put("yellow", ChatColor.YELLOW);
        this.mapping.put("white", ChatColor.WHITE);
    }

    public String encode(HashMap<String, ?> originalMap) {
        String string = this.gson.toJson(originalMap);
        return this.encoder.encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    public HashMap<String, ?> decode(String original) {
        byte[] stringBytes = this.decoder.decode(original.getBytes(StandardCharsets.UTF_8));
        String decodeString = new String(stringBytes, StandardCharsets.UTF_8);
        return this.gson.fromJson(decodeString, this.type);
    }

    public void toStringMessageAsync(List<LinkedTreeMap<String, String>> original, Callback<String> callback) {
        // 异步执行字符串拼接
        Bukkit.getScheduler().runTaskAsync(plugin, () -> {
            StringBuilder message = new StringBuilder();
            for (LinkedTreeMap<String, String> section : original) {
                message.append(this.mapping.getOrDefault(section.get("color"), ChatColor.GRAY)).append(section.get("text"));
            }
            callback.onComplete(message.toString()); // 回调返回结果
        });
    }

    // 回调接口，用于异步操作完成后的返回
    public interface Callback<T> {
        void onComplete(T result);
    }
}
