package ru.skywatcher_2019.limboreconnect;

import net.elytrium.java.commons.config.YamlConfig;

public class Config extends YamlConfig {
    @Ignore
    public static final Config IMP = new Config();

    @Comment("Send player to the limbo ok kick, if kick message contains this text")
    public String RESTART_MESSAGE = "Server is restarting";
}
