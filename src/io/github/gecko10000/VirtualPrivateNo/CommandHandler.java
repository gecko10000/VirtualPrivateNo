package io.github.gecko10000.VirtualPrivateNo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.misc.Task;

import java.util.ArrayList;
import java.util.List;
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
                    List<String> uuids = plugin.sql.queryResultList("SELECT uuid from whitelist;");
                    List<String> names = plugin.sql.queryResultList("SELECT uuid from whitelist;").stream()
                            .map(String.class::cast)
                            .map(UUID::fromString)
                            .map(uuid -> {
                                String name = Bukkit.getOfflinePlayer(uuid).getName();
                                return name == null ? uuid.toString() : name;
                            })
                            .collect(Collectors.toList());
                    return names;
                }));
        new CommandParser(plugin.getResource("command.rdcml"))
                .setArgTypes(whitelistedArg)
                .parse().register("vpno", this);
    }

    @CommandHook("reload")
    public void reload(CommandSender sender) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        sender.sendMessage(MiniMessage.markdown().parse("<green>Config reloaded!"));
    }

    @CommandHook("add")
    public void addPlayer(CommandSender sender, String target) {
        getOfflinePlayer(target).thenAccept(player -> {
            if (plugin.isWhitelisted(player.getUniqueId())) {
                sender.sendMessage(MiniMessage.markdown().parse("<red>" + player.getName() + " is already whitelisted!"));
                return;
            }
            plugin.sql.execute("INSERT INTO whitelist (uuid) VALUES (?);", player.getUniqueId());
            plugin.sql.commit();
            sender.sendMessage(MiniMessage.markdown().parse("<green>Added " + player.getName() + " to the whitelist."));
        });
    }

    @CommandHook("remove")
    public void removePlayer(CommandSender sender, String target) {
        getOfflinePlayer(target).thenAccept(player -> {
            if (!plugin.isWhitelisted(player.getUniqueId())) {
                sender.sendMessage(MiniMessage.markdown().parse("<red>" + target + " is not whitelisted!"));
                return;
            }
            plugin.sql.execute("DELETE FROM whitelist WHERE uuid=?;", player.getUniqueId());
            plugin.sql.commit();
            sender.sendMessage(MiniMessage.markdown().parse("<green>Removed " + target + " from the whitelist."));
        });
    }

    private CompletableFuture<OfflinePlayer> getOfflinePlayer(String target) {
        Player player = Bukkit.getPlayer(target);
        CompletableFuture<OfflinePlayer> uuid = new CompletableFuture<>();
        if (player != null) {
            uuid.complete(player);
        } else {
            Task.asyncDelayed(() -> {
                uuid.complete(target.length() > 16 ? Bukkit.getOfflinePlayer(UUID.fromString(target)) : Bukkit.getOfflinePlayer(target));
            });
        }
        return uuid;
    }

}
