package io.github.gecko10000.VirtualPrivateNo;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.misc.Task;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandHandler {

    private final VirtualPrivateNo plugin;

    public CommandHandler(VirtualPrivateNo plugin) {
        this.plugin = plugin;
        // for proper tab completion upon removal
        ArgType<String> whitelistedArg = new ArgType<>("whitelisted", u -> u)
                .setTab((sender -> {
                    return plugin.sql.queryResultList("SELECT uuid from whitelist;").stream()
                            .map(String.class::cast)
                            .map(UUID::fromString)
                            .map(uuid -> {
                                String name = Bukkit.getOfflinePlayer(uuid).getName();
                                return name == null ? uuid.toString() : name;
                            })
                            .collect(Collectors.toList());
                }));
        new CommandParser(plugin.getResource("command.rdcml"))
                .setArgTypes(whitelistedArg)
                .parse().register("vpno", this);
    }

    @CommandHook("reload")
    public void reload(CommandSender sender) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        plugin.sendMessage(sender, "<green>Config reloaded!");
    }

    @CommandHook("add")
    public void addPlayer(CommandSender sender, String target) {
        getOfflinePlayer(target).thenAccept(player -> {
            if (plugin.isWhitelisted(player.getUniqueId())) {
                plugin.sendMessage(sender, "&c" + player.getName() + " is already whitelisted!");
                return;
            }
            plugin.sql.execute("INSERT INTO whitelist (uuid) VALUES (?);", player.getUniqueId());
            plugin.sql.commit();
            plugin.sendMessage(sender, "&aAdded " + player.getName() + " to the whitelist.");
        });
    }

    @CommandHook("remove")
    public void removePlayer(CommandSender sender, String target) {
        getOfflinePlayer(target).thenAccept(player -> {
            if (!plugin.isWhitelisted(player.getUniqueId())) {
                plugin.sendMessage(sender, "&c" + target + " is not whitelisted!");
                return;
            }
            plugin.sql.execute("DELETE FROM whitelist WHERE uuid=?;", player.getUniqueId());
            plugin.sql.commit();
            plugin.sendMessage(sender, "&aRemoved " + target + " from the whitelist.");
        });
    }

    @CommandHook("alerts")
    public void toggleAlerts(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        boolean alerts = !pdc.has(plugin.noAlertKey, PersistentDataType.BYTE);
        if (alerts) {
            pdc.set(plugin.noAlertKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            pdc.remove(plugin.noAlertKey);
        }
        plugin.sendMessage(player, "&aTurned your alerts " + (alerts ? "off." : "on."));
    }

    private CompletableFuture<OfflinePlayer> getOfflinePlayer(String target) {
        Player player = Bukkit.getPlayer(target);
        CompletableFuture<OfflinePlayer> offlinePlayer = new CompletableFuture<>();
        if (player != null) {
            offlinePlayer.complete(player);
        } else {
            Task.asyncDelayed(() -> {
                offlinePlayer.complete(target.length() > 16 ? Bukkit.getOfflinePlayer(UUID.fromString(target)) : Bukkit.getOfflinePlayer(target));
            });
        }
        return offlinePlayer;
    }

}
