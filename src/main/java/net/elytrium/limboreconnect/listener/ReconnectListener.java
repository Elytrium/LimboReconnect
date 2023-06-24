/*
 * Copyright (C) 2022 - 2023 Elytrium
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

package net.elytrium.limboreconnect.listener;

import com.velocitypowered.api.event.Subscribe;
import java.util.Objects;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboreconnect.Config;
import net.elytrium.limboreconnect.LimboReconnect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ReconnectListener {

  public static final PlainTextComponentSerializer SERIALIZER = PlainTextComponentSerializer.builder().flattener(
      ComponentFlattener.basic()
  ).build();

  private final LimboReconnect plugin;

  public ReconnectListener(LimboReconnect plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
    event.setOnKickCallback(kickEvent -> {
      Component kickReason = kickEvent.getServerKickReason().isPresent() ? kickEvent.getServerKickReason().get() : Component.empty();
      String kickMessage = Objects.requireNonNullElse(SERIALIZER.serialize(kickReason), "unknown");
      if (Config.IMP.DEBUG) {
        LimboReconnect.getLogger().info("Component: {}", kickReason);
        LimboReconnect.getLogger().info("Kick message: {}", kickMessage);
        LimboReconnect.getLogger().info("Config: {}", Config.IMP.RESTART_MESSAGE);
        LimboReconnect.getLogger().info("Match: {}", kickMessage.matches(Config.IMP.RESTART_MESSAGE));
      }

      if (kickMessage.equals("") || kickMessage.matches(Config.IMP.RESTART_MESSAGE)) {
        this.plugin.addPlayer(kickEvent.getPlayer(), kickEvent.getServer());
        return true;
      } else {
        return false;
      }
    });
  }
}
