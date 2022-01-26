package ez.pogdog.yescom.network.packets.shared;

import ez.pogdog.yescom.network.YCRegistry;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Notifies the client about certain information.
 */
@Packet.Info(name="info_update", id=YCRegistry.ID_OFFSET + 9, side=Packet.Side.BOTH)
public class InfoUpdatePacket extends Packet {

    private int waitingQueries;
    private int tickingQueries;
    private float queryRate;
    private float droppedQueries;
    private boolean isConnected;
    private float tickRate;
    private float serverPing;
    private int timeSinceLastPacket;

    public InfoUpdatePacket(int waitingQueries, int tickingQueries, float queryRate, float droppedQueries, boolean isConnected,
                            float tickRate, float serverPing, int timeSinceLastPacket) {
        this.waitingQueries = waitingQueries;
        this.tickingQueries = tickingQueries;
        this.queryRate = queryRate;
        this.droppedQueries = droppedQueries;
        this.isConnected = isConnected;
        this.tickRate = tickRate;
        this.serverPing = serverPing;
        this.timeSinceLastPacket = timeSinceLastPacket;
    }

    public InfoUpdatePacket(int waitingQueries, int tickingQueries, float queryRate, float droppedQueries, float tickRate,
                            float serverPing, int timeSinceLastPacket) {
        this(waitingQueries, tickingQueries, queryRate, droppedQueries, true, tickRate, serverPing, timeSinceLastPacket);
    }

    public InfoUpdatePacket(int waitingQueries, int tickingQueries, float queryRate, float droppedQueries) {
        this(waitingQueries, tickingQueries, queryRate, droppedQueries, false, 20.0f, 0.0f, 0);
    }

    public InfoUpdatePacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        waitingQueries = Registry.UNSIGNED_SHORT.read(inputStream);
        tickingQueries = Registry.UNSIGNED_SHORT.read(inputStream);
        queryRate = Registry.FLOAT.read(inputStream);
        droppedQueries = Registry.FLOAT.read(inputStream);
        isConnected = Registry.BOOLEAN.read(inputStream);

        if (isConnected) {
            tickRate = Registry.FLOAT.read(inputStream);
            serverPing = Registry.FLOAT.read(inputStream);
            timeSinceLastPacket = Registry.UNSIGNED_SHORT.read(inputStream);
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(waitingQueries, outputStream);
        Registry.UNSIGNED_SHORT.write(tickingQueries, outputStream);
        Registry.FLOAT.write(queryRate, outputStream);
        Registry.FLOAT.write(droppedQueries, outputStream);
        Registry.BOOLEAN.write(isConnected, outputStream);

        if (isConnected) {
            Registry.FLOAT.write(tickRate, outputStream);
            Registry.FLOAT.write(serverPing, outputStream);
            Registry.UNSIGNED_SHORT.write(timeSinceLastPacket, outputStream);
        }
    }

    /**
     * @return The number of queries waiting to be processed.
     */
    public int getWaitingQueries() {
        return waitingQueries;
    }

    public void setWaitingQueries(int waitingQueries) {
        this.waitingQueries = waitingQueries;
    }

    /**
     * @return The number of queries currently being processed / ticked.
     */
    public int getTickingQueries() {
        return tickingQueries;
    }

    public void setTickingQueries(int tickingQueries) {
        this.tickingQueries = tickingQueries;
    }

    /**
     * @return The current QPS rate.
     */
    public float getQueryRate() {
        return queryRate;
    }

    public void setQueryRate(float queryRate) {
        this.queryRate = queryRate;
    }

    public float getDroppedQueries() {
        return droppedQueries;
    }

    public void setDroppedQueries(float droppedQueries) {
        this.droppedQueries = droppedQueries;
    }

    /**
     * @return Whether or not the client is connected to the Minecraft server.
     */
    public boolean getIsConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    /**
     * @return The Minecraft server's tickrate.
     */
    public float getTickRate() {
        return tickRate;
    }

    public void setTickRate(float tickRate) {
        this.tickRate = tickRate;
    }
    
    /**
     * @return The ping to the Minecraft server, as measured by the server.
     */
    public float getServerPing() {
        return serverPing;
    }
    
    public void setServerPing(float serverPing) {
        this.serverPing = serverPing;
    }

    /**
     * @return The time since the last packet was received from the Minecraft server.
     */
    public int getTimeSinceLastPacket() {
        return timeSinceLastPacket;
    }

    public void setTimeSinceLastPacket(int timeSinceLastPacket) {
        this.timeSinceLastPacket = timeSinceLastPacket;
    }
}
