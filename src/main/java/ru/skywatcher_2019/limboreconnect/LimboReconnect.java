/*
 * Copyright (C) 2022 SkyWatcher_2019
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

package ru.skywatcher_2019.limboreconnect;

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
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.slf4j.Logger;
import ru.skywatcher_2019.limboreconnect.handler.ReconnectHandler;
import ru.skywatcher_2019.limboreconnect.listener.ReconnectListener;

@Plugin(
    id = "limboreconnect",
    name = "LimboReconnect",
    version = BuildConstants.VERSION,
    authors = {"SkyWatcher_2019", "hevav"}
)
public class LimboReconnect {

  @Inject
  private static Logger LOGGER;
  private static ComponentSerializer<Component, Component, String> SERIALIZER;
  private final ProxyServer server;
  private final File configFile;
  private final LimboFactory factory;
  public Map<LimboPlayer, RegisteredServer> players = new ConcurrentHashMap<>();
  private Limbo limbo;
  private ScheduledTask limboTask;
  private Component offlineServerMessage;
  private Component connectingMessage;

  @Inject
  public LimboReconnect(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
    setLogger(logger);

    this.server = server;

    File dataDirectoryFile = dataDirectory.toFile();
    this.configFile = new File(dataDirectoryFile, "config.yml");

    this.factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
  }

  private static void setSerializer(ComponentSerializer<Component, Component, String> serializer) {
    SERIALIZER = serializer;
  }

  private static void setLogger(Logger logger) {
    LOGGER = logger;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.reload();
  }

  public void reload() {
    Config.IMP.reload(this.configFile);

    ComponentSerializer<Component, Component, String> serializer = Serializers.MINIMESSAGE.getSerializer();
    if (serializer == null) {
      LOGGER.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
      setSerializer(Serializers.LEGACY_AMPERSAND.getSerializer());
    } else {
      setSerializer(serializer);
    }

    VirtualWorld world = this.factory.createVirtualWorld(Dimension.OVERWORLD, 0, 100, 0, (float) 90, (float) 0.0);
    this.limbo = this.factory.createLimbo(world).setName("LimboReconnect").setWorldTime(6000);

    this.offlineServerMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.SERVER_OFFLINE);
    this.connectingMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.CONNECTING);

    this.server.getEventManager().register(this, new ReconnectListener(this));

    startTask();
  }

  private ProxyServer getServer() {
    return this.server;
  }

  public void addPlayer(Player player, RegisteredServer server) {
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
          player.getProxyPlayer().sendMessage(this.offlineServerMessage);
          return;
        }
        player.getProxyPlayer().sendMessage(this.connectingMessage);
        player.disconnect(server);
      });
    }).repeat(Config.IMP.CHECK_INTERVAL, TimeUnit.MILLISECONDS).schedule();
  }
}
