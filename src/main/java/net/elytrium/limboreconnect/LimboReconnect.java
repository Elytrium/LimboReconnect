/*
 * Copyright (C) 2022 - 2025 Elytrium
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
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
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
import com.velocitypowered.proxy.protocol.packet.BossBarPacket;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;
import net.elytrium.limboreconnect.commands.LimboReconnectCommand;
import net.elytrium.limboreconnect.handler.ReconnectHandler;
import net.elytrium.limboreconnect.listener.ReconnectListener;
import net.elytrium.limboreconnect.protocol.packets.PlaySound;
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

  public static final Config CONFIG = new Config();
  @Inject
  private static Logger LOGGER;
  private static ComponentSerializer<Component, Component, String> SERIALIZER;
  public final List<Title> offlineTitles = new ArrayList<>();
  public final List<Title> connectingTitles = new ArrayList<>();
  public Pattern triggerMessage;
  private final ProxyServer server;
  private final Path configPath;
  private final Path dataDirectory;
  private final LimboFactory factory;
  private final CommandMeta commandMeta;
  private Limbo limbo;

  @Inject
  public LimboReconnect(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
    setLogger(logger);
    this.server = server;
    this.commandMeta = server.getCommandManager().metaBuilder("limboreconnect").plugin(this).build();

    this.dataDirectory = dataDirectory;
    this.configPath = dataDirectory.resolve("config.yml");

    this.factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
  }

  public static ComponentSerializer<Component, Component, String> getSerializer() {
    return SERIALIZER;
  }

  private static void setSerializer(ComponentSerializer<Component, Component, String> serializer) {
    SERIALIZER = serializer;
  }

  public static Logger getLogger() {
    return LOGGER;
  }

  private static void setLogger(Logger logger) {
    LOGGER = logger;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.reload();
  }

  public void reload() {
    CONFIG.reload(this.configPath);

    if (CONFIG.triggerMessage.equals("((?i)^(server closed|server is restarting|multiplayer\\.disconnect\\.server_shutdown))+$")) {
      LOGGER.warn("Looks like you using default config!");
      LOGGER.warn("Please check 'trigger-message' option and confirm, that it matches your server's restart message");
    }

    setSerializer(CONFIG.serializer.getSerializer());

    Config.World.PlayerCoords playerCoords = CONFIG.world.playerCoords;

    VirtualWorld world = this.factory.createVirtualWorld(
        Dimension.valueOf(CONFIG.world.dimension), playerCoords.x, playerCoords.y, playerCoords.z, playerCoords.yaw, playerCoords.pitch);

    try {
      this.triggerMessage = Pattern.compile(CONFIG.triggerMessage);
    } catch (PatternSyntaxException e) {
      throw new IllegalArgumentException("Unable to parse 'triggerMessage'  into a valid RegEx, check config: " + CONFIG.triggerMessage, e);
    }

    if (CONFIG.world.loadWorld) {
      try {
        Path path = this.dataDirectory.resolve(CONFIG.world.worldFilePath);
        WorldFile file = this.factory.openWorldFile(CONFIG.world.worldFileType, path);

        Config.World.WorldCoords coords = CONFIG.world.worldCoords;
        file.toWorld(this.factory, world, coords.x, coords.y, coords.z, CONFIG.world.worldLightLevel);
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    this.limbo = this.factory.createLimbo(world)
        .setName("LimboReconnect")
        .setShouldRejoin(CONFIG.useLimbo)
        .setShouldRespawn(CONFIG.useLimbo)
        .setGameMode(CONFIG.world.gamemode)
        .registerPacket(PacketDirection.CLIENTBOUND, PlaySound.class, null, new PacketMapping[] {
            new PacketMapping(0x29, ProtocolVersion.MINECRAFT_1_7_2, true),
            new PacketMapping(0x19, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x1a, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x19, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x1a, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x19, ProtocolVersion.MINECRAFT_1_16, true),
            new PacketMapping(0x18, ProtocolVersion.MINECRAFT_1_16_2, true),
            new PacketMapping(0x19, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x16, ProtocolVersion.MINECRAFT_1_19, true),
            new PacketMapping(0x17, ProtocolVersion.MINECRAFT_1_19_1, true),
            new PacketMapping(0x5e, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x62, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x64, ProtocolVersion.MINECRAFT_1_20_2, true),
            new PacketMapping(0x66, ProtocolVersion.MINECRAFT_1_20_3, true),
            new PacketMapping(0x68, ProtocolVersion.MINECRAFT_1_20_5, true),
            new PacketMapping(0x6f, ProtocolVersion.MINECRAFT_1_21_2, true),
            new PacketMapping(0x6e, ProtocolVersion.MINECRAFT_1_21_5, true),
            new PacketMapping(0x6e, ProtocolVersion.MINECRAFT_1_21_7, true),
            new PacketMapping(0x6f, ProtocolVersion.MINECRAFT_1_21_9, true),
            new PacketMapping(0x6f, ProtocolVersion.MINECRAFT_1_21_11, true)
            });

    this.offlineTitles.clear();
    CONFIG.messages.titles.titles.forEach(title -> this.offlineTitles.add(Title.title(
        SERIALIZER.deserialize(title.title),
        SERIALIZER.deserialize(title.subtitle),
        Title.Times.times(
            Duration.ofMillis(0),
            Duration.ofMillis(1000 * 30),
            Duration.ofMillis(0)
        )
    )));
    this.connectingTitles.clear();
    CONFIG.messages.titles.connectingTitles.forEach(title -> this.connectingTitles.add(Title.title(
        SERIALIZER.deserialize(title.title),
        SERIALIZER.deserialize(title.subtitle),
        Title.Times.times(
            Duration.ofMillis(0),
            Duration.ofMillis(1000 * 30),
            Duration.ofMillis(0)
        )
    )));


    this.server.getEventManager().unregisterListeners(this);
    this.server.getEventManager().register(this, new ReconnectListener(this));
    CommandManager commandManager = this.server.getCommandManager();
    commandManager.unregister("limboreconnect");
    commandManager.register(this.commandMeta, new LimboReconnectCommand(this));
  }

  public void addPlayer(Player player, RegisteredServer server) {
    ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
    MinecraftConnection connection = connectedPlayer.getConnection();
    MinecraftSessionHandler minecraftSessionHandler = connection.getActiveSessionHandler();
    if (minecraftSessionHandler != null) {
      if (minecraftSessionHandler instanceof ClientPlaySessionHandler sessionHandler) {
        for (UUID bossBar : sessionHandler.getServerBossBars()) {
          BossBarPacket deletePacket = new BossBarPacket();
          deletePacket.setUuid(bossBar);
          deletePacket.setAction(BossBarPacket.REMOVE);
          connectedPlayer.getConnection().delayedWrite(deletePacket);
        }
        sessionHandler.getServerBossBars().clear();
      }
    }

    connectedPlayer.getTabList().clearAll();
    this.limbo.spawnPlayer(player, new ReconnectHandler(this, server));
  }
}
