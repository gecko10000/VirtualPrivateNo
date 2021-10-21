package io.github.gecko10000.VirtualPrivateNo;

import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.sql.SQLHelper;

import java.sql.Connection;
import java.util.StringJoiner;

public class VirtualPrivateNo extends JavaPlugin {

    SQLHelper sql;

    public void onEnable() {
        saveDefaultConfig();
        startDatabase();
        new Listeners(this);
    }

    public void onDisable() {
        sql.commit();
        sql.close();
    }

    private void startDatabase() {
        Connection connection = SQLHelper.openSQLite(getDataFolder().toPath().resolve("ipcache.db"));
        sql = new SQLHelper(connection);
        sql.execute("CREATE TABLE IF NOT EXISTS ips (ip INT, vpn BIT(1));");
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
            ip.add((mask >> (i*8)) + "");
        }
        return ip.toString();
    }

}
