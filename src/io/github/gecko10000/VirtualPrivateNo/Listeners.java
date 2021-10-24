package io.github.gecko10000.VirtualPrivateNo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Listeners implements Listener {

    private final VirtualPrivateNo plugin;
    private final HttpClient client;
    private final Gson gson = new Gson();

    public Listeners(VirtualPrivateNo plugin) {
        client = HttpClient.newHttpClient();
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onJoin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        if (plugin.isWhitelisted(event.getUniqueId())) {
            return;
        }
        String ip = event.getAddress().getHostAddress();
        AsyncPlayerPreLoginEvent.Result result = isVPN(ip)
                ? AsyncPlayerPreLoginEvent.Result.KICK_OTHER : AsyncPlayerPreLoginEvent.Result.ALLOWED;
        event.disallow(result, MiniMessage.markdown().parse(plugin.getConfig().getString("kickMessage")));
        if (result != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.inform(event.getName());
        }
    }

    private boolean isVPN(String ip) {
        int ipInt = plugin.ipToInt(ip);
        Integer vpn = plugin.sql.querySingleResult("SELECT vpn FROM ips WHERE ip=?;", ipInt);
        return vpn != null ? vpn > 0 : webReq(ip);
    }

    private boolean webReq(String ip) {
        String requestUrl = plugin.getConfig().getString("requestUrl")
                .replace("%ip%", ip);
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl)).build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        JsonObject object = gson.fromJson(response.body(), JsonObject.class);
        if (!object.getAsJsonPrimitive("success").getAsBoolean()) {
            plugin.getLogger().warning("API request was unsuccessful.");
            return false;
        }
        boolean proxy = object.getAsJsonPrimitive("proxy").getAsBoolean();
        plugin.sql.execute("INSERT INTO ips (ip, vpn) VALUES (?, ?)", plugin.ipToInt(ip), proxy ? 1 : 0);
        plugin.sql.commit();
        return proxy;
    }

}
