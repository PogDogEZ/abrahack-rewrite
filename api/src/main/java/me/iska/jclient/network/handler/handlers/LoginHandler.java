package me.iska.jclient.network.handler.handlers;

import me.iska.jclient.network.Connection;
import me.iska.jclient.network.Encryption;
import me.iska.jclient.network.handler.IHandler;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.packets.ClientCapabilitiesPacket;
import me.iska.jclient.network.packet.packets.ClientCapabilitiesResponsePacket;
import me.iska.jclient.network.packet.packets.DisconnectPacket;
import me.iska.jclient.network.packet.packets.EncryptionRequestPacket;
import me.iska.jclient.network.packet.packets.EncryptionResponsePacket;
import me.iska.jclient.network.packet.packets.LoginRequestPacket;
import me.iska.jclient.network.packet.packets.LoginResponsePacket;
import me.iska.jclient.network.packet.packets.ServerInfoPacket;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

public class LoginHandler implements IHandler {

    private final int KEY_SIZE = 512;
    
    private final Logger logger = Connection.getLogger();

    private final Connection connection;

    private State currentState;
    private KeyFactory keyFactory;
    private KeyAgreement keyAgreement;

    public LoginHandler(Connection connection) {
        this.connection = connection;

        currentState = State.HAND_SHAKE;
    }

    @Override
    public String toString() {
        return String.format("LoginHandler(state=%s)", currentState.name());
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof DisconnectPacket) {
            connection.exit(((DisconnectPacket)packet).getMessage());
            return;
        }

        switch (currentState) {
            case HAND_SHAKE: {
                if (packet instanceof ServerInfoPacket) {
                    ServerInfoPacket serverInfo = (ServerInfoPacket)packet;

                    logger.fine(String.format("Connected to server: %s.", serverInfo.getServerName()));

                    connection.getServerInfo().setServerName(serverInfo.getServerName());
                    connection.getServerInfo().setEncryptionEnabled(serverInfo.isEncryptionEnabled());
                    connection.getServerInfo().setEncryptionType(serverInfo.getEncryptionType());
                    connection.getServerInfo().setCompressionEnabled(serverInfo.isCompressionEnabled());
                    connection.getServerInfo().setCompressionThreshold(serverInfo.getCompressionThreshold());
                    connection.getServerInfo().setAuthenticationEnabled(serverInfo.isAuthenticationEnabled());

                    if (connection.getServerInfo().isEncryptionEnabled()) {
                        try {
                            startEncryption();
                        } catch (GeneralSecurityException error) {
                            connection.exit(error.toString());
                        }
                    } else {
                        startLogin();
                    }

                } else {
                    connection.exit(String.format("Invalid packet in stage HAND_SHAKE: %s.", packet));
                }
                break;
            }
            case ENCRYPTION: {
                if (packet instanceof EncryptionResponsePacket) {
                    EncryptionResponsePacket encryptionResponse = (EncryptionResponsePacket)packet;

                    logger.finer("We have the peer's public key, decoding it...");
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encryptionResponse.getBPeerPublicKey());

                    logger.finer("Generating public and executing phase...");
                    try {
                        keyAgreement.doPhase(keyFactory.generatePublic(keySpec), true);
                    } catch (InvalidKeyException | InvalidKeySpecException error) {
                        connection.exit(error.toString());
                        return;
                    }

                    logger.finer("Generating shared secret...");
                    byte[] sharedSecret = keyAgreement.generateSecret();

                    // logger.debug(Base64.getEncoder().encodeToString(sharedSecret));

                    logger.finer("Creating ciphers...");

                    Cipher encryptCipher;
                    Cipher decryptCipher;
                    try {
                        encryptCipher = Encryption.getCipherFromSecrets(connection.getServerInfo().getEncryptionType(),
                                Cipher.ENCRYPT_MODE, sharedSecret, encryptionResponse.getInitVector());
                        decryptCipher = Encryption.getCipherFromSecrets(connection.getServerInfo().getEncryptionType(),
                                Cipher.DECRYPT_MODE, sharedSecret, encryptionResponse.getInitVector());
                    } catch (GeneralSecurityException error) {
                        connection.exit(error.toString());
                        return;
                    }

                    connection.setInputStream(new CipherInputStream(connection.getInputStream(), decryptCipher));
                    connection.setOutputStream(new CipherOutputStream(connection.getOutputStream(), encryptCipher));
                    logger.finer("Done.");

                    logger.info(String.format("Secure connection has been established with %s.",
                            connection.getServerInfo().getServerName()));

                    startLogin();
                }
                break;
            }
            case LOGIN: {
                if (packet instanceof ClientCapabilitiesResponsePacket) {
                    ClientCapabilitiesResponsePacket clientCapabilitiesResponse = (ClientCapabilitiesResponsePacket)packet;

                    if (clientCapabilitiesResponse.isRejected()) {
                        connection.exit("Client capabilities rejected.");
                    } else {
                        logger.fine("Client capabilities accepted, authenticating...");
                        if (connection.getServerInfo().isAuthenticationEnabled()) {
                            Connection.Account account = connection.getAccount();
                            connection.sendPacket(new LoginRequestPacket(account.getUsername(), account.getGroupName(),
                                    account.getPassword()));
                        }
                        logger.fine("Waiting for server response...");
                    }

                } else if (packet instanceof LoginResponsePacket) {
                    LoginResponsePacket loginResponse = (LoginResponsePacket)packet;

                    if (!loginResponse.isSuccessful()) {
                        connection.exit(loginResponse.getMessage());
                    } else {
                        logger.info(String.format("Successfully authenticated with server %s: %s",
                                connection.getServerInfo().getServerName(), loginResponse.getMessage()));
                        doHandOff();
                    }

                } else {
                    connection.exit(String.format("Invalid packet in stage LOGIN: %s.", packet));
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

    private void startEncryption() throws GeneralSecurityException {
        currentState = State.ENCRYPTION;
        logger.info(String.format("Attempting to start a secure connection with server %s.",
                connection.getServerInfo().getServerName()));

        AlgorithmParameterGenerator paramGenerator = AlgorithmParameterGenerator.getInstance("DH");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("DH");
        keyFactory = KeyFactory.getInstance("DH");
        keyAgreement = KeyAgreement.getInstance("DH");

        logger.finer(String.format("Beginning with key size: %d.", KEY_SIZE));

        logger.finer("Initializing parameter generator...");
        paramGenerator.init(KEY_SIZE);

        logger.finer("Generating parameters...");
        AlgorithmParameters parameters = paramGenerator.generateParameters();
        DHParameterSpec dhParameters = parameters.getParameterSpec(DHParameterSpec.class);

        generator.initialize(new DHParameterSpec(dhParameters.getP(), dhParameters.getG(), KEY_SIZE));
        logger.finer("Generating key pair...");
        KeyPair keyPair = generator.generateKeyPair();
        logger.finer("Initializing key agreement...");
        keyAgreement.init(keyPair.getPrivate());

        logger.finer("Generating key spec and encoding...");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());

        logger.finer("Sending generated key...");
        // Yeah it looks like I got them the wrong way around (or did python)
        EncryptionRequestPacket encryptionRequest = new EncryptionRequestPacket(keySpec.getEncoded(), KEY_SIZE,
                dhParameters.getG(), dhParameters.getP());
        connection.sendPacket(encryptionRequest);

        logger.fine("Waiting for server response...");
    }

    private void startLogin() {
        currentState = State.LOGIN;

        logger.fine("Sending client capabilities...");
        connection.sendPacket(new ClientCapabilitiesPacket());
    }

    private void doHandOff() {
        connection.setPrimaryHandler(new DefaultHandler(connection));
    }

    public State getCurrentState() {
        return currentState;
    }

    private enum State {
        HAND_SHAKE, ENCRYPTION, LOGIN;
    }
}
