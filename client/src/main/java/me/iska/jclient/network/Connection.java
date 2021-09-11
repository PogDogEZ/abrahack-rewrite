package me.iska.jclient.network;

import ez.pogdog.yescom.logging.Logger;
import me.iska.jclient.impl.user.User;
import me.iska.jclient.network.enums.EncryptionType;
import me.iska.jclient.network.handler.IHandler;
import me.iska.jclient.network.handler.LoginHandler;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import javax.crypto.CipherOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Connection extends Thread {

    public final Logger logger;

    private final List<Packet> latestRecv = new LinkedList<>();
    private final List<Packet> sendQueue = new LinkedList<>();

    private final List<IHandler> otherHandlers = new ArrayList<>();

    private final int protocolVersion = 4;

    private final String host;
    private final int port;

    private Socket sock;
    private InputStream inputStream;
    private OutputStream outputStream;

    private IHandler handler;

    private Supplier<User> userSupplier;
    private Supplier<String> passwordSupplier;
    private boolean cacheLatest;

    private boolean connected;
    private String serverName;

    private boolean encryption;
    private EncryptionType encryptionType;

    private boolean compression;
    private int compressionThreshold;

    private boolean shouldExit;
    private String exitReason;

    public Connection(String host, int port, Logger logger) {
        this.host = host;
        this.port = port;

        this.logger = logger;

        cacheLatest = false;

        connected = false;
        serverName = "";

        encryption = false;
        encryptionType = EncryptionType.NONE;

        compression = false;
        compressionThreshold = 256;

        shouldExit = false;
        exitReason = "Generic Disconnect.";
    }

    @Override
    @SuppressWarnings("BusyWait")
    public void run() {
        while (connected) {
            doSendPackets();
            doReadPackets();

            if (shouldExit) { // We still want to read and write packets while exiting
                if (!sendQueue.isEmpty()) {
                    doSendPackets();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }

                logger.info(String.format("Client disconnect, reason: '%s'.", exitReason));

                try {
                    sock.close();
                } catch (IOException error) {
                    logger.warn(String.format("Error while closing socket: %s.", error));
                }

                connected = false;
            }

            /*
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
             */
        }
    }

    /* ----------------------------- Handlers and Connection Stuff ----------------------------- */

    public void setAuthSuppliers(Supplier<User> userSupplier, Supplier<String> passwordSupplier) {
        this.userSupplier = userSupplier;
        this.passwordSupplier = passwordSupplier;
    }

    public IHandler getHandler() {
        return handler;
    }

    public void setHandler(IHandler handler) {
        this.handler = handler;
    }

    public void addHandler(IHandler handler) {
        if (!otherHandlers.contains(handler)) otherHandlers.add(handler);
    }

    public void removeHandler(IHandler handler) {
        otherHandlers.remove(handler);
    }

    public void connect() throws IOException {
        sock = new Socket();
        // For some reason Java sockets only support AF_INET and AF_INET6 (that's what the server uses but still)
        // And I guess they're also using TCP cos fuck support for anything else
        sock.connect(new InetSocketAddress(host, port));

        connected = true;
        shouldExit = false;
        exitReason = "Generic Disconnect";

        inputStream = sock.getInputStream();
        outputStream = sock.getOutputStream();

        handler = new LoginHandler(this);
        start();
    }

    /* ----------------------------- Reading Packets ----------------------------- */

    private void doReadPackets() {
        int readTimeout = sendQueue.isEmpty() ? 10 : 1;

        for (int index = 0; index < 300; ++index) {
            Packet packet;
            try {
                packet = readPacket(readTimeout);
            } catch (Exception error) {
                if (!(error instanceof SocketTimeoutException)) exit(error.toString());
                return; // We've read all we can right now probably
            }

            if (packet != null) {
                if (handler != null) handler.handlePacket(packet);
                otherHandlers.forEach(handler -> handler.handlePacket(packet));
                if (cacheLatest) latestRecv.add(packet);
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
                if (justRead <= 0) return null; // We can probably say that the peer hasn't sent us anything
                bytesRead += justRead;
            }

            sock.setSoTimeout(30000);

            ByteArrayInputStream headerInputStream = new ByteArrayInputStream(headerBytes);

            int packetLength = Registry.INT.read(headerInputStream);
            byte[] flags = new byte[1];
            headerInputStream.read(flags);
            int packetID = Registry.UNSIGNED_SHORT.read(headerInputStream);

            bytesRead = 0;

            byte[] packetContents = new byte[packetLength];
            while (bytesRead < packetLength) {
                int justRead = inputStream.read(packetContents, bytesRead, packetLength - bytesRead);
                if (justRead <= 0) return null;
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

            Optional<Class<? extends Packet>> packetClazz = Registry.knownPackets.stream()
                    .filter(clazz -> Packet.getID(clazz) == packetID)
                    .findFirst();

            Packet packet = null;

            if (packetClazz.isPresent()) {
                try {
                    packet = packetClazz.get().newInstance();
                } catch (IllegalAccessException | InstantiationException error) {
                    logger.warn(String.format("Couldn't create a new instance of %s:", packetClazz));
                    logger.error(error.toString());
                    // TODO: Exception?
                }
            } else {
                // TODO: Exception?
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

    /* ----------------------------- Writing Packets ----------------------------- */

    private void doSendPackets() {
        for (int index = 0; index < Math.min(15, sendQueue.size()); ++index) {
            try {
                writePacket(sendQueue.remove(0), false);
            } catch (Exception error) {
                if (!(error instanceof SocketTimeoutException)) exit(error.toString());
                return;
            }
        }
    }

    private void writePacket(Packet packet, boolean noCompression) throws IOException {
        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        packet.write(serialized);

        byte flags = 0;

        if (!noCompression && compression && serialized.size() > compressionThreshold) {
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

        Registry.INT.write(serialized.size(), outputStream);
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

    /* ----------------------------- Other ----------------------------- */

    public void exit(String reason) {
        shouldExit = true;
        exitReason = reason;
    }

    public void exit() {
        exit("Generic Disconnect.");
    }

    /* ----------------------------- Setters/Getters ----------------------------- */

    public boolean getCacheLatest() {
        return cacheLatest;
    }

    public void setCacheLatest(boolean cacheLatest) {
        this.cacheLatest = cacheLatest;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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

    public boolean isConnected() {
        return connected;
    }

    public User getUser() {
        return userSupplier == null ? null : userSupplier.get();
    }

    public String getPassword() {
        return passwordSupplier == null ? "" : passwordSupplier.get();
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean getEncryption() {
        return encryption;
    }

    public void setEncryption(boolean encryption) {
        this.encryption = encryption;
    }

    public EncryptionType getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(EncryptionType encryptionType) {
        this.encryptionType = encryptionType;
    }

    public boolean getCompression() {
        return compression;
    }

    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    public void setCompressionThreshold(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    public String getExitReason() {
        return exitReason;
    }
}
