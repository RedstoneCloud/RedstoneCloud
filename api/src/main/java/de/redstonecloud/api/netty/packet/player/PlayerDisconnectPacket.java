package de.redstonecloud.api.netty.packet.player;

import de.pierreschwang.nettypacket.Packet;
import de.pierreschwang.nettypacket.buffer.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDisconnectPacket extends Packet {
    public static int NETWORK_ID = 0x06;

    protected String uuid;
    protected String server;

    @Override
    public void read(PacketBuffer buffer) {
        this.uuid = buffer.readUTF8();
        this.server = buffer.readUTF8();
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeUTF8(this.uuid);
        buffer.writeUTF8(this.server);
    }
}
