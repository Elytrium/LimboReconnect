package ru.skywatcher_2019.limboreconnect;

import net.elytrium.java.commons.config.YamlConfig;

public class Config extends YamlConfig {
    @Ignore
    public static final Config IMP = new Config();

    @Comment("Send player to the limbo ok kick, if kick message contains this text")
    public String RESTART_MESSAGE = "Server is restarting";

    @Comment("Server status check interval in seconds")
    public long CHECK_INTERVAL = 1;
    @Comment("Server that sould be checked for restarts")
    public String TARGET_SERVER = "survival";

    @Create
    public MESSAGES MESSAGES;

    public static class MESSAGES {
        @Comment("Serializers: LEGACY_AMPERSAND, LEGACY_SECTION, MINIMESSAGE")
        public String SERIALIZER = "MINIMESSAGE";
        public String OFFLINE_SERVER_MESSAGE = "Server is restarting, please wait...";
    }
}
