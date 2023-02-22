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
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limboreconnect.commands.LimboReconnectCommand;
import net.elytrium.limboreconnect.handler.ReconnectHandler;
import net.elytrium.limboreconnect.listener.ReconnectListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.title.Title;
import org.slf4j.Logger;

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
  private final Path configPath;
  private final Path dataDirectory;
  private final LimboFactory factory;
  private final List<Title> offlineTitles = new ArrayList<>();
  private Limbo limbo;
  private Component connectingMessage;

  @Inject
  public LimboReconnect(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
    setLogger(logger);
    this.server = server;

    try {
      Class.forName("com.velocitypowered.api.proxy.server.PingOptions");
    } catch (Throwable throwable) {
      throw new UnsupportedOperationException("You are using outdated velocity build! Please update velocity to build #224+");
    }

    this.dataDirectory = dataDirectory;
    this.configPath = dataDirectory.resolve("config.yml");

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
    Config.IMP.reload(this.configPath);

    setSerializer(Config.IMP.SERIALIZER.getSerializer());

    VirtualWorld world = this.factory.createVirtualWorld(Dimension.valueOf(Config.IMP.WORLD.DIMENSION), 0, 100, 0, (float) 90, (float) 0.0);

    if (Config.IMP.WORLD.LOAD_WORLD) {
      try {
        Path path = this.dataDirectory.resolve(Config.IMP.WORLD.WORLD_FILE_PATH);
        WorldFile file = this.factory.openWorldFile(Config.IMP.WORLD.WORLD_FILE_TYPE, path);

        Config.WORLD.WORLD_COORDS coords = Config.IMP.WORLD.WORLD_COORDS;
        file.toWorld(this.factory, world, coords.X, coords.Y, coords.Z, Config.IMP.WORLD.WORLD_LIGHT_LEVEL);
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    this.limbo = this.factory.createLimbo(world).setName("LimboReconnect").setWorldTime(6000).setShouldRejoin(Config.IMP.USE_LIMBO)
        .setShouldRespawn(Config.IMP.USE_LIMBO);

    this.offlineTitles.clear();
    Config.IMP.MESSAGES.TITLES.forEach(title -> this.offlineTitles.add(Title.title(
        SERIALIZER.deserialize(title.TITLE),
        SERIALIZER.deserialize(title.SUBTITLE),
        Title.Times.times(
            Duration.ofMillis(0),
            Duration.ofMillis(1000 * 30),
            Duration.ofMillis(0)
        )
    )));

    this.connectingMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.CONNECTING);

    this.server.getEventManager().unregisterListeners(this);
    this.server.getEventManager().register(this, new ReconnectListener(this));
    this.server.getCommandManager().unregister("limboreconnect");
    this.server.getCommandManager().register("limboreconnect", new LimboReconnectCommand(this));
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
    this.limbo.spawnPlayer(player, new ReconnectHandler(this, server));
  }

  public Component getConnectingMessage() {
    return this.connectingMessage;
  }

  public List<Title> getOfflineTitles() {
    return this.offlineTitles;
  }

  public static ComponentSerializer<Component, Component, String> getSerializer() {
    return SERIALIZER;
  }


  public static Logger getLogger() {
    return LOGGER;
  }

  private static void setLogger(Logger logger) {
    LOGGER = logger;
  }
}
