package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="yc_init_request", id=YCRegistry.ID_OFFSET, side=Packet.Side.CLIENT)
public class YCInitRequestPacket extends Packet {

    private final EnumType<ClientType> CLIENT_TYPE = new EnumType<>(ClientType.class);

    private ClientType clientType;

    private byte[] handlerHash;
    private byte[] publicKey;
    private String handlerName;

    private String hostName;
    private int hostPort;

    public YCInitRequestPacket(ClientType clientType, byte[] handlerHash, byte[] publicKey, String handlerName,
                               String hostName, int hostPort) {
        this.clientType = clientType;

        this.handlerHash = handlerHash.clone();

        this.publicKey = publicKey.clone();
        this.handlerName = handlerName;

        this.hostName = hostName;
        this.hostPort = hostPort;
    }

    public YCInitRequestPacket(String handlerName, String hostName, int hostPort) {
        this(ClientType.LISTENING, new byte[0], new byte[0], handlerName, hostName, hostPort);
    }

    public YCInitRequestPacket() {
        this(ClientType.REPORTING, new byte[0], new byte[0], "", "localhost", 25565);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        clientType = CLIENT_TYPE.read(inputStream);

        switch (clientType) {
            case REPORTING:
            case ARCHIVING: {
                handlerHash = Registry.BYTES.read(inputStream);
                publicKey = Registry.BYTES.read(inputStream);
            }
            case LISTENING: {
                handlerName = Registry.STRING.read(inputStream);
                hostName = Registry.STRING.read(inputStream);
                hostPort = Registry.UNSIGNED_SHORT.read(inputStream);
                break;
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        CLIENT_TYPE.write(clientType, outputStream);

        switch (clientType) {
            case REPORTING:
            case ARCHIVING: {
                Registry.BYTES.write(handlerHash, outputStream);
                Registry.BYTES.write(publicKey, outputStream);
            }
            case LISTENING: {
                Registry.STRING.write(handlerName, outputStream);
                Registry.STRING.write(hostName, outputStream);
                Registry.UNSIGNED_SHORT.write(hostPort, outputStream);
                break;
            }
        }
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public byte[] getHandlerHash() {
        return handlerHash.clone();
    }

    public void setHandlerHash(byte[] handlerHash) {
        this.handlerHash = handlerHash.clone();
    }

    public byte[] getPublicKey() {
        return publicKey.clone();
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey.clone();
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getHostPort() {
        return hostPort;
    }

    public void setHostPort(int hostPort) {
        this.hostPort = hostPort;
    }

    public enum ClientType {
        LISTENING, REPORTING, ARCHIVING;
    }
}
