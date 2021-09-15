package ez.pogdog.yescom.tracking.algorithm;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.tracking.Tracker;
import ez.pogdog.yescom.tracking.information.TrackerPosition;
import ez.pogdog.yescom.util.BlockPosition;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.concurrent.atomic.AtomicBoolean;

abstract public class TrackingAlgorithm {

    protected final Tracker TRACKER;

    protected final int RENDER_DISTANCE = 6;

    /**
     * This is used for predictive algorithms, it can be synced to the TRACKER's position with trackerPositionSync.
     */
    protected TrackerPosition position;
    private final boolean trackerPositionSync;

    TrackingAlgorithm(Tracker tracker, ChunkPosition playerChunk, Dimension dimension ,boolean trackerPositionSync) {
        this.TRACKER = tracker;
        this.trackerPositionSync = trackerPositionSync;

        if (trackerPositionSync) {
            position = tracker.getPosition();
        } else {
            position = new TrackerPosition(playerChunk, dimension);
        }
    }

    /* ------------------------ Abstract Methods ---------------------- */

    public abstract TickResult onTick();

    public abstract ChunkPosition getPlayerPosition();

    public abstract Dimension getDimension();

    /* ------------------------ Public Methods ------------------------ */

    public void onTrackerMove(TrackerPosition trackerPosition) {
        if (trackerPositionSync)
            position.moved(trackerPosition.getPosition(), trackerPosition.getDimension());
    }

    /**
     * Searches a 60 chunk radius around the specified position, this is used to relocate the player if we lose him.
     * Also used for dimension changes, assuming we detect the player vanishing from the current dimension in 15 seconds, they would
     * have to travel 64 blocks per second to escape the tracker.
     * This process uses 25 queries.
     * @param position Last recorded position.
     * @param dimension Dimension to check in.
     */
    public void scanForPlayer(ChunkPosition position, Dimension dimension) {
        AtomicBoolean foundChunk = new AtomicBoolean(false);
        for (int chunkX = position.getX() - 30; position.getX() + 30 >= chunkX; chunkX += RENDER_DISTANCE*2) {
            for (int chunkZ = position.getZ() - 30; position.getZ() + 30 >= chunkZ; chunkZ += RENDER_DISTANCE*2) {
                YesCom.getInstance().queryHandler.addQuery(new IsLoadedQuery(new BlockPosition(chunkX, 0, chunkZ),
                        dimension, IQuery.Priority.HIGH, YesCom.getInstance().configHandler.TYPE,
                        (query, result) -> {
                                if (result == IsLoadedQuery.Result.LOADED) {
                                    onPlayerFound(getRenderDistanceCenter(query.getChunkPosition(), dimension), dimension);
                                    foundChunk.set(true);
                            }
                        }));

                if (foundChunk.get())
                    return;
            }
        }
    }

    public ChunkPosition getRenderDistanceCenter(ChunkPosition loadedChunk, Dimension dimension) {
        return new ChunkPosition(0, 0);
    }

    public void onPlayerFound(ChunkPosition chunkPosition, Dimension dimension) {
        if (!trackerPositionSync)
            position.moved(chunkPosition, dimension);
    }

    public boolean isTrackerPositionSync() {
        return trackerPositionSync;
    }

    public enum TickResult {
        Moved, Stagnant, Lost;
    }
}
