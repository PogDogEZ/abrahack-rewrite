package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

@Packet.Info(name="encryption_request", id=1, side=Packet.Side.CLIENT)
public class EncryptionRequestPacket extends Packet { // TODO: StartEncryptionPacket

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

    @Override
    public void read(InputStream inputStream) throws IOException {
        aPeerPublicKey = Registry.BYTES.read(inputStream);
        keySize = Registry.UNSIGNED_SHORT.read(inputStream);
        paramG = Registry.VARINT.read(inputStream);
        paramP = Registry.VARINT.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BYTES.write(aPeerPublicKey, outputStream);
        Registry.UNSIGNED_SHORT.write(keySize, outputStream);
        Registry.VARINT.write(paramG, outputStream);
        Registry.VARINT.write(paramP, outputStream);
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
