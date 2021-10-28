package ez.pogdog.yescom.tracking.trackers.adaptive;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.util.ChunkPosition;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class AdaptiveTracker implements ITracker {

    private final YesCom yesCom = YesCom.getInstance();

    private final IPhase WALKING_PHASE = new BasicPhase(1000, Arrays.asList(
            new BasicPhase.Offset(1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(-1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(0.0f, 1.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(0.0f, -1.0f, IsLoadedQuery.Result.LOADED)));
    private final IPhase GEZA_PHASE = new BasicPhase(1250, Arrays.asList(
            new BasicPhase.Offset(1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(-1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(0.0f, 1.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(0.0f, -1.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(2.75f, 0.0f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(-2.75f, 0.0f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(0.0f, 2.75f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(0.0f, -2.75f, IsLoadedQuery.Result.UNLOADED)));
    private final IPhase FATALE_PHASE = new BasicPhase(1000, Arrays.asList(
            new BasicPhase.Offset(1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(-1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(0.0f, 1.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(0.0f, -1.0f, IsLoadedQuery.Result.LOADED),
            new BasicPhase.Offset(2.75f, 0.0f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(-2.75f, 0.0f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(0.0f, 2.75f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(0.0f, -2.75f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(2.75f, 2.75f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(-2.75f, -2.75f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(2.75f, -2.75f, IsLoadedQuery.Result.UNLOADED),
            new BasicPhase.Offset(-2.75f, 2.75f, IsLoadedQuery.Result.UNLOADED)));

    private final Deque<IsLoadedQuery> currentQueries = new ConcurrentLinkedDeque<>();

    private final List<Float> loadedSamples = new ArrayList<>();
    private final List<ChunkPosition> loadedOffsets = new ArrayList<>();

    private final long trackerID;
    private final TrackedPlayer trackedPlayer;

    private IPhase currentPhase;
    private float currentHealth;

    private long lastUpdate;
    private boolean awaitingMovementCheck;

    private float lastOffsetX;
    private float lastOffsetZ;

    public AdaptiveTracker(long trackerID, TrackedPlayer trackedPlayer) {
        this.trackerID = trackerID;
        this.trackedPlayer = trackedPlayer;

        currentPhase = GEZA_PHASE;
        currentHealth = 3.8f;

        lastUpdate = System.currentTimeMillis() - currentPhase.getUpdateTime();

        awaitingMovementCheck = false;
    }

    @Override
    public void onTick() {
        if (System.currentTimeMillis() - lastUpdate < currentPhase.getUpdateTime()) return;
        lastUpdate = System.currentTimeMillis();

        if (!awaitingMovementCheck) {
            awaitingMovementCheck = true;

            loadedOffsets.clear();
            currentHealth = Math.max(0.0f, Math.min(5.0f, currentHealth + (float)yesCom.configHandler.ADAPTIVE_TRACKER_PHASE_CHANGE_NORMAL));

            if (currentHealth <= 2.0f) { // TODO: Streamline?
                currentPhase = WALKING_PHASE;
            } else if (currentHealth <= 3.5f) {
                currentPhase = GEZA_PHASE;
            } else if (currentHealth <= 5.0f) {
                currentPhase = FATALE_PHASE;
            }

            float distance = (yesCom.configHandler.RENDER_DISTANCE - 1) / 2.4f;
            AtomicInteger expectedLoaded = new AtomicInteger(0);

            for (int index = 0; index < currentPhase.getMaxOffsets(); ++index) {
                ChunkPosition offset = currentPhase.getOffset(index, distance);
                if (currentPhase.getResult(index) == IsLoadedQuery.Result.LOADED) expectedLoaded.addAndGet(1);

                IsLoadedQuery isLoadedQuery = new IsLoadedQuery(trackedPlayer.getRenderDistance().getCenterPosition().add(offset),
                        trackedPlayer.getDimension(), IQuery.Priority.MEDIUM, yesCom.configHandler.TYPE, (query, result) -> {
                    currentQueries.remove(query);

                    if (result == IsLoadedQuery.Result.LOADED) loadedOffsets.add(offset);

                    if (currentQueries.isEmpty()) { // Finished this update
                        awaitingMovementCheck = false;

                        loadedSamples.add(loadedOffsets.size() / (float)expectedLoaded.get());
                        while (loadedSamples.size() > 20) loadedSamples.remove(0);

                        float earlyAverage = getAverage(yesCom.configHandler.ADAPTIVE_TRACKER_EARLY_SAMPLES);
                        float lateAverage = getAverage(yesCom.configHandler.ADAPTIVE_TRACKER_LATE_SAMPLES);

                        if (loadedOffsets.isEmpty()) { // None loaded?
                            currentHealth = 5.0f;

                        } else {
                            // Are we above the maximum number of expected loaded offsets? -> decrease the number of offsets we are taking
                            if (loadedSamples.size() > yesCom.configHandler.ADAPTIVE_TRACKER_LATE_SAMPLES &&
                                    lateAverage >= yesCom.configHandler.ADAPTIVE_TRACKER_MAX_EXPECTED) {
                                currentHealth += yesCom.configHandler.ADAPTIVE_TRACKER_PHASE_CHANGE_SHRINK;
                            } else if (loadedSamples.size() > yesCom.configHandler.ADAPTIVE_TRACKER_EARLY_SAMPLES &&
                                    earlyAverage < yesCom.configHandler.ADAPTIVE_TRACKER_MIN_EXPECTED) {
                                currentHealth += yesCom.configHandler.ADAPTIVE_TRACKER_PHASE_CHANGE_GROW;
                            }
                        }

                        if (loadedSamples.size() > yesCom.configHandler.ADAPTIVE_TRACKER_LATE_SAMPLES && lateAverage <= 0.0f) { // No loaded points?
                            yesCom.logger.info(String.format("Failed online check for %s.", trackedPlayer));
                            if (yesCom.trackingHandler.getTracker(trackerID) != null) yesCom.trackingHandler.trackPanic(trackedPlayer);

                        } else {
                            float averageX = 0.0f;
                            float averageZ = 0.0f;

                            for (ChunkPosition loadedOffset : loadedOffsets) {
                                averageX += loadedOffset.getX();
                                averageZ += loadedOffset.getZ();
                            }

                            averageX /= loadedOffsets.size();
                            averageZ /= loadedOffsets.size();

                            ChunkPosition centerOffset = new ChunkPosition(
                                    (int)Math.floor(averageX * 0.75f + lastOffsetX * 0.25f),
                                    (int)Math.floor(averageZ * 0.75f + lastOffsetZ * 0.25f));

                            trackedPlayer.setRenderDistance(yesCom.dataHandler.newRenderDistance(
                                    trackedPlayer.getRenderDistance().getCenterPosition().add(centerOffset),
                                    yesCom.configHandler.RENDER_DISTANCE, 0.0f, 0.0f)); // TODO: Work out the error

                            lastOffsetX = averageX;
                            lastOffsetZ = averageZ;

                            yesCom.logger.debug(String.format("%s, curr: %.1f, avg: %.1f / %.1f.", trackedPlayer, currentHealth,
                                    earlyAverage, lateAverage));
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
    public TrackedPlayer getTrackedPlayer() {
        return trackedPlayer;
    }

    @Override
    public Health getHealth() {
        return Health.GOOD;
    }

    private float getAverage(int samples) {
        if (samples > loadedSamples.size()) return 1.0f;
        List<Float> subSamples = loadedSamples.subList(loadedSamples.size() - samples, loadedSamples.size());
        return subSamples.stream().reduce(Float::sum).orElse(0.0f) / (float)samples;
    }
}
