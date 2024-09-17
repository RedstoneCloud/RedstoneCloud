package de.redstonecloud.api.netty.packet;

import de.pierreschwang.nettypacket.Packet;
import de.pierreschwang.nettypacket.buffer.PacketBuffer;
import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import de.redstonecloud.api.encryption.KeyManager;
import de.redstonecloud.api.encryption.cache.KeyCache;
import de.redstonecloud.api.netty.NettyHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import java.security.PublicKey;
import java.util.Arrays;

public class EncryptedPacket extends Packet {
    public static int NETWORK_ID = -999;

    protected String recipientId;
    protected Packet packet;

    public EncryptedPacket(String recipientId, Packet packet) {
        this.recipientId = recipientId;

        if (packet instanceof EncryptedPacket) {
            throw new RuntimeException("Cannot store an encrypted packet inside another encrypted packet");
        }

        this.packet = packet;
    }

    @Override
    public void read(PacketBuffer buffer) {
        int packetId = buffer.readInt();
        long sessionId = buffer.readLong();

        IPacketRegistry packetRegistry = NettyHelper.constructRegistry();
        if (!packetRegistry.containsPacketId(packetId)) {
            throw new DecoderException("Received invalid packet id");
        }

        PublicKey publicKey = KeyManager.getPublicKey();

        while (buffer.isReadable()) {
            int keyLength = buffer.readInt();
            byte[] encodedKey = buffer.readBytes(keyLength).array();

            int packetBufferLength = buffer.readInt();
            ByteBuf packetBuf = buffer.readBytes(packetBufferLength);

            if (!Arrays.equals(publicKey.getEncoded(), encodedKey)) {
                continue;
            }

            byte[] encryptedBuffer = packetBuf.array();
            PacketBuffer packetBuffer = this.decryptBuffer(encryptedBuffer);

            try {
                this.packet = packetRegistry.constructPacket(packetId);
                this.packet.setSessionId(sessionId);
                this.packet.read(packetBuffer);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            break;
        }
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeInt(NettyHelper.constructRegistry().getPacketId(this.packet.getClass()));
        buffer.writeLong(this.packet.getSessionId());

        for (PublicKey key : KeyCache.Holder.getCache().getKeys(this.recipientId)) {
            byte[] encodedKey = key.getEncoded();
            buffer.writeInt(encodedKey.length);
            buffer.writeBytes(key.getEncoded());

            PacketBuffer packetBuffer = new PacketBuffer();
            this.packet.write(packetBuffer);

            byte[] encryptedBuffer = this.encryptBuffer(key, packetBuffer);
            buffer.writeInt(encryptedBuffer.length);
            buffer.writeBytes(encryptedBuffer);
        }
    }

    protected byte[] encryptBuffer(PublicKey key, PacketBuffer packetBuffer) {
        byte[] arr = packetBuffer.array();
        return KeyManager.encrypt(arr, key);
    }

    protected PacketBuffer decryptBuffer(byte[] arr) {
        byte[] bufferArray = KeyManager.decrypt(arr);
        return new PacketBuffer(Unpooled.wrappedBuffer(arr));
    }
}
