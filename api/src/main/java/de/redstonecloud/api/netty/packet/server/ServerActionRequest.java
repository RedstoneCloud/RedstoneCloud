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
public class ServerActionRequest extends Packet {
    public static int NETWORK_ID = 0x08;
    protected String server;
    protected String playerUuid;
    protected String action;
    protected JSONObject extraData;

    @Override
    public void read(PacketBuffer buffer) {
        this.server = buffer.readUTF8();
        this.action = buffer.readUTF8();
        this.playerUuid = buffer.readUTF8();
        this.extraData = new JSONObject(buffer.readUTF8());
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeUTF8(this.server);
        buffer.writeUTF8(this.action);
        buffer.writeUTF8(this.playerUuid);
        buffer.writeUTF8(this.extraData.toString());
    }

    public ServerAction toActionPacket() {
        return new ServerAction().setAction(action).setPlayerUuid(playerUuid).setExtraData(extraData);
    }
}
