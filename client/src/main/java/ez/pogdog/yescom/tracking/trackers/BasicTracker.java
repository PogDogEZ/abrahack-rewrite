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

    private ChunkPosition centerOffset;
    private ChunkPosition lastCenterOffset;

    private long lastLoadedChunk;

    public BasicTracker(long trackerID, TrackedPlayer trackedPlayer) {
        this.trackerID = trackerID;
        this.trackedPlayer = trackedPlayer;

        lastUpdate = System.currentTimeMillis() - 100;
        updateTime = 100; // Start off fast initially as we don't know how fast they are travelling

        centerOffset = new ChunkPosition(0, 0);
        lastCenterOffset = new ChunkPosition(0, 0);
    }

    @Override
    public void onTick() {
        if (System.currentTimeMillis() - lastUpdate < updateTime) return;
        lastUpdate = System.currentTimeMillis();

        if (!awaitingMovementCheck) doMovementCheck();
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

        int distance = yesCom.configHandler.BASIC_TRACKER_DIST;

        for (int index = 0; index < 4; ++index) {
            ChunkPosition offset = new ChunkPosition(
                    (int)Math.pow(-1, index + 1) * distance,
                    (int)Math.pow(-1, Math.ceil((index + 1) / 2.0f)) * distance);

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

                    centerOffset = centerOffset.add(newOffset);
                } else {
                    lastLoadedChunk = System.currentTimeMillis();
                }

                if (currentQueries.isEmpty()) { // Done
                    awaitingMovementCheck = false;

                    int updateTimeAddition = 0;

                    if (centerOffset.equals(new ChunkPosition(0, 0)) && lastCenterOffset.equals(new ChunkPosition(0, 0))) {
                        updateTimeAddition += updateTime / 4;
                    } else {
                        updateTimeAddition -= updateTime / 4;
                    }

                    // TODO: Adjust this automatically based on speed I guess + check speed estimation works
                    updateTime = Math.max(100, Math.min(updateTime + updateTimeAddition, 3500));

                    double velocity = trackedPlayer.getTrackingData().getVelocity(System.currentTimeMillis() - 2000, System.currentTimeMillis());
                    yesCom.logger.debug(String.format("%s has estimated velocity: %.2f.", trackedPlayer, velocity));

                    ChunkPosition shift = new ChunkPosition(
                            (int)(centerOffset.getX() * 0.75 + lastCenterOffset.getX() * 0.25),
                            (int)(centerOffset.getZ() * 0.75 + lastCenterOffset.getZ() * 0.25));

                    trackedPlayer.setRenderDistance(yesCom.dataHandler.newRenderDistance(
                            trackedPlayer.getRenderDistance().getCenterPosition().subtract(shift),
                            yesCom.configHandler.RENDER_DISTANCE, centerOffset.getX(), centerOffset.getZ()));

                    lastCenterOffset = centerOffset;
                    // trackedPlayer.getTrackingData().addRenderDistance(trackedPlayer.getRenderDistance());

                    if (System.currentTimeMillis() - lastLoadedChunk > yesCom.configHandler.BASIC_TRACKER_ONLINE_CHECK_TIME) {
                        yesCom.logger.info(String.format("Failed online check for %s.", trackedPlayer));
                        // Avoid duplicates
                        if (yesCom.trackingHandler.getTracker(trackerID) != null) yesCom.trackingHandler.trackPanic(trackedPlayer);
                    }
                }
            });

            currentQueries.add(isLoadedQuery);
            yesCom.queryHandler.addQuery(isLoadedQuery);
        }
    }

    private void doOnlineCheck() { // Could be used for something else, idk what yet tho
        if (trackedPlayer.getRenderDistance() == null) {
            yesCom.logger.warn(String.format("Couldn't do online check for %s, no render distance data.", trackedPlayer));
            return;
        }

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
        });

        currentQueries.add(isLoadedQuery);
        yesCom.queryHandler.addQuery(isLoadedQuery);
    }
}
