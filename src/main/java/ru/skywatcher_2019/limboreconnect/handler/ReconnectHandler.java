package ru.skywatcher_2019.limboreconnect.handler;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import ru.skywatcher_2019.limboreconnect.LimboReconnect;

public class ReconnectHandler implements LimboSessionHandler {
    private final RegisteredServer previousServer;
    private final LimboReconnect plugin;
    private LimboPlayer player;

    public ReconnectHandler(LimboReconnect plugin, RegisteredServer server) {
        this.plugin = plugin;
        this.previousServer = server;
    }

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.player = player;
        this.player.disableFalling();
        this.plugin.players.put(player, previousServer);
    }

    @Override
    public void onDisconnect() {
        this.plugin.players.remove(this.player);
    }
}
