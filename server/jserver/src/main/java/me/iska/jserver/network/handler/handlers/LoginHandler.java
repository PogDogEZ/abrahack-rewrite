package me.iska.jserver.network.handler.handlers;

import me.iska.jserver.JServer;
import me.iska.jserver.event.events.connection.CapabilitiesEvent;
import me.iska.jserver.event.events.connection.LoginEvent;
import me.iska.jserver.exception.AuthenticationException;
import me.iska.jserver.network.Connection;
import me.iska.jserver.network.Encryption;
import me.iska.jserver.network.handler.IHandler;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.packets.*;
import me.iska.jserver.user.User;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoginHandler implements IHandler {

    private final JServer jServer = JServer.getInstance();
    private final Logger logger = JServer.getLogger();

    private final Connection connection;

    private State currentState;

    public LoginHandler(Connection connection) {
        this.connection = connection;

        currentState = jServer.connectionManager.isEncryptionEnabled() ? State.ENCRYPTION : State.CAPABILITIES;

        ServerInfoPacket serverInfo = new ServerInfoPacket(jServer.getServerName(), jServer.getProtocolVersion(),
                jServer.connectionManager.getEncryptionType(), jServer.connectionManager.getCompressionThreshold());
        serverInfo.setCompressionEnabled(jServer.connectionManager.isCompressionEnabled());
        serverInfo.setEncryptionEnabled(jServer.connectionManager.isEncryptionEnabled());
        serverInfo.setAuthenticationEnabled(jServer.connectionManager.isAuthenticationEnabled());

        this.connection.sendPacket(serverInfo, true, true);
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof DisconnectPacket) {
            connection.exit(((DisconnectPacket)packet).getMessage());
            return;
        }

        switch (currentState) {
            case ENCRYPTION: {
                if (packet instanceof EncryptionRequestPacket) {
                    EncryptionRequestPacket encryptionRequest = (EncryptionRequestPacket)packet;

                    if (!jServer.connectionManager.isEncryptionEnabled()) {
                        connection.exit("Unexpected encryption request.");
                        return;
                    }

                    logger.fine(String.format("Starting secure connection with %s.", connection));
                    logger.finer(String.format("Beginning with key size %d.", encryptionRequest.getKeySize()));

                    try {
                        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
                        KeyFactory keyFactory = KeyFactory.getInstance("DH");
                        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");

                        logger.finer("Initializing with provided parameters...");
                        DHParameterSpec dhParameters = new DHParameterSpec(encryptionRequest.getParamP(), encryptionRequest.getParamG());
                        keyPairGenerator.initialize(dhParameters);

                        logger.finer("Generating key pair...");
                        KeyPair keyPair = keyPairGenerator.generateKeyPair();
                        keyAgreement.init(keyPair.getPrivate());

                        logger.finer("Generating init vector...");
                        byte[] initVector = new byte[32];
                        new Random().nextBytes(initVector);

                        logger.finer("Generating key spec and encoding...");
                        X509EncodedKeySpec serverKeySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());

                        logger.finer("Decoding client's public key...");
                        X509EncodedKeySpec clientKeySpec = new X509EncodedKeySpec(encryptionRequest.getAPeerPublicKey());

                        logger.finer("Executing phase and generating shared secret...");
                        keyAgreement.doPhase(keyFactory.generatePublic(clientKeySpec), true);
                        byte[] sharedSecret = keyAgreement.generateSecret();

                        logger.finer("Creating ciphers...");
                        Cipher encryptCipher = Encryption.getCipherFromSecrets(jServer.connectionManager.getEncryptionType(),
                                Cipher.ENCRYPT_MODE, sharedSecret, initVector);
                        Cipher decryptCipher = Encryption.getCipherFromSecrets(jServer.connectionManager.getEncryptionType(),
                                Cipher.DECRYPT_MODE, sharedSecret, initVector);

                        connection.setInputStream(new CipherInputStream(connection.getInputStream(), decryptCipher));
                        connection.sendPacket(new EncryptionResponsePacket(serverKeySpec.getEncoded(), initVector), true);
                        connection.setOutputStream(new CipherOutputStream(connection.getOutputStream(), encryptCipher));

                        logger.fine("Done.");

                    } catch (GeneralSecurityException error) {
                        logger.warning("Failed to initialize secure connection:");
                        logger.throwing(LoginHandler.class.getSimpleName(), "handlePacket", error);
                        return;
                    }

                    logger.info(String.format("A secure connection has been established with %s.", connection));
                    currentState = State.CAPABILITIES;

                } else {
                    throw new IllegalStateException(String.format("Expected %s, got %s.",
                            Packet.getName(EncryptionRequestPacket.class), Packet.getName(packet.getClass())));
                }
                break;
            }
            case CAPABILITIES: {
                if (packet instanceof ClientCapabilitiesPacket) {
                    ClientCapabilitiesPacket clientCapabilities = (ClientCapabilitiesPacket)packet;

                    List<ClientCapabilitiesPacket.PacketRepresentation> clientPackets = clientCapabilities.getPackets();
                    List<ClientCapabilitiesPacket.PacketRepresentation> serverPackets = Registry.KNOWN_PACKETS.stream()
                            .map(ClientCapabilitiesPacket.PacketRepresentation::new)
                            .collect(Collectors.toList());
                    boolean rejected = false;

                    // Check we support all the packets they want
                    for (ClientCapabilitiesPacket.PacketRepresentation serverPacket : serverPackets) {
                        if (!clientPackets.contains(serverPacket)) {
                            rejected = true;
                            break;
                        }
                    }

                    // Check they support all the packets we want
                    for (ClientCapabilitiesPacket.PacketRepresentation clientPacket : clientPackets) {
                        if (!serverPackets.contains(clientPacket)) {
                            rejected = true;
                            break;
                        }
                    }

                    CapabilitiesEvent event = new CapabilitiesEvent(connection, rejected, clientPackets, serverPackets);
                    jServer.eventBus.post(event);

                    if (event.isCancelled()) {
                        clientPackets = event.getClientPackets();
                        serverPackets = event.getServerPackets();
                        rejected = event.isRejected();
                    }

                    for (ClientCapabilitiesPacket.PacketRepresentation serverPacket : serverPackets) {
                        if (!clientPackets.contains(serverPacket)) {
                            connection.getCapabilities().stream()
                                    .filter(packetClazz -> Packet.getID(packetClazz) == serverPacket.getID() &&
                                            Packet.getName(packetClazz).equals(serverPacket.getName()) &&
                                            Packet.getSide(packetClazz) == serverPacket.getSide())
                                    .findFirst()
                                    .ifPresent(connection::removeCapability);
                            logger.fine(String.format("Unsupported (server-client): %s.", serverPacket));
                        }
                    }
                    for (ClientCapabilitiesPacket.PacketRepresentation clientPacket : clientPackets) {
                        if (!serverPackets.contains(clientPacket)) logger.fine(String.format("Unsupported (client-server): %s.", clientPacket));
                    }
                    logger.fine(String.format("Rejected: %s.", rejected));

                    connection.sendPacket(new ClientCapabilitiesResponsePacket(rejected));

                    if (jServer.connectionManager.isAuthenticationEnabled()) {
                        currentState = State.LOGIN;
                    } else {
                        if (login("", "", "")) connection.setPrimaryHandler(new DefaultHandler(connection));
                    }

                } else {
                    throw new IllegalStateException(String.format("Expected %s, got %s.",
                            Packet.getName(ClientCapabilitiesPacket.class), Packet.getName(packet.getClass())));
                }
                break;
            }
            case LOGIN: {
                if (packet instanceof LoginRequestPacket) {
                    LoginRequestPacket loginRequest = (LoginRequestPacket)packet;

                    if (!jServer.connectionManager.isAuthenticationEnabled()) {
                        connection.exit("Unexpected login attempt.");
                        return;
                    }

                    if (login(loginRequest.getUsername(), loginRequest.getPassword(), loginRequest.getGroupName()))
                        connection.setPrimaryHandler(new DefaultHandler(connection));

                } else {
                    throw new IllegalStateException(String.format("Expected %s, got %s.",
                            Packet.getName(LoginRequestPacket.class), Packet.getName(packet.getClass())));
                }
                break;
            }
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void exit(String reason) {
        connection.sendPacket(new DisconnectPacket(reason));
    }

    private boolean login(String username, String password, String groupName) {
        String message = "No additional info.";

        User user = null;
        if (jServer.connectionManager.isAuthenticationEnabled()) {
            try {
                user = jServer.userManager.login(username, password, groupName);
                if (user != null) jServer.connectionManager.setUser(connection, user);
            } catch (AuthenticationException error) {
                message = error.getMessage();
            }
        }
        boolean successful = !jServer.connectionManager.isAuthenticationEnabled() || user != null;

        LoginEvent event = new LoginEvent(connection, username, password, groupName, successful, message);
        jServer.eventBus.post(event);

        if (event.isCancelled()) {
            successful = event.isSuccessful();
            message = event.getMessage();
        }

        connection.sendPacket(new LoginResponsePacket(successful, message, user));
        if (successful) {
            logger.info(String.format("%s logged in successfully.", connection));

        } else {
            logger.info(String.format("%s failed to login: %s", connection, message));
            connection.exit(message);
        }

        return successful;
    }

    public enum State {
        ENCRYPTION, CAPABILITIES, LOGIN;
    }
}
