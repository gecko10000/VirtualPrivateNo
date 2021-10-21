package io.github.gecko10000.VirtualPrivateNo;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class Listeners implements Listener {

    private final VirtualPrivateNo plugin;

    public Listeners(VirtualPrivateNo plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        String requestUrl = plugin.getConfig().getString("requestUrl")
                .replace("%ip%", ip);
    }

}
