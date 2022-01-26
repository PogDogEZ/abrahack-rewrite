package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.*;
import ez.pogdog.yescom.data.serializable.*;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataHandler implements IDataProvider, IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Map<Long, ServerStats> serverStats = new HashMap<>();

    private final Map<BigInteger, ChunkState> chunkStates = new HashMap<>();
    private final Map<BigInteger, RenderDistance> renderDistances = new HashMap<>();
    private final Map<BigInteger, TrackedPlayer> trackedPlayers = new HashMap<>();

    private final Map<BigInteger, String> logData = new HashMap<>();
    private final Map<BigInteger, ChatMessage> chatData = new HashMap<>();

    private long earliestStatData;
    private long latestStatData;

    private BigInteger chunkStateID;
    private BigInteger renderDistanceID;
    private BigInteger trackedPlayerID;

    private BigInteger logID;
    private BigInteger chatID;

    private long lastUpdate;

    public DataHandler() {
        earliestStatData = Long.MAX_VALUE;
        latestStatData = Long.MIN_VALUE;

        chunkStateID = BigInteger.ZERO;
        renderDistanceID = BigInteger.ZERO;
        trackedPlayerID = BigInteger.ZERO;

        logID = BigInteger.ZERO;
        chatID = BigInteger.ZERO;

        lastUpdate = System.currentTimeMillis();

        createDirectories();
    }

    @Override
    public ChunkState getChunkState(BigInteger chunkStateID) {
        if (hasLocalChunkState(chunkStateID)) {
            return getLocalChunkState(chunkStateID);
        } else {
            return null; // TODO: Some other way around this?
        }
    }

    @Override
    public RenderDistance getRenderDistance(BigInteger renderDistanceID) {
        if (hasLocalRenderDistance(renderDistanceID)) {
            return getLocalRenderDistance(renderDistanceID);
        } else {
            return null;
        }
    }

    @Override
    public TrackedPlayer getTrackedPlayer(BigInteger trackedPlayerID) {
        if (hasLocalTrackedPlayer(trackedPlayerID)) {
            return getLocalTrackedPlayer(trackedPlayerID);
        } else {
            return null;
        }
    }

    @Override
    public void tick() {
        /*
        while (!yesCom.logger.messages.isEmpty()) {
            Message message = yesCom.logger.messages.remove(0);
            newLogMessage(String.format("[%s] %s", message.getLevel().name(), message.getText()));
        }
         */

        if (System.currentTimeMillis() - lastUpdate > yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL) {
            lastUpdate = System.currentTimeMillis();

            if (lastUpdate < earliestStatData) earliestStatData = lastUpdate;
            if (lastUpdate > latestStatData) latestStatData = lastUpdate;

            serverStats.put(System.currentTimeMillis() / yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL,
                    new ServerStats(System.currentTimeMillis(), yesCom.connectionHandler.getMeanTickRate(),
                            yesCom.connectionHandler.getMeanServerPing(), yesCom.connectionHandler.getTimeSinceLastPacket()));
        }
    }

    @Override
    public void exit() {
    }

    /* ------------------------ New Data Objects ------------------------ */

    public synchronized ChunkState newChunkState(ChunkState.State state, ChunkPosition chunkPosition, Dimension dimension, long foundAt) {
        ChunkState chunkState = new ChunkState(chunkStateID, state, chunkPosition, dimension, foundAt);
        chunkStates.put(chunkStateID, chunkState);

        chunkStateID = chunkStateID.add(BigInteger.ONE);

        if (yesCom.ycHandler != null) yesCom.ycHandler.onChunkState(chunkState); // FIXME: Is there a better place for this stuff?
        return chunkState;
    }

    public synchronized RenderDistance newRenderDistance(ChunkPosition centerPosition, int renderDist, float errorX, float errorZ) {
        RenderDistance renderDistance = new RenderDistance(renderDistanceID, centerPosition, renderDist, errorX, errorZ);
        renderDistances.put(renderDistanceID, renderDistance);

        renderDistanceID = renderDistanceID.add(BigInteger.ONE);

        return renderDistance;
    }

    public synchronized TrackedPlayer newTrackedPlayer(TrackedPlayer.TrackingData trackingData, RenderDistance renderDistance,
                                                       Dimension dimension, boolean loggedOut, long foundAt) {
        TrackedPlayer trackedPlayer = new TrackedPlayer(trackedPlayerID, trackingData, renderDistance, dimension, loggedOut, foundAt);
        trackedPlayers.put(trackedPlayerID, trackedPlayer);

        trackedPlayerID = trackedPlayerID.add(BigInteger.ONE);

        if (yesCom.ycHandler != null) yesCom.ycHandler.onTrackedPlayer(trackedPlayer);
        return trackedPlayer;
    }

    /**
     * Adds a new log entry.
     * @param message The log message.
     */
    public synchronized void newLogMessage(String message) { // FIXME: Better format for log messages
        logData.put(logID, message);
        logID = logID.add(BigInteger.ONE);
    }

    /**
     * Adds a new chat message.
     * @param username The username of the account that received the chat message.
     * @param message The chat message.
     * @return The serializable chat message.
     */
    public synchronized ChatMessage newChatMessage(String username, String message, long timestamp) {
        ChatMessage chatMessage = new ChatMessage(chatID, username, message, timestamp);

        chatData.put(chatID, chatMessage);
        chatID = chatID.add(BigInteger.ONE);

        if (yesCom.ycHandler != null) yesCom.ycHandler.onChatMessage(chatMessage);
        return chatMessage;
    }

    /* ------------------------ Lookup ------------------------ */

    public long clampStatStartTime(long startTime) {
        return Math.min(Math.max(earliestStatData, startTime), latestStatData) / yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL;
    }

    public long clampStatEndTime(long endTime) {
        return Math.min(Math.max(earliestStatData, endTime), latestStatData) / yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL;
    }

    public List<ServerStats> getServerStats(long startTime, long endTime) {
        startTime = clampStatStartTime(startTime);
        endTime = clampStatEndTime(endTime);

        List<ServerStats> stats = new ArrayList<>();
        for (long index = startTime; index < endTime; ++index) stats.add(serverStats.get(index));

        return stats;
    }

    public List<Float> getTickData(long startTime, long endTime) {
        return getServerStats(startTime, endTime).stream().map(ServerStats::getTPS).collect(Collectors.toList());
    }

    public List<Float> getPingData(long startTime, long endTime) {
        return getServerStats(startTime, endTime).stream().map(ServerStats::getPING).collect(Collectors.toList());
    }

    public List<Float> getTSLPData(long startTime, long endTime) {
        return getServerStats(startTime, endTime).stream().map(ServerStats::getTSLP).collect(Collectors.toList());
    }

    /**
     * Whether this client has the given chunk state locally stored.
     * @param chunkStateID The chunk state ID.
     * @return Whether it is locally stored.
     */
    public boolean hasLocalChunkState(BigInteger chunkStateID) {
        return chunkStateID.compareTo(this.chunkStateID) <= 0; // FIXME: Yeah
    }

    public ChunkState getLocalChunkState(BigInteger chunkStateID) {
        if (!chunkStates.containsKey(chunkStateID)) { // TODO: Disk caching lookup
            return null;
        } else {
            return chunkStates.get(chunkStateID);
        }
    }

    /**
     * Whether this client has the given render distance locally stored.
     * @param renderDistanceID The render distance ID.
     * @return Whether it is locally stored.
     */
    public boolean hasLocalRenderDistance(BigInteger renderDistanceID) {
        return renderDistanceID.compareTo(this.renderDistanceID) <= 0;
    }

    public RenderDistance getLocalRenderDistance(BigInteger renderDistanceID) {
        if (!renderDistances.containsKey(renderDistanceID)) {
            return null;
        } else {
            return renderDistances.get(renderDistanceID);
        }
    }

    /**
     * Whether this client has the given tracked player locally stored.
     * @param trackedPlayerID The tracked player ID.
     * @return Whether it is locally stored.
     */
    public boolean hasLocalTrackedPlayer(BigInteger trackedPlayerID) {
        return trackedPlayerID.compareTo(this.trackedPlayerID) <= 0;
    }

    public TrackedPlayer getLocalTrackedPlayer(BigInteger trackedPlayerID) {
        if (!trackedPlayers.containsKey(trackedPlayerID)) {
            return null;
        } else {
            return trackedPlayers.get(trackedPlayerID);
        }
    }

    public boolean hasLogMessage(BigInteger logID) {
        return logID.compareTo(this.logID) <= 0;
    }

    public String getLogMessage(BigInteger logID) {
        if (!logData.containsKey(logID)) {
            return null;
        } else {
            return logData.get(logID);
        }
    }

    public boolean hasChatMessage(BigInteger chatID) {
        return chatID.compareTo(this.chatID) <= 0;
    }

    public ChatMessage getChatMessage(BigInteger chatID) {
        if (!chatData.containsKey(chatID)) {
            return null;
        } else {
            return chatData.get(chatID);
        }
    }

    /* ------------------------ Uncompressed Saving & Private Methods ------------------------ */

    /**
     * TODO: This can probably be removed later, but is useful for testing and shit
     * @param data Any string you want to store uncompressed
     */
    public void saveUncompressed(String data, String filename) {
        String directory = "data/uncompressed/" + filename + ".txt";
        File file = new File(directory);
        try {
            FileWriter writer = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(data);
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDirectories() {
        new File(yesCom.configHandler.MAIN_DIRECTORY).mkdirs();
        new File(yesCom.configHandler.RAW_DIRECTORY).mkdirs();
        new File(yesCom.configHandler.PLAYER_DIRECTORY).mkdirs();
        new File(yesCom.configHandler.TRACKERS_DIRECTORY).mkdirs();
        new File("data/uncompressed").mkdirs();
    }

    /* ------------------------ Setters and getters ------------------------ */

    public long getEarliestStatData() {
        return earliestStatData;
    }

    public long getLatestStatData() {
        return latestStatData;
    }

    public BigInteger getChunkStateID() {
        return chunkStateID;
    }

    public BigInteger getRenderDistanceID() {
        return renderDistanceID;
    }

    public BigInteger getTrackedPlayerID() {
        return trackedPlayerID;
    }

    public BigInteger getLogID() {
        return logID;
    }

    public BigInteger getChatID() {
        return chatID;
    }
}
