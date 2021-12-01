package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

/**
 * Sent by the server periodically to keep the connection alive.
 */
@Packet.Info(name="keep_alive", id=12, side= Packet.Side.SERVER)
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
        keepAliveID = Registry.VAR_INTEGER.read(inputStream).longValue();
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.VAR_INTEGER.write(BigInteger.valueOf(keepAliveID), outputStream);
    }

    public long getKeepAliveID() {
        return keepAliveID;
    }

    public void setKeepAliveID(long keepAliveID) {
        this.keepAliveID = keepAliveID;
    }
}
