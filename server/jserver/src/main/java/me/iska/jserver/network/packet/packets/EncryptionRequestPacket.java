package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

@Packet.Info(name="encryption_request", id=1, side= Packet.Side.CLIENT)
public class EncryptionRequestPacket extends Packet {

    private byte[] aPeerPublicKey;
    private int keySize;
    private BigInteger paramG;
    private BigInteger paramP;

    public EncryptionRequestPacket(byte[] aPeerPublicKey, int keySize, BigInteger paramG, BigInteger paramP) {
        this.aPeerPublicKey = aPeerPublicKey.clone();
        this.keySize = keySize;
        this.paramG = paramG;
        this.paramP = paramP;
    }

    public EncryptionRequestPacket() {
        this(new byte[0], 0, BigInteger.ZERO, BigInteger.ZERO);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        aPeerPublicKey = Registry.BYTES.read(inputStream);
        keySize = Registry.UNSIGNED_SHORT.read(inputStream);
        paramG = Registry.VAR_INTEGER.read(inputStream);
        paramP = Registry.VAR_INTEGER.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BYTES.write(aPeerPublicKey, outputStream);
        Registry.UNSIGNED_SHORT.write(keySize, outputStream);
        Registry.VAR_INTEGER.write(paramG, outputStream);
        Registry.VAR_INTEGER.write(paramP, outputStream);
    }

    public byte[] getAPeerPublicKey() {
        return aPeerPublicKey.clone();
    }

    public int getKeySize() {
        return keySize;
    }

    public BigInteger getParamG() {
        return paramG;
    }

    public BigInteger getParamP() {
        return paramP;
    }
}
