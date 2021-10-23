package me.iska.jclient.network.handler;

import me.iska.jclient.network.Connection;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.packets.ConnectionInfoResponsePacket;
import me.iska.jclient.network.packet.packets.DisconnectPacket;
import me.iska.jclient.network.packet.packets.InputPacket;
import me.iska.jclient.network.packet.packets.KeepAlivePacket;
import me.iska.jclient.network.packet.packets.KeepAliveResponsePacket;
import me.iska.jclient.network.packet.packets.PrintPacket;

public class DefaultHandler implements IHandler {

    private final Connection connection;

    public DefaultHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof DisconnectPacket) {
            connection.exit(((DisconnectPacket)packet).getMessage());

        } else if (packet instanceof ConnectionInfoResponsePacket) {

        } else if (packet instanceof PrintPacket) {

        } else if (packet instanceof InputPacket) {

        } else if (packet instanceof KeepAlivePacket) {
            connection.sendPacket(new KeepAliveResponsePacket(((KeepAlivePacket)packet).getKeepAliveID()));
        }
    }
}
