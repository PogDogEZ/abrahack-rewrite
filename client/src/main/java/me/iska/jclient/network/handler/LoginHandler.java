package me.iska.jclient.network.handler;

import me.iska.jclient.network.Connection;
import me.iska.jclient.network.Encryption;
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
import javax.crypto.spec.DHPublicKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class LoginHandler implements IHandler {

    private final int keySize = 512;

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

                    connection.logger.debug(String.format("Connected to server: %s.", serverInfo.getServerName()));

                    connection.setServerName(serverInfo.getServerName());
                    connection.setEncryption(serverInfo.isEncryptionEnabled());
                    connection.setEncryptionType(serverInfo.getEncryptionType());
                    connection.setCompression(serverInfo.isCompressionEnabled());
                    connection.setCompressionThreshold(serverInfo.getCompressionThreshold());

                    if (connection.getEncryption()) {
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

                    connection.logger.debug("We have the peer's public key, decoding it...");
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encryptionResponse.getBPeerPublicKey());

                    connection.logger.debug("Generating public and executing phase...");
                    try {
                        keyAgreement.doPhase(keyFactory.generatePublic(keySpec), true);
                    } catch (InvalidKeyException | InvalidKeySpecException error) {
                        connection.exit(error.toString());
                        return;
                    }

                    connection.logger.debug("Generating shared secret...");
                    byte[] sharedSecret = keyAgreement.generateSecret();

                    // connection.logger.debug(Base64.getEncoder().encodeToString(sharedSecret));

                    connection.logger.debug("Creating ciphers...");

                    Cipher encryptCipher;
                    Cipher decryptCipher;
                    try {
                        encryptCipher = Encryption.getCipherFromSecrets(connection.getEncryptionType(), Cipher.ENCRYPT_MODE,
                                sharedSecret, encryptionResponse.getInitVector());
                        decryptCipher = Encryption.getCipherFromSecrets(connection.getEncryptionType(), Cipher.DECRYPT_MODE,
                                sharedSecret, encryptionResponse.getInitVector());
                    } catch (GeneralSecurityException error) {
                        connection.exit(error.toString());
                        return;
                    }

                    connection.setInputStream(new CipherInputStream(connection.getInputStream(), decryptCipher));
                    connection.setOutputStream(new CipherOutputStream(connection.getOutputStream(), encryptCipher));

                    connection.logger.debug("Done.");

                    connection.logger.info(String.format("Secure connection has been established with %s.",
                            connection.getServerName()));

                    startLogin();

                    // keyAgreement.doPhase()
                }
                break;
            }
            case LOGIN: {
                if (packet instanceof ClientCapabilitiesResponsePacket) {
                    ClientCapabilitiesResponsePacket clientCapabilitiesResponse = (ClientCapabilitiesResponsePacket)packet;

                    if (clientCapabilitiesResponse.isRejected()) {
                        connection.exit("Client capabilities rejected.");
                    } else {
                        connection.logger.debug("Client capabilities accepted, authenticating...");
                        connection.sendPacket(new LoginRequestPacket(connection.getUser(), connection.getPassword()));
                    }

                } else if (packet instanceof LoginResponsePacket) {
                    LoginResponsePacket loginResponse = (LoginResponsePacket)packet;

                    if (!loginResponse.isSuccessful()) {
                        connection.exit(loginResponse.getMessage());
                    } else {
                        connection.logger.info(String.format("Successfully authenticated with server %s: %s.",
                                connection.getServerName(), loginResponse.getMessage()));
                        doHandOff();
                    }

                } else {
                    connection.exit(String.format("Invalid packet in stage LOGIN: %s.", packet));
                }
                break;
            }
        }
    }

    private void startEncryption() throws GeneralSecurityException {
        currentState = State.ENCRYPTION;
        connection.logger.info(String.format("Attempting to start a secure connection with server %s.",
                connection.getServerName()));

        AlgorithmParameterGenerator paramGenerator = AlgorithmParameterGenerator.getInstance("DH");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("DH");
        keyFactory = KeyFactory.getInstance("DH");
        keyAgreement = KeyAgreement.getInstance("DH");

        connection.logger.debug(String.format("Beginning with key size: %d.", keySize));

        connection.logger.debug("Initializing parameter generator...");
        paramGenerator.init(keySize);

        connection.logger.debug("Generating parameters...");
        AlgorithmParameters parameters = paramGenerator.generateParameters();
        DHParameterSpec dhParameters = parameters.getParameterSpec(DHParameterSpec.class);

        generator.initialize(new DHParameterSpec(dhParameters.getP(), dhParameters.getG(), keySize));
        connection.logger.debug("Generating key pair...");
        KeyPair keyPair = generator.generateKeyPair();
        connection.logger.debug("Initializing key agreement...");
        keyAgreement.init(keyPair.getPrivate());

        connection.logger.debug("Generating key spec and encoding...");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());

        connection.logger.debug("Sending generated key...");
        // Yeah it looks like I got them the wrong way around (or did python)
        EncryptionRequestPacket encryptionRequest = new EncryptionRequestPacket(keySpec.getEncoded(), keySize,
                dhParameters.getG(), dhParameters.getP());
        connection.sendPacket(encryptionRequest);

        connection.logger.debug("Waiting for server response...");
    }

    private void startLogin() {
        currentState = State.LOGIN;

        connection.logger.debug("Sending client capabilities...");
        connection.sendPacket(new ClientCapabilitiesPacket());
    }

    private void doHandOff() {
        connection.setHandler(new DefaultHandler(connection));
    }

    public State getCurrentState() {
        return currentState;
    }

    private enum State {
        HAND_SHAKE, ENCRYPTION, LOGIN;
    }
}
