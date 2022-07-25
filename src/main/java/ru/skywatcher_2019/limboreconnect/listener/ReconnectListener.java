package ru.skywatcher_2019.limboreconnect.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.kyori.adventure.text.TextComponent;
import ru.skywatcher_2019.limboreconnect.Config;
import ru.skywatcher_2019.limboreconnect.LimboReconnect;

public class ReconnectListener {
    private final LimboReconnect plugin;

    public ReconnectListener(LimboReconnect plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
        event.setOnKickCallback(kickEvent -> {
            if (kickEvent.kickedDuringServerConnect()) return false;
            if (kickEvent.getServerKickReason().isEmpty()) return false;
            TextComponent kickMessage = (TextComponent) kickEvent.getServerKickReason().get();
            Player player = kickEvent.getPlayer();
            if (!kickMessage.content().contains(Config.IMP.RESTART_MESSAGE)) {
                player.disconnect(kickMessage);
                return false;
            }
            this.plugin.addPlayer(player, kickEvent.getServer());
            return true;
        });
    }
}
