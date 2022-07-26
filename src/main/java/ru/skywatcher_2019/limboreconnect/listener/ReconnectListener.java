/*
 * Copyright (C) 2022 - 2022 SkyWatcher_2019
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
            Component kickReason = kickEvent.getServerKickReason().isPresent() ? kickEvent.getServerKickReason().get() : Component.empty();
            String kickMessage;

            if (kickReason instanceof TranslatableComponent) {
                kickMessage = ((TranslatableComponent) kickReason).key();
            } else {
                kickMessage = ((TextComponent) kickReason).content();
            }

            if (kickEvent.kickedDuringServerConnect()) {
                player.disconnect(kickReason);
                return false;
            }

            if (kickMessage.contains(Config.IMP.RESTART_MESSAGE)) {
                this.plugin.addPlayer(player);
                return true;
            } else {
                player.disconnect(kickReason);
                return false;
            }
        });
    }
}
