package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="connection_info_request", id=7, side= Packet.Side.CLIENT)
public class ConnectionInfoRequestPacket extends Packet {

    public ConnectionInfoRequestPacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
    }
}
