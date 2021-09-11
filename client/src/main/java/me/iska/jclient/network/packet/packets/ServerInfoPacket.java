package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.enums.EncryptionType;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="server_info", id=0, side=Packet.Side.SERVER)
public class ServerInfoPacket extends Packet {

    private String serverName;
    private int protocolVer;
    private EncryptionType encryptionType;
    private int compressionThreshold;
    private byte bitmask;

    public ServerInfoPacket(String serverName, int protocolVer, EncryptionType encryptionType, int compressionThreshold,
                            byte bitmask) {
        this.serverName = serverName;
        this.protocolVer = protocolVer;
        this.encryptionType = encryptionType;
        this.compressionThreshold = compressionThreshold;
        this.bitmask = bitmask;
    }

    public ServerInfoPacket() {
        this("bruh", 4, EncryptionType.NONE, 0, (byte)0);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        serverName = Registry.STRING.read(inputStream);
        protocolVer = Registry.UNSIGNED_SHORT.read(inputStream);
        encryptionType = new EnumType<>(EncryptionType.class).read(inputStream);
        compressionThreshold = Registry.INT.read(inputStream);
        byte[] bitmaskBytes = new byte[1];
        inputStream.read(bitmaskBytes);
        bitmask = bitmaskBytes[0];
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.STRING.write(serverName, outputStream);
        Registry.UNSIGNED_SHORT.write(protocolVer, outputStream);
        new EnumType<>(EncryptionType.class).write(encryptionType, outputStream);
        Registry.INT.write(compressionThreshold, outputStream);
        outputStream.write(new byte[] { bitmask });
    }

    private boolean isBitSet(int bit) {
        return (bitmask & (int)Math.pow(2, bit - 1)) == (int)Math.pow(2, bit - 1);
    }

    private void setBit(int bit) {
        bitmask |= (int)Math.pow(2, bit - 1);
    }

    private void setCompressionEnabled() {
        setBit(1);
    }

    private void setEncryptionEnabled() {
        setBit(2);
    }

    public String getServerName() {
        return serverName;
    }

    public int getProtocolVer() {
        return protocolVer;
    }

    public EncryptionType getEncryptionType() {
        return encryptionType;
    }

    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    public boolean isCompressionEnabled() {
        return isBitSet(1);
    }

    public boolean isEncryptionEnabled() {
        return isBitSet(2);
    }
}
