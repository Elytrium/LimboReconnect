package ru.skywatcher_2019.limboreconnect.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
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
            Player player = kickEvent.getPlayer();
            if (kickEvent.getServerKickReason().isEmpty()) {
                player.disconnect(Component.text(""));
                System.out.println(1);
                return false;
            }
            if (kickEvent.getServerKickReason().get() instanceof TranslatableComponent) {
                player.disconnect(kickEvent.getServerKickReason().get());
                System.out.println(2);
                return false;
            }
            TextComponent kickMessage = (TextComponent) kickEvent.getServerKickReason().get();
            if (kickEvent.kickedDuringServerConnect()) {
                player.disconnect(kickMessage);
                System.out.println(3);
                return false;
            }
            if (kickMessage.content().contains(Config.IMP.RESTART_MESSAGE)) {
                this.plugin.addPlayer(player);
                System.out.println(4);
                return true;
            } else {
                player.disconnect(kickMessage);
                System.out.println(5);
                return false;
            }
        });
    }
}
