package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;

/**
 * Handles general information, such as online players, tickrate, packet loss, etc...
 */
public class InfoHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    @Override
    public void tick() {
        boolean connected = yesCom.connectionHandler.isConnected();

        int waitingSize = yesCom.queryHandler.getWaitingSize();
        int tickingSize = yesCom.queryHandler.getTickingSize();
        float queryRate = yesCom.queryHandler.getQueryRate();
        float droppedQueries = yesCom.queryHandler.getDroppedQueries();

        if (yesCom.ycHandler != null) { // No need to notify the server, if we aren't connected
            if (connected) {
                yesCom.ycHandler.onInfoUpdate(waitingSize, tickingSize, queryRate, droppedQueries,
                        yesCom.connectionHandler.getMeanTickRate(), yesCom.connectionHandler.getMeanServerPing(),
                        yesCom.connectionHandler.getTimeSinceLastPacket());
            } else {
                yesCom.ycHandler.onInfoUpdate(waitingSize, tickingSize, queryRate, droppedQueries);
            }
        }
    }

    @Override
    public void exit() {
    }
}
