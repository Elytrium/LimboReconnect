package ru.skywatcher_2019.limboreconnect;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(
        id = "LimboReconnect",
        name = "LimboReconnect",
        version = BuildConstants.VERSION,
        authors = {"SkyWatcher_2019"}
)
public class LimboReconnect {

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }
}
