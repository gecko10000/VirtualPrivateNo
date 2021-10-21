package io.github.gecko10000.VirtualPrivateNo;

import org.bukkit.plugin.java.JavaPlugin;

public class VirtualPrivateNo extends JavaPlugin {

    public void onEnable() {
        saveDefaultConfig();
        new Listeners(this);
    }

}
