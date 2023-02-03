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

import net.elytrium.commons.config.YamlConfig;
import net.elytrium.commons.kyori.serialization.Serializers;

public class Config extends YamlConfig {

  @Ignore
  public static final Config IMP = new Config();

  @Comment({
     "Available serializers:",
     "LEGACY_AMPERSAND - \"&c&lExample &c&9Text\".",
     "LEGACY_SECTION - \"§c§lExample §c§9Text\".",
     "MINIMESSAGE - \"<bold><red>Example</red> <blue>Text</blue></bold>\". (https://webui.adventure.kyori.net/)",
     "GSON - \"[{\"text\":\"Example\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \",\"bold\":true},{\"text\":\"Text\",\"bold\":true,\"color\":\"blue\"}]\". (https://minecraft.tools/en/json_text.php/)",
     "GSON_COLOR_DOWNSAMPLING - Same as GSON, but uses downsampling."
  })
  public Serializers SERIALIZER = Serializers.MINIMESSAGE;

  @Comment("Send player to the limbo, if disconnect reason contains this text (using regex)")
  public String RESTART_MESSAGE = "((?i)^(server closed|multiplayer\\.disconnect\\.server_shutdown|server is restarting))+$";

  @Comment("Server status check interval in milliseconds")
  public long CHECK_INTERVAL = 1000;
  @Comment("Server status check timeout in milliseconds")
  public long PING_TIMEOUT = 500;

  @Create
    public Config.MAIN.WORLD WORLD;

    public static class WORLD {

      @Comment(
          "Dimensions: OVERWORLD, NETHER, THE_END"
      )
      public String DIMENSION = "OVERWORLD";
    }

  }

  @Create
  public TITLE TITLE;

  public static class TITLE {
    @Comment(value = "time in ticks", at = Comment.At.SAME_LINE)
    public int FADE_IN = 10;
    @Comment(value = "time in ticks", at = Comment.At.SAME_LINE)
    public int FADE_OUT = 20;
  }

  @Create
  public MESSAGES MESSAGES;

  @Comment("Empty messages will not be sent")
  public static class MESSAGES {

    public String SERVER_OFFLINE = "<red>Server is restarting, please wait...";
    public String CONNECTING = "<aqua>Connecting to the server...";

    @Create
    public TITLE TITLE;

    public static class TITLE {

      public String OFFLINE_TITLE = "";
      public String OFFLINE_SUBTITLE = "<red>Server is restarting, please wait...";

      public String CONNECTING_TITLE = "";
      public String CONNECTING_SUBTITLE = "<aqua>Connecting to the server...";
    }
  }
}
