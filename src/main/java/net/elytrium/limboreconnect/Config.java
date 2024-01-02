/*
 * Copyright (C) 2022 - 2024 Elytrium
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
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.limboapi.api.file.BuiltInWorldFileType;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public class Config extends YamlSerializable {

  private static final SerializerConfig CONFIG = new SerializerConfig.Builder().setCommentValueIndent(1).build();


  @Comment(value = {
      @CommentValue("Available serializers:"),
      @CommentValue("LEGACY_AMPERSAND - \"&c&lExample &c&9Text\"."),
      @CommentValue("LEGACY_SECTION - \"§c§lExample §c§9Text\"."),
      @CommentValue("MINIMESSAGE - \"<bold><red>Example</red> <blue>Text</blue></bold>\". (https://webui.adventure.kyori.net/)"),
      @CommentValue("GSON - \"[{\"text\":\"Example\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \",\"bold\":true},{\"text\":\"Text\",\"bold\":true,\"color\":\"blue\"}]\". (https://minecraft.tools/en/json_text.php/)"),
      @CommentValue("GSON_COLOR_DOWNSAMPLING - Same as GSON, but uses downsampling.")
  })
  public Serializers serializer = Serializers.MINIMESSAGE;

  @Comment(value = @CommentValue("Send player to the limbo, if disconnect reason contains this text (using regex)"))
  public String triggerMessage
      = "((?i)^(server closed|server is restarting|multiplayer\\.disconnect\\.server_shutdown))+$";

  @Comment(value = @CommentValue("Server status check interval in milliseconds"))
  public long checkInterval = 1000;
  @Comment(value = @CommentValue("Server status check timeout in milliseconds"))
  public long pingTimeout = 500;
  @Comment(value = @CommentValue("Reconnect delay after server startup"))
  public long joinDelay = 2000;
  @Comment(value = @CommentValue("Send to limbo or use current server's world"))
  public boolean useLimbo = false;
  @Comment(value = @CommentValue("Require permission to reconnect (limboreconnect.reconnect)"))
  public boolean requirePermission = false;

  public World world;
  public Messages messages;
  public boolean debug = false;


  public Config() {
    super(Config.CONFIG);
    this.world = new World();
    this.messages = new Messages();
  }


  public static class World {

    @Comment(value = @CommentValue("Dimensions: OVERWORLD, NETHER, THE_END"))
    public String dimension = "OVERWORLD";
    @Comment(value = @CommentValue("Load world from file"))
    public boolean loadWorld = false;
    @Comment(value = @CommentValue("Schematic file name"))
    public String worldFilePath = "world.schem";
    public BuiltInWorldFileType worldFileType = BuiltInWorldFileType.WORLDEDIT_SCHEM;
    public int worldLightLevel = 15;
    public GameMode gamemode = GameMode.ADVENTURE;


    public WorldCoords worldCoords;
    public PlayerCoords playerCoords;

    public World() {
      this.worldCoords = new WorldCoords();
      this.playerCoords = new PlayerCoords();
    }

    public static class WorldCoords {

      public int x = 0;
      public int y = 100;
      public int z = 0;

      public WorldCoords() {
      }
    }

    public static class PlayerCoords {

      public int x = 0;
      public int y = 100;
      public int z = 0;
      public float pitch = 0;
      public float yaw = 90;

      public PlayerCoords() {
      }
    }
  }

  public static class Messages {

    public String reload = "<green>LimboReconnect reloaded";
    public Titles titles;

    public Messages() {
      this.titles = new Titles();
    }

    public static class Titles {

      @Comment(value = @CommentValue("time in ticks"), at = Comment.At.SAME_LINE)
      public long showDelay = 20;

      public List<Title> titles = List.of(
          new Title("Server is restarting."), new Title("Server is restarting.."), new Title("Server is restarting...")
      );
      public List<Title> connectingTitles = List.of(
          new Title("Connecting."), new Title("Connecting.."), new Title("Connecting...")
      );
    }

    public static class Title {

      public String title;
      public String subtitle = "Please wait...";

      public Title() {
      }

      public Title(String title) {
        this.title = title;
      }
    }
  }
}
