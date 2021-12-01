package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by the server to indicate whether or not it supports the client's capabilities.
 */
@Packet.Info(name="client_capabilities_response", id=4, side= Packet.Side.SERVER)
public class ClientCapabilitiesResponsePacket extends Packet {

    private boolean rejected;

    public ClientCapabilitiesResponsePacket(boolean rejected) {
        this.rejected = rejected;
    }

    public ClientCapabilitiesResponsePacket() {
        this(false);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        rejected = Registry.BOOLEAN.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BOOLEAN.write(rejected, outputStream);
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }
}
