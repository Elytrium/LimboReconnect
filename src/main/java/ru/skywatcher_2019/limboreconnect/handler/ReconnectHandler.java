package ru.skywatcher_2019.limboreconnect.handler;

import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import ru.skywatcher_2019.limboreconnect.LimboReconnect;

public class ReconnectHandler implements LimboSessionHandler {
    private final LimboReconnect plugin;
    private LimboPlayer player;

    public ReconnectHandler(LimboReconnect plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.player = player;
        this.player.disableFalling();
        this.plugin.players.add(player);
    }

    @Override
    public void onDisconnect() {
        this.plugin.players.remove(this.player);
    }
}
