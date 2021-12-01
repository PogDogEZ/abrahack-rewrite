package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by the client to request information about the current connection.
 */
@Packet.Info(name="connection_info_request", id=7, side=Packet.Side.CLIENT)
public class ConnInfoRequestPacket extends Packet {

    public ConnInfoRequestPacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
    }
}
