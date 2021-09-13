package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.tracking.algorithm.BasicAlgorithm;
import ez.pogdog.yescom.tracking.algorithm.ITrackingAlgorithm;
import ez.pogdog.yescom.tracking.information.TrackerPosition;
import ez.pogdog.yescom.tracking.information.Trail;
import ez.pogdog.yescom.util.ChunkPosition;

import java.util.UUID;

/**
 * Main tracker class.
 */
public class Tracker {

    private final UUID uuid;

    private TrackerPosition position;
    private Trail trail;

    private ITrackingAlgorithm algorithm;

    /**
     * Initialises the tracker with provided information.
     * @param loadedChunk The initial LoadedChunk which detected the player.
     */
    public Tracker (ChunkPosition loadedChunk) {
        uuid = UUID.randomUUID();

        position = new TrackerPosition(loadedChunk);
        trail = new Trail(loadedChunk);

        algorithm = new BasicAlgorithm(getCenterPosition(loadedChunk));
    }

    public Tracker (TrackedPlayer trackedPlayer) {
        uuid = trackedPlayer.getMostLikelyPlayer();

        ChunkPosition logOutPos = trackedPlayer.getLogOutPos();

        position = new TrackerPosition(logOutPos);
        trail = new Trail(logOutPos);

        algorithm = new BasicAlgorithm(getCenterPosition(logOutPos));
    }


    /* ------------------------ Private Methods ------------------------ */

    /**
     * Calculates the center of the target's render distance.
     * @param loadedChunk A chunk in the target's render distance.
     * @return The chunk in the middle of the target's render distance.
     */
    private ChunkPosition getCenterPosition(ChunkPosition loadedChunk) {
        return new ChunkPosition(0, 0);
    }
}
