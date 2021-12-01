package me.iska.jserver.managers;

import me.iska.jserver.IManager;
import me.iska.jserver.JServer;
import me.iska.jserver.exception.AuthenticationException;
import me.iska.jserver.network.Connection;
import me.iska.jserver.network.EncryptionType;
import me.iska.jserver.network.Server;
import me.iska.jserver.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ConnectionManager implements IManager {

    private final Logger logger = JServer.getLogger();

    private final Map<Connection, User> connections = new HashMap<>();
    private final List<Server> servers = new ArrayList<>();

    private final String host;
    private final int port;

    private boolean compressionEnabled;
    private int compressionThreshold;

    private boolean encryptionEnabled;
    private EncryptionType encryptionType;

    private boolean authenticationEnabled;

    public ConnectionManager(String host, int port) {
        this.host = host;
        this.port = port;

        compressionEnabled = true;
        compressionThreshold = 256;

        encryptionEnabled = true;
        encryptionType = EncryptionType.AES256;

        authenticationEnabled = false;
    }

    @Override
    public void update() {
    }

    @Override
    public void exit() {
        logger.fine("Exiting connection manager.");
        logger.finer("Closing all connections.");
        connections.forEach((connection, user) -> connection.exit("Server shutdown."));
        connections.clear();
        logger.finer("Closing all servers.");
        servers.forEach(Server::exit);
        servers.clear();
        logger.fine("Done.");
    }

    /* ----------------------------- Server stuff ----------------------------- */

    public void addServer(Server server) {
        servers.add(server);
    }

    public void removeServer(Server server) {
        servers.remove(server);
        if (!server.isExited()) server.exit();
    }

    /* ----------------------------- Connection stuff ----------------------------- */

    public void addConnection(Connection connection) {
        if (!connections.containsKey(connection)) {
            logger.finer(String.format("Adding connection %s.", connection));
            connections.put(connection, null);
        }
    }

    public void removeConnection(Connection connection) {
        if (connections.containsKey(connection)) {
            logger.finer(String.format("Removing connection %s.", connection));
            connections.remove(connection);
            if (!connection.isExited()) connection.exit();
        }
    }

    public User getUser(Connection connection) {
        return connections.get(connection);
    }

    public void setUser(Connection connection, User user) throws AuthenticationException {
        if (!user.isLoggedIn()) throw new IllegalStateException("User is not logged in.");

        if (connections.containsKey(connection)) {
            if (connections.get(connection) != null) connections.get(connection).getGroup().logout(connections.get(connection));
            connections.put(connection, user);
        }
    }

    /* ----------------------------- Setters and getters ----------------------------- */

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    public void setCompressionThreshold(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public EncryptionType getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(EncryptionType encryptionType) {
        this.encryptionType = encryptionType;
    }

    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public void setAuthenticationEnabled(boolean authenticationEnabled) {
        this.authenticationEnabled = authenticationEnabled;
    }
}
