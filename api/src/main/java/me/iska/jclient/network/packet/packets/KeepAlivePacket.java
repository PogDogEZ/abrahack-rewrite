package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

@Packet.Info(name="keep_alive", id=12, side=Packet.Side.SERVER)
public class KeepAlivePacket extends Packet {

    private long keepAliveID;

    public KeepAlivePacket(long keepAliveID) {
        this.keepAliveID = keepAliveID;
    }

    public KeepAlivePacket() {
        this(0L);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        keepAliveID = Registry.VARINT.read(inputStream).longValue();
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.VARINT.write(BigInteger.valueOf(keepAliveID), outputStream);
    }

    public long getKeepAliveID() {
        return keepAliveID;
    }

    public void setKeepAliveID(long keepAliveID) {
        this.keepAliveID = keepAliveID;
    }
}
