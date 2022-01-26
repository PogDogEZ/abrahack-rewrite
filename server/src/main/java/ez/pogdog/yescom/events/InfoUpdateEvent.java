package ez.pogdog.yescom.events;

import ez.pogdog.yescom.network.handlers.YCReporter;

public class InfoUpdateEvent extends ReporterEvent {

    private final int waitingQueries;
    private final int tickingQueries;
    private final float queryRate;
    private final float droppedQueries;
    private final boolean isConnected;
    private final float tickRate;
    private final float serverPing;
    private final int timeSinceLastPacket;

    public InfoUpdateEvent(YCReporter reporter, int waitingQueries, int tickingQueries, float queryRate, float droppedQueries,
                           boolean isConnected, float tickRate, float serverPing, int timeSinceLastPacket) {
        super(reporter);

        this.waitingQueries = waitingQueries;
        this.tickingQueries = tickingQueries;
        this.queryRate = queryRate;
        this.droppedQueries = droppedQueries;
        this.isConnected = isConnected;
        this.tickRate = tickRate;
        this.serverPing = serverPing;
        this.timeSinceLastPacket = timeSinceLastPacket;
    }

    public int getWaitingQueries() {
        return waitingQueries;
    }

    public int getTickingQueries() {
        return tickingQueries;
    }

    public float getQueryRate() {
        return queryRate;
    }

    public float getDroppedQueries() {
        return droppedQueries;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public float getTickRate() {
        return tickRate;
    }

    public float getServerPing() {
        return serverPing;
    }

    public int getTimeSinceLastPacket() {
        return timeSinceLastPacket;
    }
}
