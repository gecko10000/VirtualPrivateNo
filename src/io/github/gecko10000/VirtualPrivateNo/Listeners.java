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
        AsyncPlayerPreLoginEvent.Result result = isVPN(event.getAddress().getHostAddress())
                ? AsyncPlayerPreLoginEvent.Result.KICK_OTHER : AsyncPlayerPreLoginEvent.Result.ALLOWED;
        event.disallow(result, MiniMessage.markdown().parse(plugin.getConfig().getString("kickMessage")));
    }

    private boolean isVPN(String ip) {
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
        Bukkit.broadcast(Component.text(response.body()));
        JsonObject object = gson.fromJson(response.body(), JsonObject.class);
        return object.getAsJsonPrimitive("proxy").getAsBoolean();
    }

}
