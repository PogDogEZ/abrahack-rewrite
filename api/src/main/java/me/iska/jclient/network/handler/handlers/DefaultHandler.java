package me.iska.jclient.network.handler.handlers;

import me.iska.jclient.network.Connection;
import me.iska.jclient.network.handler.IHandler;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.packets.DisconnectPacket;
import me.iska.jclient.network.packet.packets.KeepAlivePacket;
import me.iska.jclient.network.packet.packets.KeepAliveResponsePacket;

import java.util.logging.Logger;

public class DefaultHandler implements IHandler {

    private final Logger logger = Connection.getLogger();

    private final Connection connection;

    public DefaultHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof DisconnectPacket) {
            connection.exit(((DisconnectPacket)packet).getMessage());

        } else if (packet instanceof KeepAlivePacket) {
            KeepAlivePacket keepAlive = (KeepAlivePacket)packet;

            // logger.fine(String.format("Latest keepalive: %s.", keepAlive.getKeepAliveID()));
            connection.sendPacket(new KeepAliveResponsePacket(keepAlive.getKeepAliveID()));
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void exit(String reason) {
        connection.sendPacket(new DisconnectPacket(reason));
    }
}
