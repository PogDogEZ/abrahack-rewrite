package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="yc_init_response", id=YCRegistry.ID_OFFSET + 1, side=Packet.Side.SERVER)
public class YCInitResponsePacket extends Packet {

    private boolean rejected;
    private int handlerID;
    private String message;
    private String hostName;
    private int hostPort;

    public YCInitResponsePacket(boolean rejected, int handlerID, String message, String hostName, int hostPort) {
        this.rejected = rejected;
        this.handlerID = handlerID;
        this.message = message;
        this.hostName = hostName;
        this.hostPort = hostPort;
    }

    public YCInitResponsePacket() {
        this(false, 0, "", "localhost", 25565);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        rejected = Registry.BOOLEAN.read(inputStream);
        handlerID = Registry.UNSIGNED_SHORT.read(inputStream);
        message = Registry.STRING.read(inputStream);

        if (!rejected) {
            hostName = Registry.STRING.read(inputStream);
            hostPort = Registry.UNSIGNED_SHORT.read(inputStream);
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BOOLEAN.write(rejected, outputStream);
        Registry.UNSIGNED_SHORT.write(handlerID, outputStream);
        Registry.STRING.write(message, outputStream);

        if (!rejected) {
            Registry.STRING.write(hostName, outputStream);
            Registry.UNSIGNED_SHORT.write(hostPort, outputStream);
        }
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public int getHandlerID() {
        return handlerID;
    }

    public void setHandlerID(int handlerID) {
        this.handlerID = handlerID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
}
