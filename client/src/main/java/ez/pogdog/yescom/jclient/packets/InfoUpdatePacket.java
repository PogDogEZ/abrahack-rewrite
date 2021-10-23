package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Packet.Info(name="info_update", id=YCRegistry.ID_OFFSET + 15, side=Packet.Side.CLIENT)
public class InfoUpdatePacket extends Packet {

    private int waitingQueries;
    private int tickingQueries;
    private boolean isConnected;
    private float tickRate;
    private int timeSinceLastPacket;

    public InfoUpdatePacket(int waitingQueries, int tickingQueries, boolean isConnected, float tickRate,
                            int timeSinceLastPacket) {
        this.waitingQueries = waitingQueries;
        this.tickingQueries = tickingQueries;
        this.isConnected = isConnected;
        this.tickRate = tickRate;
        this.timeSinceLastPacket = timeSinceLastPacket;
    }

    public InfoUpdatePacket(int waitingQueries, int tickingQueries, float tickRate, int timeSinceLastPacket) {
        this(waitingQueries, tickingQueries, true, tickRate, timeSinceLastPacket);
    }

    public InfoUpdatePacket(int waitingQueries, int tickingQueries) {
        this(waitingQueries, tickingQueries, false, 20.0f, 0);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        waitingQueries = Registry.UNSIGNED_SHORT.read(inputStream);
        tickingQueries = Registry.UNSIGNED_SHORT.read(inputStream);
        isConnected = Registry.BOOLEAN.read(inputStream);

        if (isConnected) {
            tickRate = Registry.FLOAT.read(inputStream);
            timeSinceLastPacket = Registry.UNSIGNED_SHORT.read(inputStream);
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(waitingQueries, outputStream);
        Registry.UNSIGNED_SHORT.write(tickingQueries, outputStream);
        Registry.BOOLEAN.write(isConnected, outputStream);

        if (isConnected) {
            Registry.FLOAT.write(tickRate, outputStream);
            Registry.UNSIGNED_SHORT.write(timeSinceLastPacket, outputStream);
        }
    }

    public int getWaitingQueries() {
        return waitingQueries;
    }

    public void setWaitingQueries(int waitingQueries) {
        this.waitingQueries = waitingQueries;
    }

    public int getTickingQueries() {
        return tickingQueries;
    }

    public void setTickingQueries(int tickingQueries) {
        this.tickingQueries = tickingQueries;
    }

    public boolean getIsConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public float getTickRate() {
        return tickRate;
    }

    public void setTickRate(float tickRate) {
        this.tickRate = tickRate;
    }

    public int getTimeSinceLastPacket() {
        return timeSinceLastPacket;
    }

    public void setTimeSinceLastPacket(int timeSinceLastPacket) {
        this.timeSinceLastPacket = timeSinceLastPacket;
    }
}
