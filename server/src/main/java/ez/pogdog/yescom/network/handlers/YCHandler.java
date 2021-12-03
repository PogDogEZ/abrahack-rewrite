package ez.pogdog.yescom.network.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.network.packets.shared.DataExchangePacket;
import ez.pogdog.yescom.network.packets.shared.YCExtendedResponsePacket;
import ez.pogdog.yescom.network.packets.shared.YCInitRequestPacket;
import ez.pogdog.yescom.network.packets.shared.YCInitResponsePacket;
import ez.pogdog.yescom.user.permission.ListenerPermission;
import me.iska.jserver.JServer;
import me.iska.jserver.network.Connection;
import me.iska.jserver.network.handler.IHandler;
import me.iska.jserver.network.packet.Packet;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class YCHandler implements IHandler {

    protected final JServer jServer = JServer.getInstance();
    protected final Logger logger = JServer.getLogger();
    protected final YesCom yesCom = YesCom.getInstance();

    protected final Connection connection;
    private final int handlerID;

    private String handlerName; // Used for extended init
    private String minecraftHost;
    private int minecraftPort;

    private byte[] nonce;

    public YCHandler(Connection connection, int handlerID) {
        this.connection = connection;
        this.handlerID = handlerID;
    }

    @Override
    public String toString() {
        return String.format("YCHandler(connection=%s)", connection);
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof YCInitRequestPacket) {
            YCInitRequestPacket initRequest = (YCInitRequestPacket)packet;

            logger.fine(String.format("Attempting YC init for %s...", connection));

            YCHandler newHandler = null;
            String message = "";

            switch (initRequest.getClientType()) {
                case LISTENING: {
                    logger.finer(String.format("%s is listening.", connection));

                    /*
                    if (jServer.connectionManager.getUser(connection) == null) {
                        logger.warning(String.format("%s attempted to init YC connection without user, check your config.", connection));
                        message = "No user.";
                    } else if (!jServer.connectionManager.getUser(connection).getPermissions().contains(new ListenerPermission())) {
                        logger.warning(String.format("%s attempted to init YC connection, but does not have listener permission.", connection));
                        message = "No listener permission.";
                    } else {
                    }
                     */

                    newHandler = new YCListener(connection, yesCom.handlersManager.getCurrentHandlerID(), initRequest.getHandlerName());
                    message = "You'll probably never read this lol, but if you do, wtf are you doing?";
                    break;
                }
                case REPORTING: {
                    if (!yesCom.trustedManager.isTrusted(initRequest.getHandlerHash(), initRequest.getPublicKey())) {
                        logger.warning(String.format("Untrusted handler attempted connection: %s.", connection));
                        message = "Untrusted handler.";
                    } else {
                        logger.finer(String.format("Creating extended init for handler %s.", connection));

                        handlerName = initRequest.getHandlerName();
                        minecraftHost = initRequest.getHostName();
                        minecraftPort = initRequest.getHostPort();

                        nonce = new byte[32];
                        new Random().nextBytes(nonce);

                        try {
                            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(initRequest.getPublicKey()));

                            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

                            connection.sendPacket(new YCInitResponsePacket(cipher.doFinal(nonce)));

                        } catch (GeneralSecurityException error) {
                            logger.warning("An error occurred while generating the signature:");
                            logger.throwing(YCHandler.class.getSimpleName(), "handlePacket", error);
                        }

                        return;
                    }
                    break;
                }
                case ARCHIVING: { // TODO: Finish archivers
                    message = "Not implemented yet.";
                    break;
                }
            }

            if (newHandler != null) {
                logger.fine(String.format("Successfully initialized YC connection with %s.", connection));

                connection.removeSecondaryHandler(this);
                connection.addSecondaryHandler(newHandler);
                yesCom.handlersManager.addHandler(newHandler);

                connection.sendPacket(new YCInitResponsePacket(false, newHandler.getID(), message));

            } else {
                logger.fine(String.format("Unsuccessful YC init attempt from %s: %s", connection, message));

                connection.sendPacket(new YCInitResponsePacket(true, -1, message));
                connection.exit(message);
            }

        } else if (packet instanceof YCExtendedResponsePacket) {
            YCExtendedResponsePacket extendedResponse = (YCExtendedResponsePacket)packet;

            YCHandler newHandler = null;
            String message = "";

            if (Arrays.equals(nonce, extendedResponse.getIdentityProofNonce())) {
                newHandler = new YCReporter(connection, yesCom.handlersManager.getCurrentHandlerID(), handlerName,
                        minecraftHost, minecraftPort);
                message = "Oooooh a hidden message that you're not supposed to see :o.";
            } else {
                message = "Invalid nonce.";
            }

            if (newHandler != null) {
                logger.fine(String.format("Successfully initialized YC connection with %s.", connection));

                connection.removeSecondaryHandler(this);
                connection.addSecondaryHandler(newHandler);
                yesCom.handlersManager.addHandler(newHandler);

                connection.sendPacket(new YCInitResponsePacket(false, newHandler.getID(), message));

            } else {
                connection.sendPacket(new YCInitResponsePacket(true, -1, message));
                connection.exit(message);
            }
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void exit(String reason) {
        yesCom.handlersManager.removeHandler(this);
    }

    public void provideData(DataExchangePacket.DataType dataType, List<Object> data, List<BigInteger> invalidDataIDs,
                            long startTime, long endTime, int updateInterval) {
        connection.sendPacket(new DataExchangePacket(dataType, 0, data, invalidDataIDs, startTime, endTime, updateInterval));
    }

    public void requestData(int originatorID, DataExchangePacket.DataType dataType, List<BigInteger> dataIDs,
                            long startTime, long endTime) {
    }

    public int getID() {
        return handlerID;
    }
}
