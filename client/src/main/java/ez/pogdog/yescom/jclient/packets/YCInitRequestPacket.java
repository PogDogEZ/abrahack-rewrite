package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="yc_init_request", id=YCRegistry.ID_OFFSET, side=Packet.Side.CLIENT)
public class YCInitRequestPacket extends Packet {

    private String handlerName;
    private String hostName;
    private int hostPort;
    private boolean listening;

    public YCInitRequestPacket(String handlerName, String hostName, int hostPort, boolean listening) {
        this.handlerName = handlerName;
        this.hostName = hostName;
        this.hostPort = hostPort;
        this.listening = listening;
    }

    public YCInitRequestPacket() {
        this("", "localhost", 25565, false);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        handlerName = Registry.STRING.read(inputStream);
        hostName = Registry.STRING.read(inputStream);
        hostPort = Registry.UNSIGNED_SHORT.read(inputStream);
        listening = Registry.BOOLEAN.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.STRING.write(handlerName, outputStream);
        Registry.STRING.write(hostName, outputStream);
        Registry.UNSIGNED_SHORT.write(hostPort, outputStream);
        Registry.BOOLEAN.write(listening, outputStream);
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

    public boolean isListening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }
}
