package ez.pogdog.yescom.tracking.trackers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.util.ChunkPosition;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BasicTracker implements ITracker {

    private final YesCom yesCom = YesCom.getInstance();

    private final int[] X_ORDINATE_OFFSETS = new int[] { 1, -1, 0, 0 };
    private final int[] Z_ORDINATE_OFFSETS = new int[] { 0, 0, 1, -1 };

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

        if (!awaitingMovementCheck) {
            awaitingMovementCheck = true;
            centerOffset = new ChunkPosition(0, 0);

            int distance = yesCom.configHandler.BASIC_TRACKER_DIST;

            for (int index = 0; index < 4; ++index) {
                ChunkPosition offset = getOffset(index);

                IsLoadedQuery isLoadedQuery = new IsLoadedQuery(
                        trackedPlayer.getRenderDistance().getCenterPosition().add(offset.getX() * distance, offset.getZ() * distance),
                        trackedPlayer.getDimension(), IQuery.Priority.MEDIUM, yesCom.configHandler.TYPE, (query, result) -> {
                    currentQueries.remove(query);

                    if (result != IsLoadedQuery.Result.LOADED) {
                        centerOffset = centerOffset.add(offset.getX() * distance, offset.getZ() * distance);
                    } else {
                        lastLoadedChunk = System.currentTimeMillis();
                    }

                    if (currentQueries.isEmpty()) { // Done
                        awaitingMovementCheck = false;

                        int updateTimeAddition = 0;

                        if (centerOffset.equals(new ChunkPosition(0, 0)) && lastCenterOffset.equals(new ChunkPosition(0, 0))) {
                            updateTimeAddition += updateTime / 32;
                        } else {
                            updateTimeAddition -= updateTime / 4;
                        }

                        // TODO: Adjust this automatically based on speed I guess + check speed estimation works
                        updateTime = Math.max(100, Math.min(updateTime + updateTimeAddition, 3500));

                        double velocity = trackedPlayer.getTrackingData().getVelocity(System.currentTimeMillis() - 2000, System.currentTimeMillis());
                        yesCom.logger.finer(String.format("%s has estimated velocity: %.2f.", trackedPlayer, velocity));

                        yesCom.logger.finer(String.format("%s: %dms", trackedPlayer, updateTime));

                        ChunkPosition shift = new ChunkPosition(
                                (int)(centerOffset.getX() * 0.75 + lastCenterOffset.getX() * 0.25),
                                (int)(centerOffset.getZ() * 0.75 + lastCenterOffset.getZ() * 0.25));

                        trackedPlayer.setRenderDistance(yesCom.dataHandler.newRenderDistance(
                                trackedPlayer.getRenderDistance().getCenterPosition().subtract(shift),
                                yesCom.configHandler.RENDER_DISTANCE, centerOffset.getX(), centerOffset.getZ()));

                        lastCenterOffset = centerOffset;
                        trackedPlayer.getTrackingData().addRenderDistance(trackedPlayer.getRenderDistance());

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
    public List<TrackedPlayer> getTrackedPlayers() {
        return Collections.singletonList(trackedPlayer);
    }

    @Override
    public Health getHealth() {
        return Health.GOOD;
    }

    /* ------------------------ Private methods ------------------------ */

    private void doOnlineCheck() { // Could be used for something else, idk what yet tho
        if (trackedPlayer.getRenderDistance() == null) {
            yesCom.logger.warning(String.format("Couldn't do online check for %s, no render distance data.", trackedPlayer));
            return;
        }

        IsLoadedQuery isLoadedQuery = new IsLoadedQuery(
                trackedPlayer.getRenderDistance().getCenterPosition(), trackedPlayer.getDimension(), IQuery.Priority.HIGH,
                yesCom.configHandler.TYPE, (query, result) -> {
            currentQueries.remove(query);

            if (result != IsLoadedQuery.Result.LOADED) {
                yesCom.logger.fine(String.format("Failed online check for %s.", trackedPlayer));
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

    private ChunkPosition getOffset(int index) {
        return new ChunkPosition(X_ORDINATE_OFFSETS[index], Z_ORDINATE_OFFSETS[index]);
    }
}
