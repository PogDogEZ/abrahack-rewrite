package me.iska.jclient.network;

import me.iska.jclient.network.handler.IHandler;
import me.iska.jclient.network.handler.handlers.LoginHandler;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.user.User;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Connection extends Thread {

    private static final Logger logger = Logger.getLogger("jclient");

    public static Logger getLogger() {
        return logger;
    }

    private final List<Class<? extends Packet>> capabilities = new ArrayList<>();

    private final List<Packet> latestRecv = new ArrayList<>();
    private final List<Packet> sendQueue = new ArrayList<>();

    private final List<IHandler> secondaryHandlers = new ArrayList<>();

    private final String host;
    private final int port;

    private final ServerInfo serverInfo;

    private Socket sock;
    private InputStream inputStream;
    private OutputStream outputStream;

    private IHandler primaryHandler;

    private boolean connected;
    private long lastPacket;

    private Supplier<Account> accountSupplier;
    private User serverUser;

    private boolean exited;
    private boolean shouldExit;
    private String exitReason;

    public Connection(String host, int port) {
        this.host = host;
        this.port = port;

        capabilities.addAll(Registry.KNOWN_PACKETS);

        serverInfo = new ServerInfo("", false, EncryptionType.NONE, true, 1024, true);

        connected = false;
        lastPacket = System.currentTimeMillis();

        exited = false;
        shouldExit = false;
        exitReason = "Generic disconnect.";
    }

    @Override
    public String toString() {
        return String.format("Connection(host=%s, port=%d)", host, port);
    }

    @Override
    @SuppressWarnings("BusyWait")
    public void run() {
        while (connected) {
            try {
                primaryHandler.update();
                new ArrayList<>(secondaryHandlers).forEach(IHandler::update);
            } catch (Exception error) {
                logger.warning("Error while updating handlers:");
                logger.throwing(Connection.class.getSimpleName(), "run", error);
                exit(error.toString());
            }

            doReadPackets();
            doSendPackets();

            if (System.currentTimeMillis() - lastPacket > 30000) exit("Timed out.");

            if (shouldExit) { // We still want to read and write packets while exiting
                try {
                    primaryHandler.exit(exitReason);
                    new ArrayList<>(secondaryHandlers).forEach(handler -> handler.exit(exitReason));
                } catch (Exception error) {
                    logger.warning("Error while handling exit:");
                    logger.throwing(Connection.class.getSimpleName(), "run", error);
                }

                if (!sendQueue.isEmpty() && connected) { // Don't send if we aren't still connected
                    try {
                        doSendPackets();
                        Thread.sleep(1000);
                    } catch (Exception ignored) {
                    }
                }

                logger.info(String.format("%s disconnected, reason: %s", this, exitReason));

                try {
                    sock.close();
                } catch (IOException error) {
                    logger.warning("Error while closing socket:");
                    logger.throwing(Connection.class.getSimpleName(), "run", error);
                }

                connected = false;
                exited = true;
            }

            /*
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
             */
        }
    }

    /* ----------------------------- Handlers and connection stuff ----------------------------- */

    public IHandler getPrimaryHandler() {
        return primaryHandler;
    }

    public void setPrimaryHandler(IHandler primaryHandler) {
        this.primaryHandler = primaryHandler;
    }

    public void addSecondaryHandler(IHandler handler) {
        if (!secondaryHandlers.contains(handler)) secondaryHandlers.add(handler);
    }

    public void removeSecondaryHandler(IHandler handler) {
        secondaryHandlers.remove(handler);
    }

    public void setAccountSupplier(Supplier<Account> accountSupplier) {
        this.accountSupplier = accountSupplier;
    }

    public void connect(Socket sock) throws IOException {
        this.sock = sock;
        this.sock.connect(new InetSocketAddress(host, port));

        connected = true;
        lastPacket = System.currentTimeMillis();

        shouldExit = false;
        exitReason = "Generic disconnect.";

        inputStream = sock.getInputStream();
        outputStream = sock.getOutputStream();

        primaryHandler = new LoginHandler(this);
        start();
    }

    /* ----------------------------- Reading packets ----------------------------- */

    private void doReadPackets() {
        int readTimeout = 2; // sendQueue.isEmpty() ? 10 : 1;

        for (int index = 0; index < 10; ++index) {
            Packet packet;
            try {
                packet = readPacket(readTimeout);
            } catch (Exception error) {
                if (!(error instanceof SocketTimeoutException)) {
                    logger.warning("Error while reading packets:");
                    logger.throwing(Connection.class.getSimpleName(), "doReadPackets", error);

                    connected = false; // We can be sure that we aren't connected anymore
                    exit(error.toString());
                }
                return; // We've read all we can right now probably
            }

            if (packet != null) {
                lastPacket = System.currentTimeMillis();

                try {
                    if (primaryHandler != null) primaryHandler.handlePacket(packet);
                    new ArrayList<>(secondaryHandlers).forEach(handler -> handler.handlePacket(packet));
                } catch (Exception error) {
                    logger.warning("Error while handling packet:");
                    logger.throwing(Connection.class.getSimpleName(), "doReadPackets", error);
                    exit(error.toString());
                }
            }
        }
    }

    private Packet readPacket(int readTimeout) throws IOException {
        synchronized (this) {
            byte[] headerBytes = new byte[7];
            int bytesRead = 0;
            sock.setSoTimeout(readTimeout);

            while (bytesRead < 7) {
                int justRead = inputStream.read(headerBytes, bytesRead, 7 - bytesRead);
                sock.setSoTimeout(30000); // Set this immediately so we don't timeout on next read, this mistake caused A LOT of pain
                bytesRead += justRead;
                if (bytesRead == 0) { // We can probably say that the peer hasn't sent us anything
                    return null;
                } else if (justRead < 0) {
                    throw new IOException("Connection closed.");
                }
            }

            ByteArrayInputStream headerInputStream = new ByteArrayInputStream(headerBytes);

            int packetLength = Registry.INTEGER.read(headerInputStream);
            byte[] flags = new byte[1];
            headerInputStream.read(flags);
            int packetID = Registry.UNSIGNED_SHORT.read(headerInputStream);

            bytesRead = 0;

            byte[] packetContents = new byte[packetLength];
            while (bytesRead < packetLength) {
                int justRead = inputStream.read(packetContents, bytesRead, packetLength - bytesRead);
                if (justRead == 0) {
                    return null;
                } else if (justRead < 0) {
                    throw new IOException("Connection closed.");
                }
                bytesRead += justRead;
            }

            if ((flags[0] & 1) == 1) { // Compression
                Inflater inflater = new Inflater();
                ByteArrayOutputStream decompressed = new ByteArrayOutputStream();

                inflater.setInput(packetContents);

                byte[] buffer = new byte[1024];
                while (!inflater.finished()) {
                    int amountRead;
                    try {
                        amountRead = inflater.inflate(buffer);
                    } catch (DataFormatException error) {
                        break;
                    }
                    decompressed.write(buffer, 0, amountRead);
                }

                inflater.end();
                decompressed.close();
                packetContents = decompressed.toByteArray();
            }

            // logger.debug(packetID);

            Optional<Class<? extends Packet>> packetClazz = capabilities.stream()
                    .filter(clazz -> Packet.getID(clazz) == packetID)
                    .findFirst();

            Packet packet = null;

            if (packetClazz.isPresent()) {
                try {
                    packet = packetClazz.get().newInstance();
                } catch (IllegalAccessException | InstantiationException error) {
                    logger.warning(String.format("Couldn't create a new instance of %s:", packetClazz.get()));
                    logger.throwing(Connection.class.getSimpleName(), "readPacket", error);
                    exit("Error while instantiating packet.");
                }
            } else {
                logger.warning(String.format("Unknown packet ID: %d", packetID));
                exit("Received unknown packet.");
            }

            if (packet != null) packet.read(new ByteArrayInputStream(packetContents));

            return packet;
        }
    }

    public Packet getLatestPacket(int timeout) {
        long startedTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startedTime < timeout) {
            if (!latestRecv.isEmpty()) return latestRecv.remove(0);
        }
        return null;
    }

    /* ----------------------------- Writing packets ----------------------------- */

    private void doSendPackets() {
        for (int index = 0; index < Math.min(15, sendQueue.size()); ++index) {
            try {
                writePacket(sendQueue.remove(0), false);
            } catch (Exception error) {
                if (!(error instanceof SocketTimeoutException)) {
                    logger.warning("Error while sending packets:");
                    logger.throwing(Connection.class.getSimpleName(), "doSendPackets", error);

                    connected = false;
                    exit(error.toString());
                }
                return;
            }
        }
    }

    private void writePacket(Packet packet, boolean noCompression) throws IOException {
        if (packet == null) return;
        // logger.finest(String.format("Writing packet: %s.", packet));

        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        packet.write(serialized);

        byte flags = 0;

        if (!noCompression && serverInfo.isCompressionEnabled() && serialized.size() > serverInfo.getCompressionThreshold()) {
            flags |= 1;

            Deflater deflater = new Deflater();
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();

            deflater.setInput(serialized.toByteArray());
            deflater.finish();

            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int amountWritten = deflater.deflate(buffer);
                compressed.write(buffer, 0, amountWritten);
            }

            deflater.end();
            compressed.close();

            serialized = new ByteArrayOutputStream();
            serialized.write(compressed.toByteArray());
        }

        Registry.INTEGER.write(serialized.size(), outputStream);
        outputStream.write(flags);
        Registry.UNSIGNED_SHORT.write(Packet.getID(packet.getClass()), outputStream);
        outputStream.write(serialized.toByteArray());
        // outputStream.flush();
    }

    public void sendPacket(Packet packet, boolean instant, boolean noCompression) {
        if (instant) {
            try {
                writePacket(packet, noCompression);
            } catch (IOException error) {
                exit(error.toString());
            }
        } else {
            sendQueue.add(packet);
        }
    }

    public void sendPacket(Packet packet, boolean instant) {
        sendPacket(packet, instant, false);
    }

    public void sendPacket(Packet packet) {
        sendPacket(packet, false);
    }

    /* ----------------------------- Capabilities ----------------------------- */

    public List<Class<? extends Packet>> getCapabilities() {
        return new ArrayList<>(capabilities);
    }

    public void addCapability(Class<? extends Packet> packetClazz) {
        if (!capabilities.contains(packetClazz)) capabilities.add(packetClazz);
    }

    public void setCapabilities(List<Class<? extends Packet>> capabilities) {
        this.capabilities.clear();
        this.capabilities.addAll(capabilities);
    }

    public void addCapabilities(List<Class<? extends Packet>> capabilities) {
        this.capabilities.addAll(capabilities);
    }

    public void removeCapability(Class<? extends Packet> packetClazz) {
        capabilities.remove(packetClazz);
    }

    /* ----------------------------- Other ----------------------------- */

    public void exit(String reason) {
        if (shouldExit) return;

        logger.finer(String.format("Exiting, reason: %s", reason));
        shouldExit = true;
        exitReason = reason;
    }

    public void exit() {
        exit("Generic disconnect.");
    }

    /* ----------------------------- Setters and getters ----------------------------- */

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        doReadPackets();
        synchronized (this) {
            this.inputStream = inputStream;
        }
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        doSendPackets();
        synchronized (this) {
            this.outputStream = outputStream;
        }
    }

    public Account getAccount() {
        return accountSupplier == null ? new Account("", "", "") : accountSupplier.get();
    }

    public User getServerUser() {
        return serverUser;
    }

    public void setServerUser(User serverUser) {
        this.serverUser = serverUser;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isExited() {
        return exited;
    }

    public String getExitReason() {
        return exitReason;
    }

    /* ----------------------------- Inner classes ----------------------------- */

    /**
     * A representation of an account, for simplicity.
     */
    public static class Account {

        private final String username;
        private final String password;
        private final String groupName;

        public Account(String username, String password, String groupName) {
            this.username = username;
            this.password = password;
            this.groupName = groupName;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getGroupName() {
            return groupName;
        }
    }

    /**
     * Stores information about the server we are connected to.
     */
    public static class ServerInfo {

        private String serverName;

        private boolean encryptionEnabled;
        private EncryptionType encryptionType;

        private boolean compressionEnabled;
        private int compressionThreshold;

        private boolean authenticationEnabled;

        public ServerInfo(String serverName, boolean encryptionEnabled, EncryptionType encryptionType,
                          boolean compressionEnabled, int compressionThreshold, boolean authenticationEnabled) {
            this.serverName = serverName;
            this.encryptionEnabled = encryptionEnabled;
            this.encryptionType = encryptionType;
            this.compressionEnabled = compressionEnabled;
            this.compressionThreshold = compressionThreshold;
            this.authenticationEnabled = authenticationEnabled;
        }

        /**
         * @return The reported name of the server.
         */
        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        /**
         * @return Whether or not encryption is enabled on the server.
         */
        public boolean isEncryptionEnabled() {
            return encryptionEnabled;
        }

        public void setEncryptionEnabled(boolean encryptionEnabled) {
            this.encryptionEnabled = encryptionEnabled;
        }

        /**
         * @return The type of encryption the server wants us to use.
         */
        public EncryptionType getEncryptionType() {
            return encryptionType;
        }

        public void setEncryptionType(EncryptionType encryptionType) {
            this.encryptionType = encryptionType;
        }

        /**
         * @return Whether or not compression is enabled on the server.
         */
        public boolean isCompressionEnabled() {
            return compressionEnabled;
        }

        public void setCompressionEnabled(boolean compressionEnabled) {
            this.compressionEnabled = compressionEnabled;
        }

        /**
         * @return The compression threshold.
         */
        public int getCompressionThreshold() {
            return compressionThreshold;
        }

        public void setCompressionThreshold(int compressionThreshold) {
            this.compressionThreshold = compressionThreshold;
        }

        /**
         * @return Whether or not authentication is enabled on the server.
         */
        public boolean isAuthenticationEnabled() {
            return authenticationEnabled;
        }

        public void setAuthenticationEnabled(boolean authenticationEnabled) {
            this.authenticationEnabled = authenticationEnabled;
        }
    }
}
