package me.iska.jserver.network.handler.handlers;

import me.iska.jserver.JServer;
import me.iska.jserver.network.Connection;
import me.iska.jserver.network.handler.IHandler;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.packets.*;

import java.util.logging.Logger;

public class DefaultHandler implements IHandler {

    private final JServer jServer = JServer.getInstance();
    private final Logger logger = JServer.getLogger();

    private final Connection connection;

    private long lastKeepAliveTime;
    private long keepAliveAwait;
    private int ping;

    public DefaultHandler(Connection connection) {
        this.connection = connection;

        lastKeepAliveTime = System.currentTimeMillis();
        keepAliveAwait = -1L;
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof DisconnectPacket) {
            connection.exit(((DisconnectPacket)packet).getMessage());
            return;
        }

        if (packet instanceof ConnInfoRequestPacket) {
            connection.sendPacket(new ConnInfoResponsePacket(ping, jServer.connectionManager.getUser(connection),
                    connection.getHost(), connection.getPort()));

        } else if (packet instanceof KeepAliveResponsePacket) {
            KeepAliveResponsePacket keepAliveResponse = (KeepAliveResponsePacket)packet;

            if (keepAliveAwait != -1L && keepAliveResponse.getKeepAliveID() == keepAliveAwait) {
                ping = (int)(System.currentTimeMillis() - keepAliveAwait) / 2;
                keepAliveAwait = -1L;

                logger.finer(String.format("%s ping: %dms.", connection, ping));
            }
        }
    }

    @Override
    public void update() {
        if (System.currentTimeMillis() - lastKeepAliveTime > 5000) { // TODO: Configurable
            if (keepAliveAwait == -1L) {
                lastKeepAliveTime = System.currentTimeMillis();
                keepAliveAwait = System.currentTimeMillis();
                connection.sendPacket(new KeepAlivePacket(keepAliveAwait));
            } else if (System.currentTimeMillis() - keepAliveAwait > 30000) {
                connection.exit("Keep alive timeout.");
            }
        }
    }

    @Override
    public void exit(String reason) {
        connection.sendPacket(new DisconnectPacket(reason));
    }
}
