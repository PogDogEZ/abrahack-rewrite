package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.EncryptionType;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is sent by the server when the client first attempts to connect, it contains information that the client will
 * need to connect to the server.
 */
@SuppressWarnings("FieldCanBeLocal")
@Packet.Info(name="server_info", id=0, side=Packet.Side.SERVER)
public class ServerInfoPacket extends Packet {

    private final EnumType<EncryptionType> ENCRYPTION_TYPE = new EnumType<>(EncryptionType.class);

    private final int COMPRESSION_BIT = 1;
    private final int ENCRYPTION_BIT = 2;
    private final int AUTHENTICATION_BIT = 3;

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

    public ServerInfoPacket(String serverName, int protocolVer, EncryptionType encryptionType, int compressionThreshold) {
        this(serverName, protocolVer, encryptionType, compressionThreshold, (byte)0);
    }

    public ServerInfoPacket() {
        this("", 4, EncryptionType.NONE, 0, (byte)0);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        serverName = Registry.STRING.read(inputStream);
        protocolVer = Registry.UNSIGNED_SHORT.read(inputStream);
        encryptionType = ENCRYPTION_TYPE.read(inputStream);
        compressionThreshold = Registry.INTEGER.read(inputStream);
        byte[] bitmaskBytes = new byte[1];
        inputStream.read(bitmaskBytes);
        bitmask = bitmaskBytes[0];
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.STRING.write(serverName, outputStream);
        Registry.UNSIGNED_SHORT.write(protocolVer, outputStream);
        ENCRYPTION_TYPE.write(encryptionType, outputStream);
        Registry.INTEGER.write(compressionThreshold, outputStream);
        outputStream.write(new byte[] { bitmask });
    }

    private boolean isBitSet(int bit) {
        return (bitmask & (int)Math.pow(2, bit - 1)) == (int)Math.pow(2, bit - 1);
    }

    private void setBit(int bit, boolean value) {
        if (value) {
            bitmask |= (int)Math.pow(2, bit - 1);
        } else {
            bitmask &= ~(int)Math.pow(2, bit - 1);
        }
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getProtocolVer() {
        return protocolVer;
    }

    public void setProtocolVer(int protocolVer) {
        this.protocolVer = protocolVer;
    }

    public EncryptionType getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(EncryptionType encryptionType) {
        this.encryptionType = encryptionType;
    }

    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    public void setCompressionThreshold(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    public boolean isCompressionEnabled() {
        return isBitSet(COMPRESSION_BIT);
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        setBit(COMPRESSION_BIT, compressionEnabled);
    }

    public boolean isEncryptionEnabled() {
        return isBitSet(ENCRYPTION_BIT);
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        setBit(ENCRYPTION_BIT, encryptionEnabled);
    }

    public boolean isAuthenticationEnabled() {
        return isBitSet(AUTHENTICATION_BIT);
    }

    public void setAuthenticationEnabled(boolean authenticationEnabled) {
        setBit(AUTHENTICATION_BIT, authenticationEnabled);
    }
}
