package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.*;
import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.jclient.packets.UpdateDataIDsPacket;
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
import java.util.function.Consumer;

public class DataHandler implements IDataProvider, IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Map<BigInteger, ChunkState> chunkStates = new HashMap<>();
    private final Map<BigInteger, RenderDistance> renderDistances = new HashMap<>();
    private final Map<BigInteger, TrackedPlayer> trackedPlayers = new HashMap<>();

    private final List<BigInteger> remoteChunkStates = new ArrayList<>();
    private final List<BigInteger> remoteRenderDistances = new ArrayList<>();
    private final List<BigInteger> remoteTrackedPlayers = new ArrayList<>();

    private BigInteger minChunkStateID;
    private BigInteger minRenderDistanceID;
    private BigInteger minTrackedPlayerID;

    private BigInteger chunkStateID;
    private BigInteger renderDistanceID;
    private BigInteger trackedPlayerID;

    private BigInteger lastChunkStateID;
    private BigInteger lastRenderDistanceID;
    private BigInteger lastTrackedPlayerID;

    public DataHandler() {
        minChunkStateID = BigInteger.ZERO;
        minRenderDistanceID = BigInteger.ZERO;
        minTrackedPlayerID = BigInteger.ZERO;

        chunkStateID = BigInteger.ZERO;
        renderDistanceID = BigInteger.ZERO;
        trackedPlayerID = BigInteger.ZERO;

        lastChunkStateID = BigInteger.ZERO;
        lastRenderDistanceID = BigInteger.ZERO;
        lastTrackedPlayerID = BigInteger.ZERO;

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
    public void onTick() {
        if (yesCom.handler != null) {
            if (!chunkStateID.equals(lastChunkStateID)) {
                yesCom.handler.onUpdateDataIDs(UpdateDataIDsPacket.DataType.CHUNK_STATE, minChunkStateID, chunkStateID);
                lastChunkStateID = chunkStateID;
            }

            if (!renderDistanceID.equals(lastRenderDistanceID)) {
                yesCom.handler.onUpdateDataIDs(UpdateDataIDsPacket.DataType.RENDER_DISTANCE, minRenderDistanceID, renderDistanceID);
                lastRenderDistanceID = renderDistanceID;
            }

            if (!trackedPlayerID.equals(lastTrackedPlayerID)) {
                yesCom.handler.onUpdateDataIDs(UpdateDataIDsPacket.DataType.TRACKED_PLAYER, minTrackedPlayerID, trackedPlayerID);
                lastTrackedPlayerID = trackedPlayerID;
            }

            if (!remoteChunkStates.isEmpty()) {
                yesCom.handler.requestChunkStatesDownload(remoteChunkStates, this::onChunkStates);
                remoteChunkStates.clear();
            }

            if (!remoteRenderDistances.isEmpty()) {
                yesCom.handler.requestRenderDistancesDownload(remoteRenderDistances, this::onRenderDistances);
                remoteRenderDistances.clear();
            }

            if (!remoteTrackedPlayers.isEmpty()) {
                yesCom.handler.requestTrackedPlayersDownload(remoteTrackedPlayers, this::onTrackedPlayers);
                remoteTrackedPlayers.clear();
            }
        }
    }

    @Override
    public void onExit() {
    }

    private void onChunkStates(boolean valid, Map<Integer, byte[]> dataParts) {

    }

    private void onRenderDistances(boolean valid, Map<Integer, byte[]> dataParts) {

    }

    private void onTrackedPlayers(boolean valid, Map<Integer, byte[]> dataParts) {

    }

    /* ------------------------ New Data Objects ------------------------ */

    public synchronized ChunkState newChunkState(ChunkState.State state, ChunkPosition chunkPosition, Dimension dimension, long foundAt) {
        ChunkState chunkState = new ChunkState(chunkStateID, state, chunkPosition, dimension, foundAt);
        chunkStates.put(chunkStateID, chunkState);

        chunkStateID = chunkStateID.add(BigInteger.ONE);

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

        return trackedPlayer;
    }

    /* ------------------------ Loaded Chunk Stuff ------------------------ */

    public void onLoaded(ChunkPosition chunkPosition, Dimension dimension) {
        if (yesCom.handler != null)
            yesCom.handler.onChunkState(newChunkState(ChunkState.State.LOADED, chunkPosition, dimension, System.currentTimeMillis()));
    }

    public void onUnloaded(ChunkPosition chunkPosition, Dimension dimension) {
        if (yesCom.handler != null)
            yesCom.handler.onChunkState(newChunkState(ChunkState.State.UNLOADED, chunkPosition, dimension, System.currentTimeMillis()));
    }

    /* ------------------------ Lookup ------------------------ */

    /**
     * Whether this client has the given chunk state locally stored.
     * @param chunkStateID The chunk state ID.
     * @return Whether it is locally stored.
     */
    public boolean hasLocalChunkState(BigInteger chunkStateID) {
        return chunkStateID.compareTo(minChunkStateID) > 0 && chunkStateID.compareTo(this.chunkStateID) <= 0;
    }

    public ChunkState getLocalChunkState(BigInteger chunkStateID) {
        if (!chunkStates.containsKey(chunkStateID)) { // TODO: Disk caching lookup
            return null;
        } else {
            return chunkStates.get(chunkStateID);
        }
    }

    public void getRemoteChunkState(BigInteger chunkStateID, Consumer<ChunkState> callBack) {

    }

    /**
     * Whether this client has the given render distance locally stored.
     * @param renderDistanceID The render distance ID.
     * @return Whether it is locally stored.
     */
    public boolean hasLocalRenderDistance(BigInteger renderDistanceID) {
        return renderDistanceID.compareTo(minRenderDistanceID) > 0 && renderDistanceID.compareTo(this.renderDistanceID) <= 0;
    }

    public RenderDistance getLocalRenderDistance(BigInteger renderDistanceID) {
        if (!renderDistances.containsKey(renderDistanceID)) {
            return null;
        } else {
            return renderDistances.get(renderDistanceID);
        }
    }

    public void getRemoteRenderDistance(BigInteger renderDistanceID, Consumer<RenderDistance> callBack) {

    }

    /**
     * Whether this client has the given tracked player locally stored.
     * @param trackedPlayerID The tracked player ID.
     * @return Whether it is locally stored.
     */
    public boolean hasLocalTrackedPlayer(BigInteger trackedPlayerID) {
        return trackedPlayerID.compareTo(minTrackedPlayerID) > 0 && trackedPlayerID.compareTo(this.trackedPlayerID) <= 0;
    }

    public TrackedPlayer getLocalTrackedPlayer(BigInteger trackedPlayerID) {
        if (!trackedPlayers.containsKey(trackedPlayerID)) {
            return null;
        } else {
            return trackedPlayers.get(trackedPlayerID);
        }
    }

    public void getRemoteTrackedPlayer(BigInteger trackedPlayerID, Consumer<TrackedPlayer> callBack) {

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
}
