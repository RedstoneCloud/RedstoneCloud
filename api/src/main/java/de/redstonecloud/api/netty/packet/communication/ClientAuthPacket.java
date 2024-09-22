package de.redstonecloud.api.netty.packet.communication;

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
public class ClientAuthPacket extends Packet {
    public static int NETWORK_ID = 0x00;

    protected String clientId;

    @Override
    public void read(PacketBuffer buffer) {
        this.clientId = buffer.readUTF8();
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeUTF8(this.clientId);
    }
}
