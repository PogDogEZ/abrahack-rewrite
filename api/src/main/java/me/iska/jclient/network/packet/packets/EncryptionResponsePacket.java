package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="encryption_response", id=2, side= Packet.Side.SERVER)
public class EncryptionResponsePacket extends Packet {

    private byte[] bPeerPublicKey;
    private byte[] initVector;

    public EncryptionResponsePacket(byte[] bPeerPublicKey, byte[] initVector) {
        this.bPeerPublicKey = bPeerPublicKey.clone();
        this.initVector = initVector.clone();
    }

    public EncryptionResponsePacket() {
        this(new byte[0], new byte[0]);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        bPeerPublicKey = Registry.BYTES.read(inputStream);
        initVector = Registry.BYTES.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BYTES.write(bPeerPublicKey, outputStream);
        Registry.BYTES.write(initVector, outputStream);
    }

    public byte[] getBPeerPublicKey() {
        return bPeerPublicKey.clone();
    }

    public byte[] getInitVector() {
        return initVector.clone();
    }
}
