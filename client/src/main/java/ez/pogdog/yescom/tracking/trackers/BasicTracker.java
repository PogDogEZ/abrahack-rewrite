package ez.pogdog.yescom.tracking.trackers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.util.ChunkPosition;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BasicTracker implements ITracker {

    private final YesCom yesCom = YesCom.getInstance();

    private final Deque<IsLoadedQuery> currentQueries = new ConcurrentLinkedDeque<>();

    private final long trackerID;
    private final TrackedPlayer trackedPlayer;

    private long lastUpdate;
    private int updateTime;

    private boolean awaitingMovementCheck;
    private boolean awaitingOnlineCheck;

    private ChunkPosition centerOffset;

    private long lastLoadedChunk;

    private long lastQueryMeasure;
    private int queries;

    public BasicTracker(long trackerID, TrackedPlayer trackedPlayer) {
        this.trackerID = trackerID;
        this.trackedPlayer = trackedPlayer;

        lastUpdate = System.currentTimeMillis() - 100;
        updateTime = 100;
        lastQueryMeasure = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        if (System.currentTimeMillis() - lastUpdate < updateTime) return;
        lastUpdate = System.currentTimeMillis();

        if (!awaitingMovementCheck) {
            doMovementCheck();

            if (!awaitingOnlineCheck && System.currentTimeMillis() - lastLoadedChunk > yesCom.configHandler.BASIC_TRACKER_ONLINE_CHECK_TIME)
                doOnlineCheck();
        }
        long deltaTime = System.currentTimeMillis() - lastQueryMeasure;
        if(deltaTime > 1000) {
            lastQueryMeasure = System.currentTimeMillis();
            yesCom.logger.debug(String.format("%.1f queries per second", queries / (deltaTime / 1000.0f)));
            queries = 0;
        }
    }


    @Override
    public void onLost() {
        currentQueries.forEach(IsLoadedQuery::cancel);
    }

    @Override
    public long getTrackerID() {
        return trackerID;
    }

    @Override
    public TrackedPlayer getTrackedPlayer() {
        return trackedPlayer;
    }

    /* ------------------------ Private methods ------------------------ */

    private void doMovementCheck() {
        awaitingMovementCheck = true;
        centerOffset = new ChunkPosition(0, 0);

        int distance = yesCom.configHandler.BASIC_TRACKER_DIST + (yesCom.configHandler.BASIC_TRACKER_INVERT ? yesCom.configHandler.RENDER_DISTANCE : 0);

        for (int index = 0; index < 4; ++index) {
            ChunkPosition offset = new ChunkPosition(
                    (int)Math.pow(-1, index + 1) * distance,
                    (int)Math.pow(-1, Math.ceil((index + 1) / 2.0f)) * distance);

            ++queries;
            IsLoadedQuery isLoadedQuery = new IsLoadedQuery(
                    trackedPlayer.getRenderDistance().getCenterPosition().add(offset), trackedPlayer.getDimension(),
                    IQuery.Priority.MEDIUM, yesCom.configHandler.TYPE, (query, result) -> {
                currentQueries.remove(query);

                if (result != IsLoadedQuery.Result.LOADED) {
                    ChunkPosition newOffset = new ChunkPosition(
                            Math.max(-yesCom.configHandler.BASIC_TRACKER_DIST, Math.min(yesCom.configHandler.BASIC_TRACKER_DIST,
                                    offset.getX())) * 2,
                            Math.max(-yesCom.configHandler.BASIC_TRACKER_DIST, Math.min(yesCom.configHandler.BASIC_TRACKER_DIST,
                                    offset.getZ())) * 2);

                    if (yesCom.configHandler.BASIC_TRACKER_INVERT) {
                        centerOffset = centerOffset.subtract(newOffset);
                    } else {
                        centerOffset = centerOffset.add(newOffset);
                    }
                } else {
                    lastLoadedChunk = System.currentTimeMillis();
                }

                if (currentQueries.isEmpty() || (currentQueries.size() == 1 && awaitingOnlineCheck)) { // Done
                    awaitingMovementCheck = false;

                    trackedPlayer.setRenderDistance(new RenderDistance(
                            trackedPlayer.getRenderDistance().getCenterPosition().subtract(centerOffset),
                            yesCom.configHandler.RENDER_DISTANCE,
                            (float)(Math.pow(yesCom.configHandler.RENDER_DISTANCE, 2) / centerOffset.getX()),
                            (float)(Math.pow(yesCom.configHandler.RENDER_DISTANCE, 2) / centerOffset.getZ())));
                    // trackedPlayer.getTrackingData().addRenderDistance(trackedPlayer.getRenderDistance());
                }
            });
            currentQueries.add(isLoadedQuery);
            yesCom.queryHandler.addQuery(isLoadedQuery);
        }
    }

    private void doOnlineCheck() {
        awaitingOnlineCheck = true;
        if (trackedPlayer.getRenderDistance() == null) {
            yesCom.logger.warn(String.format("Couldn't do online check for %s, no render distance data.", trackedPlayer));
            return;
        }

        ++queries;
        IsLoadedQuery isLoadedQuery = new IsLoadedQuery(
                trackedPlayer.getRenderDistance().getCenterPosition(), trackedPlayer.getDimension(), IQuery.Priority.HIGH,
                yesCom.configHandler.TYPE, (query, result) -> {
                    currentQueries.remove(query);

                    if (result != IsLoadedQuery.Result.LOADED) {
                        yesCom.logger.debug(String.format("Failed online check for %s.", trackedPlayer));
                        if(yesCom.trackingHandler.getTracker(trackerID) != null){
                            yesCom.trackingHandler.trackPanic(trackedPlayer);
                        }
                    } else {
                        lastLoadedChunk = System.currentTimeMillis();
                    }

                    awaitingOnlineCheck = false;
                });
        currentQueries.add(isLoadedQuery);
        yesCom.queryHandler.addQuery(isLoadedQuery);
    }
}
