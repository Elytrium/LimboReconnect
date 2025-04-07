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

package net.elytrium.limboreconnect.protocol.packets;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class PlaySound implements MinecraftPacket {

  private final String soundName;
  private final float volume;
  private final float pitch;
  private int playerX;
  private int playerY;
  private int playerZ;

  public PlaySound(String soundName, double x, double y, double z, float volume, float pitch) {
    this.soundName = soundName;
    this.setPosition(x, y, z);
    this.volume = volume;
    this.pitch = pitch;
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
      ProtocolUtils.writeVarInt(byteBuf, 0);
      ProtocolUtils.writeString(byteBuf, this.soundName);
      byteBuf.writeBoolean(false);
    } else {
      ProtocolUtils.writeString(byteBuf, this.soundName);
    }
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
      ProtocolUtils.writeVarInt(byteBuf, 0);
    }
    byteBuf.writeInt(this.playerX);
    byteBuf.writeInt(this.playerY);
    byteBuf.writeInt(this.playerZ);
    byteBuf.writeFloat(this.volume);
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_10)) {
      byteBuf.writeFloat(this.pitch);
    } else {
      byteBuf.writeByte((int) (this.pitch * 63.5F));
    }
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      byteBuf.writeLong(0);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
    return false;
  }

  public void setPosition(double x, double y, double z) {
    this.playerX = (int) (x * 8);
    this.playerY = (int) (y * 8);
    this.playerZ = (int) (z * 8);
  }
}
