package de.redstonecloud.api.netty.packet.server;

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
public class ServerChangeStatusPacket extends Packet {
    public static int NETWORK_ID = 0x0A;
    protected String server;
    protected String newStatus;

    @Override
    public void read(PacketBuffer buffer) {
        this.server = buffer.readUTF8();
        this.newStatus = buffer.readUTF8();
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeUTF8(this.server);
        buffer.writeUTF8(this.newStatus);
    }
}
