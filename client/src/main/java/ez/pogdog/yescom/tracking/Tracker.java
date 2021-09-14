package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.handlers.TrackingHandler;
import ez.pogdog.yescom.tracking.algorithm.BasicAlgorithm;
import ez.pogdog.yescom.tracking.algorithm.TrackingAlgorithm;
import ez.pogdog.yescom.tracking.information.TrackerPosition;
import ez.pogdog.yescom.tracking.information.Trail;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Main tracker class.
 */
public class Tracker implements ITracker {

    private TrackerPosition position;
    private Trail trail;

    private TrackingAlgorithm currentAlgorithm;

    private ArrayList<UUID> possiblePlayers = new ArrayList<>();

    private boolean lost;
    private int lostTicks;

    /**
     * Initialises the tracker with provided information.
     * @param loadedChunk The initial LoadedChunk which detected the player.
     */
    public Tracker (ChunkPosition loadedChunk, Dimension dimension) {
        position = new TrackerPosition(loadedChunk, dimension);
        trail = new Trail(loadedChunk);

        currentAlgorithm = new BasicAlgorithm(this, getCenterPosition(loadedChunk, dimension));

        lost = false;
        lostTicks = 0;
    }

    public Tracker (TrackedPlayer trackedPlayer, Dimension dimension) {
        ChunkPosition logOutPos = trackedPlayer.getLogOutPos();

        position = new TrackerPosition(logOutPos, dimension);
        trail = new Trail(logOutPos);

        currentAlgorithm = new BasicAlgorithm(this, getCenterPosition(logOutPos, dimension));

        possiblePlayers.addAll(trackedPlayer.getPossiblePlayers());

        lost = false;
        lostTicks = 0;
    }

    @Override
    public TrackingHandler.TrackerTickResult onTick() {
        TrackingAlgorithm.TickResult tickResult = currentAlgorithm.onTick();

        switch (tickResult) {
            case Moved:
                onMove(currentAlgorithm.getPlayerPosition(), currentAlgorithm.getDimension());
            case Stagnant:
                break;
            case Lost:
                lostTicks++;
                break;
        }

        if (lostTicks > 0) {
            if (!lost)
                return TrackingHandler.TrackerTickResult.Found;

            return TrackingHandler.TrackerTickResult.Lost;
        }

        return TrackingHandler.TrackerTickResult.Good;
    }


    /* ------------------------ Private Methods ------------------------ */

    /**
     * Calculates the center of the target's render distance.
     * @param loadedChunk A chunk in the target's render distance.
     * @return The chunk in the middle of the target's render distance.
     */
    private ChunkPosition getCenterPosition(ChunkPosition loadedChunk, Dimension dimension) {
        return new ChunkPosition(0, 0);
    }

    private void onMove(ChunkPosition chunkPosition, Dimension dimension) {
        if (lost) {
            onFound(chunkPosition, dimension);
        }
        position.moved(chunkPosition, dimension);
        currentAlgorithm.onTrackerMove(position);
    }

    private void onFound(ChunkPosition chunkPosition, Dimension dimension) {
        lost = false;
    }

    /* ------------------------ Public Methods ------------------------ */

    public TrackerPosition getPosition() {
        return position;
    }

    public int getLostTicks() {
        return lostTicks;
    }

    public ArrayList<UUID> getPossiblePlayers() {
        return possiblePlayers;
    }
}
