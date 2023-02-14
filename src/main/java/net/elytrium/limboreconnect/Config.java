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

import java.util.List;
import net.elytrium.commons.config.YamlConfig;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.limboapi.api.file.BuiltInWorldFileType;

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
  @Comment("Send to limbo or use current server's world")
  public boolean USE_LIMBO = false;

  @Create
  public WORLD WORLD;
  @Create
  public MESSAGES MESSAGES;

  public static class WORLD {

    @Comment(
        "Dimensions: OVERWORLD, NETHER, THE_END"
    )
    public String DIMENSION = "OVERWORLD";
    @Comment(
        "Available: ADVENTURE, CREATIVE, SURVIVAL, SPECTATOR"
    )
    public String GAME_MODE = "ADVENTURE";
    public boolean LOAD_WORLD = false;
    public String WORLD_FILE_PATH = "world.schem";
    public BuiltInWorldFileType WORLD_FILE_TYPE = BuiltInWorldFileType.WORLDEDIT_SCHEM;
    public int WORLD_LIGHT_LEVEL = 15;

    @Create
    public WORLD_COORDS WORLD_COORDS;

    public static class WORLD_COORDS {

      public int X = 0;
      public int Y = 0;
      public int Z = 0;
    }
  }

  @Comment("Empty messages will not be sent")
  public static class MESSAGES {

    public String CONNECTING = "<aqua>Connecting to the server...";
    public String RELOAD = "<green>LimboReconnect reloaded";

    @Create
    public RESTART_MESSAGES_SETTINGS RESTART_MESSAGES_SETTINGS;

    public static class RESTART_MESSAGES_SETTINGS {

      @Comment(value = "time in ticks", at = Comment.At.SAME_LINE)
      public long SHOW_DELAY = 20;
    }

    public List<RESTART_MESSAGES> RESTART_MESSAGES = List.of(new RESTART_MESSAGES(), new RESTART_MESSAGES(), new RESTART_MESSAGES());

    public static class RESTART_MESSAGES {

      public String TITLE = "Server is restarting...";
      public String SUBTITLE = "Please vait...";
      public String ACTIONBAR = "";
    }
  }
}
