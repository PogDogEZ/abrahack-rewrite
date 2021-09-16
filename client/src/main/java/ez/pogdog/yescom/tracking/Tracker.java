package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.TrackingHandler;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.tracking.algorithm.BasicAlgorithm;
import ez.pogdog.yescom.tracking.algorithm.TrackingAlgorithm;
import ez.pogdog.yescom.tracking.information.TrackerPosition;
import ez.pogdog.yescom.tracking.information.Trail;
import ez.pogdog.yescom.util.BlockPosition;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main tracker class.
 */
public class Tracker implements ITracker {

    private final YesCom yesCom;
    private final TrackingHandler trackingHandler;

    private TrackerPosition position;
    private Trail trail;

    private TrackingAlgorithm currentAlgorithm;

    private List<UUID> possiblePlayers = new ArrayList<>();

    private int lostTicks;

    /**
     * Initialises the tracker with provided information.
     * @param loadedChunk The initial LoadedChunk which detected the player.
     */
    public Tracker (ChunkPosition loadedChunk, Dimension dimension) {
        yesCom = YesCom.getInstance();
        trackingHandler = yesCom.trackingHandler;

        position = new TrackerPosition(loadedChunk, dimension);
        trail = new Trail(loadedChunk);

        currentAlgorithm = new BasicAlgorithm(this, getCenterPosition(loadedChunk, dimension));

        lostTicks = 0;
    }

    public Tracker (TrackedPlayer trackedPlayer, Dimension dimension) {
        yesCom = YesCom.getInstance();
        trackingHandler = yesCom.trackingHandler;

        ChunkPosition logOutPos = trackedPlayer.getLogOutPos().getPosition();

        position = new TrackerPosition(logOutPos, dimension);
        trail = new Trail(logOutPos);

        currentAlgorithm = new BasicAlgorithm(this, getCenterPosition(logOutPos, dimension));

        possiblePlayers.addAll(trackedPlayer.getPossiblePlayers());

        lostTicks = 0;
    }

    @Override
    public TrackingHandler.TrackerTickResult onTick() {
        TrackingAlgorithm.TickResult tickResult = currentAlgorithm.onTick();

        switch (tickResult) {
            case Moved:
                onMove(currentAlgorithm.getPlayerPosition(), currentAlgorithm.getDimension());
                if (isLost())
                    onFound(currentAlgorithm.getPlayerPosition(), currentAlgorithm.getDimension());
            case Stagnant:
                break;
            case Lost:
                if (!isLost())
                    onLost();

                lostTicks++;
                break;
        }

        if (lostTicks > 0) {
            return TrackingHandler.TrackerTickResult.Lost;
        }

        return TrackingHandler.TrackerTickResult.Good;
    }

    @Override
    public void onPossibleJoin(UUID uuid) {
        if (!isLost()) {
            possiblePlayers.remove(uuid);
            return;
        }

        checkLastRecordedPos();
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

    private boolean checkLastRecordedPos() {
        AtomicBoolean loaded = new AtomicBoolean(false);
        yesCom.queryHandler.addQuery(new IsLoadedQuery(new BlockPosition(position.getX(), 0, position.getZ()),
                position.getDimension(), IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                (query, result) -> {
                    if (result == IsLoadedQuery.Result.LOADED) {
                        loaded.set(true);

                        if (isLost())
                            onFound(new ChunkPosition(position.getX(), position.getZ()), position.getDimension());
                    }
                }));

        return loaded.get();
    }

    private void onMove(ChunkPosition chunkPosition, Dimension dimension) {
        position.moved(chunkPosition, dimension);
    }

    private void onLost() {
        possiblePlayers = trackingHandler.getJoinRecords(trackingHandler.TICK_MIN);
    }

    private void onFound(ChunkPosition chunkPosition, Dimension dimension) {
        lostTicks = 0;
    }

    /* ------------------------ Public Methods ------------------------ */

    public TrackerPosition getPosition() {
        return position;
    }

    public boolean isLost() {
        return lostTicks > 0;
    }

    public int getLostTicks() {
        return lostTicks;
    }

    public List<UUID> getPossiblePlayers() {
        return possiblePlayers;
    }
}
