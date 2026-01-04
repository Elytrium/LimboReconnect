package net.elytrium.limboreconnect.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.elytrium.limboreconnect.LimboReconnect;

public class ConnectListener {

    public ProxyServer server;
    public LimboReconnect plugin;

    public ConnectListener(ProxyServer server, LimboReconnect plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerPreConnect(ServerPostConnectEvent event) {
        plugin.addPlayer(event.getPlayer(), null);
    }

}
