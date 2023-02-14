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

package net.elytrium.limboreconnect.handler;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboreconnect.Config;
import net.elytrium.limboreconnect.LimboReconnect;

public class ReconnectHandler implements LimboSessionHandler {

  private final LimboReconnect plugin;
  private final RegisteredServer server;
  private LimboPlayer player;
  private boolean connected = true;
  private int titleIndex = -1;

  public ReconnectHandler(LimboReconnect plugin, RegisteredServer server) {
    this.plugin = plugin;
    this.server = server;
  }

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    this.player = player;
    this.player.disableFalling();
    this.tick();
    this.tickMessages();
  }

  @Override
  public void onDisconnect() {
    this.connected = false;
  }

  private void tick() {
    try {
      this.server.ping().get(Config.IMP.PING_TIMEOUT, TimeUnit.MILLISECONDS);
    } catch (CompletionException | InterruptedException | ExecutionException | TimeoutException e) {

      this.player.getScheduledExecutor().schedule(this::tick, Config.IMP.CHECK_INTERVAL, TimeUnit.MILLISECONDS);
      return;
    }

    if (!Config.IMP.MESSAGES.CONNECTING.isEmpty()) {
      this.player.getProxyPlayer().sendMessage(this.plugin.getConnectingMessage());
    }

    this.player.disconnect(this.server);
  }

  private void tickMessages() {
    if (++this.titleIndex == this.plugin.getOfflineTitles().size()) {
      this.titleIndex = 0;
    }
    this.player.getProxyPlayer().showTitle(this.plugin.getOfflineTitles().get(this.titleIndex));

    if (this.connected) {
      this.player.getScheduledExecutor()
        .schedule(this::tickMessages, Config.IMP.MESSAGES.RESTART_MESSAGES_SETTINGS.SHOW_DELAY * 50, TimeUnit.MILLISECONDS);
    }
  }
}
