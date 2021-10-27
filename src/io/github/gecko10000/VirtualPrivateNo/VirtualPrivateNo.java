package io.github.gecko10000.VirtualPrivateNo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.sql.SQLHelper;

import java.sql.Connection;
import java.util.StringJoiner;
import java.util.UUID;

public class VirtualPrivateNo extends JavaPlugin {

    SQLHelper sql;
    NamespacedKey noAlertKey;

    public void onEnable() {
        saveDefaultConfig();
        startDatabase();
        noAlertKey = new NamespacedKey(this, "no_alerts");
        new Listeners(this);
        new CommandHandler(this);
    }

    public void onDisable() {
        sql.commit();
        sql.close();
    }

    private void startDatabase() {
        Connection connection = SQLHelper.openSQLite(getDataFolder().toPath().resolve("ipcache.db"));
        sql = new SQLHelper(connection);
        sql.execute("CREATE TABLE IF NOT EXISTS ips (ip INT PRIMARY KEY, vpn BIT(1));");
        sql.execute("CREATE TABLE IF NOT EXISTS whitelist (uuid STRING PRIMARY KEY);");
        sql.setAutoCommit(false);
    }

    public int ipToInt(String ip) {
        String[] split = ip.split("\\.");
        int compact = 0;
        for (int i = 0; i < split.length; i++) {
            compact |= Integer.parseInt(split[i]) << (i * 8);
        }
        return compact;
    }

    public String intToIp(int compact) {
        StringJoiner ip = new StringJoiner(".");
        for (int i = 0; i < 4; i++) {
            int mask = 255 << (i * 8);
            mask &= compact;
            ip.add((mask >>> (i*8)) + "");
        }
        return ip.toString();
    }

    public boolean isWhitelisted(UUID uuid) {
        return sql.querySingleResult("SELECT uuid FROM whitelist WHERE uuid=?;", uuid) != null;
    }

    public void inform(String name) {
        String informMessage = getConfig().getString("staffMessage")
                        .replace("%player%", name);
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("vpno.alerts"))
                .filter(p -> !p.getPersistentDataContainer().has(noAlertKey, PersistentDataType.BYTE))
                .forEach(p -> sendMessage(p, informMessage));
    }

    public void sendMessage(CommandSender sender, String input) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', input));
    }

}
