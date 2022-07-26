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
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.elytrium.java.commons.mc.serialization.Serializer;
import net.elytrium.java.commons.mc.serialization.Serializers;
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

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "limboreconnect",
        name = "LimboReconnect",
        version = BuildConstants.VERSION,
        authors = {"SkyWatcher_2019"}
)
public class LimboReconnect {

    @Inject
    private static Logger LOGGER;
    private static Serializer SERIALIZER;
    private final ProxyServer server;
    private final File configFile;
    private final LimboFactory factory;
    public HashSet<LimboPlayer> players = new HashSet<>();
    private Limbo limbo;
    private long checkInterval;
    private RegisteredServer targetServer;
    private ScheduledTask limboTask;
    private Component offlineServerMessage;

    @Inject
    public LimboReconnect(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        setLogger(logger);

        this.server = server;

        File dataDirectoryFile = dataDirectory.toFile();
        this.configFile = new File(dataDirectoryFile, "config.yml");

        this.factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
    }

    private static void setSerializer(Serializer serializer) {
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

        ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(Config.IMP.MESSAGES.SERIALIZER.toUpperCase(Locale.ROOT)).getSerializer();
        if (serializer == null) {
            LOGGER.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
            setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
        } else {
            setSerializer(new Serializer(serializer));
        }

        this.checkInterval = Config.IMP.CHECK_INTERVAL;
        VirtualWorld world = this.factory.createVirtualWorld(Dimension.OVERWORLD, 0, 100, 0, (float) 90, (float) 0.0);
        this.limbo = this.factory.createLimbo(world).setName("LimboReconnect").setWorldTime(6000);

        Optional<RegisteredServer> server = this.getServer().getServer(Config.IMP.TARGET_SERVER);
        server.ifPresent(registeredServer -> this.targetServer = registeredServer);

        this.offlineServerMessage = SERIALIZER.deserialize(Config.IMP.MESSAGES.OFFLINE_SERVER_MESSAGE);

        this.server.getEventManager().register(this, new ReconnectListener(this));

        startTask();
    }

    private ProxyServer getServer() {
        return this.server;
    }

    public void addPlayer(Player player) {
        this.limbo.spawnPlayer(player, new ReconnectHandler(this));
    }

    private void startTask() {
        if (this.limboTask != null) this.limboTask.cancel();
        this.limboTask = this.getServer().getScheduler().buildTask(this, () -> {
            try {
                ServerPing serverPing = this.targetServer.ping().get();
                if (serverPing.getPlayers().isPresent()) {
                    this.players.forEach(p -> p.disconnect(this.targetServer));
                }
            } catch (InterruptedException | ExecutionException e) {
                this.players.forEach(p -> p.getProxyPlayer().sendMessage(this.offlineServerMessage));
            }
        }).repeat(checkInterval, TimeUnit.SECONDS).schedule();
    }
}
