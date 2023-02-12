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

package net.elytrium.limboreconnect;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboreconnect.handler.ReconnectHandler;
import net.elytrium.limboreconnect.listener.ReconnectListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.title.Title;

@Plugin(
    id = "limboreconnect",
    name = "LimboReconnect",
    version = BuildConstants.VERSION,
    authors = {"SkyWatcher_2019", "hevav"}
)
public class LimboReconnect {

  @Inject
  private static ComponentSerializer<Component, Component, String> SERIALIZER;
  private final ProxyServer server;
  private final File configFile;
  private final LimboFactory factory;
  public Map<LimboPlayer, RegisteredServer> players = new ConcurrentHashMap<>();
  private Limbo limbo;
  private ScheduledTask limboTask;
  private Component offlineServerMessage;
  private Component connectingMessage;
  private Component offlineTitleMessage;
  private Component offlineSubtitleMessage;
  private Component connectingTitleMessage;
  private Component connectingSubtitleMessage;

  @Inject
  public LimboReconnect(ProxyServer server, @DataDirectory Path dataDirectory) {
    this.server = server;

    File dataDirectoryFile = dataDirectory.toFile();
    this.configFile = new File(dataDirectoryFile, "config.yml");

    this.factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
  }

  private static void setSerializer(ComponentSerializer<Component, Component, String> serializer) {
    SERIALIZER = serializer;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.reload();
  }

  public void reload() {
    Config.IMP.reload(this.configFile);

    setSerializer(Config.IMP.SERIALIZER.getSerializer());

    VirtualWorld world = this.factory.createVirtualWorld(Dimension.valueOf(Config.IMP.WORLD.DIMENSION), 0, 100, 0, (float) 90, (float) 0.0);
    this.limbo = this.factory.createLimbo(world).setName("LimboReconnect").setWorldTime(6000);

    this.offlineServerMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.SERVER_OFFLINE);
    this.connectingMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.CONNECTING);
    this.offlineTitleMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.TITLE.OFFLINE_TITLE);
    this.offlineSubtitleMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.TITLE.OFFLINE_SUBTITLE);
    this.connectingTitleMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.TITLE.CONNECTING_TITLE);
    this.connectingSubtitleMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.TITLE.CONNECTING_SUBTITLE);

    this.server.getEventManager().register(this, new ReconnectListener(this));

    this.startTask();
  }

  private ProxyServer getServer() {
    return this.server;
  }

  public void addPlayer(Player player, RegisteredServer server) {
    ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
    MinecraftConnection connection = connectedPlayer.getConnection();
    MinecraftSessionHandler minecraftSessionHandler = connection.getSessionHandler();
    if (minecraftSessionHandler != null) {
      if (minecraftSessionHandler instanceof ClientPlaySessionHandler) {
        ClientPlaySessionHandler sessionHandler = (ClientPlaySessionHandler) minecraftSessionHandler;
        for (UUID bossBar : sessionHandler.getServerBossBars()) {
          BossBar deletePacket = new BossBar();
          deletePacket.setUuid(bossBar);
          deletePacket.setAction(BossBar.REMOVE);
          connectedPlayer.getConnection().delayedWrite(deletePacket);
        }
        sessionHandler.getServerBossBars().clear();
      }
    }

    connectedPlayer.getTabList().clearAll();

    player.showTitle(Title.title(
        this.offlineTitleMessage,
        this.offlineSubtitleMessage,
        Title.Times.times(
            Duration.ofMillis(Config.IMP.TITLE.FADE_IN * 50L),
            Duration.ofDays(32),
            Duration.ofMillis(Config.IMP.TITLE.FADE_OUT * 50L)
        )
    ));

    this.limbo.spawnPlayer(player, new ReconnectHandler(this, server));
  }

  private void startTask() {
    if (this.limboTask != null) {
      this.limboTask.cancel();
    }
    this.limboTask = this.getServer().getScheduler().buildTask(this, () -> {
      if (this.players.isEmpty()) {
        return;
      }
      this.players.forEach((player, server) -> {
        try {
          server.ping().get(Config.IMP.PING_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (CompletionException | InterruptedException | ExecutionException | TimeoutException e) {
          if (!Config.IMP.MESSAGES.SERVER_OFFLINE.isEmpty()) {
            player.getProxyPlayer().sendMessage(this.offlineServerMessage);
          }
          return;
        }
        if (!Config.IMP.MESSAGES.CONNECTING.isEmpty()) {
          player.getProxyPlayer().sendMessage(this.connectingMessage);
        }
        player.getProxyPlayer().showTitle(Title.title(
            this.connectingTitleMessage,
            this.connectingSubtitleMessage,
            Title.Times.times(
                Duration.ofMillis(Config.IMP.TITLE.FADE_IN * 50L),
                Duration.ofDays(32),
                Duration.ofMillis(Config.IMP.TITLE.FADE_OUT * 50L)
            )
        ));
        player.disconnect(server);
      });
    }).repeat(Config.IMP.CHECK_INTERVAL, TimeUnit.MILLISECONDS).schedule();
  }
}
