package me.iska.jserver.network;

import me.iska.jserver.JServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class Server extends Thread {

    private final JServer jServer = JServer.getInstance();
    private final Logger logger = JServer.getLogger();

    private final ServerSocket sock;

    private boolean exited;

    public Server(ServerSocket sock) {
        this.sock = sock;

        exited = false;
    }

    @Override
    public void run() {
        while (!exited) {
            try {
                Socket newSock = sock.accept();
                logger.info(String.format("New connection from %s:%d.", newSock.getInetAddress().getHostAddress(), newSock.getPort()));
                // TODO: Unique logger names
                Connection newConnection = new Connection(newSock.getInetAddress().getHostAddress(), newSock.getPort());
                jServer.connectionManager.addConnection(newConnection);
                newConnection.accept(newSock);

            } catch (IOException error) {
                logger.warning("Error while accepting connection:");
                logger.throwing(Server.class.getSimpleName(), "run", error);
            }
        }
    }

    public void bind(String host, int port) throws IOException {
        sock.bind(new InetSocketAddress(host, port));
        sock.setSoTimeout(0);
        start();
    }

    public void exit() {
        exited = true;
        try {
            sock.close();
        } catch (IOException error) {
            logger.warning("Error while shutting down server:");
            logger.throwing(Server.class.getSimpleName(), "exit", error);
        }
        stop();
    }

    public boolean isExited() {
        return exited;
    }
}
