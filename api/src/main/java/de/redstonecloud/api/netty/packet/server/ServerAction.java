package de.redstonecloud.api.netty.packet.server;

import de.pierreschwang.nettypacket.Packet;
import de.pierreschwang.nettypacket.buffer.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServerAction extends Packet {
    public static int NETWORK_ID = 0x09;
    protected String action;
    protected String playerUuid;
    protected JSONObject extraData;

    @Override
    public void read(PacketBuffer buffer) {
        this.action = buffer.readUTF8();
        this.playerUuid = buffer.readUTF8();
        this.extraData = new JSONObject(buffer.readUTF8());
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeUTF8(this.action);
        buffer.writeUTF8(this.playerUuid);
        buffer.writeUTF8(this.extraData.toString());
    }
}
