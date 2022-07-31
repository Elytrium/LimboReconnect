/*
 * Copyright (C) 2022 - 2022 SkyWatcher_2019
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
