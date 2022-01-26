package ez.pogdog.yescom.tracking.trackers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.queries.IsLoadedQuery;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.tracking.resolvers.QuickResolver;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsible for tracking AFK players. This tracker slows down over time, so as to not waste many checks on AFK players,
 * over a certain number of checks, it re-resolves the player's render distance, just as a precaution.
 */
public class AFKTracker implements ITracker {

    private final YesCom yesCom = YesCom.getInstance();

    private final List<TrackedPlayer> trackedPlayers = new ArrayList<>();
    private final long trackerID;
    private final Dimension dimension;
    private final long trackingSince;

    private float queryRateLimit;

    private long lastUpdateTime;
    private int nextUpdate;
    private int resolveCount;

    public AFKTracker(long trackerID, float queryRateLimit, List<TrackedPlayer> trackedPlayers) {
        this.trackedPlayers.addAll(trackedPlayers);
        this.trackerID = trackerID;
        dimension = trackedPlayers.get(0).getDimension();
        trackingSince = System.currentTimeMillis();

        this.queryRateLimit = queryRateLimit;

        lastUpdateTime = System.currentTimeMillis();
        nextUpdate = 0; // Update immediately
        resolveCount = 0;
    }

    @Override
    public void onTick() {
        List<TrackedPlayer> toRemove = new ArrayList<>();
        trackedPlayers.forEach(trackedPlayer -> { // Get rid of players who have logged out/changed dimension
            if (trackedPlayer.isLoggedOut() || trackedPlayer.getDimension() != dimension) toRemove.add(trackedPlayer);
        });
        trackedPlayers.removeAll(toRemove);

        if (trackedPlayers.isEmpty()) {
            onLost();
            return;
        }

        ChunkPosition estimatedPosition;
        if (trackedPlayers.size() == 1) {
            estimatedPosition = trackedPlayers.get(0).getRenderDistance().getCenterPosition();
        } else { // FIXME: Do we really need to do this more than once? Since the next time round, all the positions will by synced
            AtomicInteger averageX = new AtomicInteger();
            AtomicInteger averageZ = new AtomicInteger();
            trackedPlayers.forEach(trackedPlayer -> {
                ChunkPosition position = trackedPlayer.getRenderDistance().getCenterPosition();
                averageX.addAndGet(position.getX());
                averageZ.addAndGet(position.getZ());
            });

            estimatedPosition = new ChunkPosition(averageX.get() / trackedPlayers.size(), averageZ.get() / trackedPlayers.size());
        }

        if (System.currentTimeMillis() - lastUpdateTime > nextUpdate) {
            IsLoadedQuery query = new IsLoadedQuery(this, estimatedPosition, dimension, IQuery.Priority.LOW,
                    yesCom.configHandler.TYPE, this::onChunkResult);
            if (yesCom.trackingHandler.requestQuery(this, query)) { // The query has gone through
                nextUpdate += (System.currentTimeMillis() - lastUpdateTime) / 2;
                // Keep it bounded
                nextUpdate = Math.max(yesCom.configHandler.MIN_AFK_UPDATE, Math.min(yesCom.configHandler.MAX_AFK_UPDATE, nextUpdate));
                lastUpdateTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onLost() {
    }

    @Override
    public long getTrackerID() {
        return trackerID;
    }

    @Override
    public String getName() {
        return "afk";
    }

    @Override
    public Dimension getDimension() {
        return dimension;
    }

    @Override
    public IQuery.Priority getPriority() {
        return IQuery.Priority.LOW; // We should come after faster moving players
    }

    @Override
    public float getMaxSpeed() {
        return 0.0f; // We're supposed to be tracking AFK players here
    }

    @Override
    public float getMinSpeed() {
        return 0.0f;
    }

    @Override
    public void setQueryRateLimit(float queryRateLimit) {
        this.queryRateLimit = queryRateLimit;
    }

    @Override
    public List<TrackedPlayer> getTrackedPlayers() {
        return new ArrayList<>(trackedPlayers);
    }

    private void onRenderDistanceResult(QuickResolver resolver, RenderDistance renderDistance) {
        // Have we been tracking for at least one update? They may move around a little at the beginning due to inaccuracy
        boolean registerMoves = System.currentTimeMillis() - trackingSince > yesCom.configHandler.MIN_AFK_UPDATE;
        ChunkPosition centerPosition = renderDistance.getCenterPosition();

        List<TrackedPlayer> toRemove = new ArrayList<>();
        for (TrackedPlayer trackedPlayer : trackedPlayers) {
            ChunkPosition deltaPosition = trackedPlayer.getRenderDistance().getCenterPosition().subtract(centerPosition);
            if (registerMoves && Math.abs(deltaPosition.getX()) > 1 || Math.abs(deltaPosition.getZ()) > 1) {
                // TODO: Retrack specific player
                // TODO: We need a finer resolver so we can separate the players, as two render distances merge when close enough
                toRemove.add(trackedPlayer);
            } else {
                trackedPlayer.setRenderDistance(renderDistance); // Update all render distances
            }
        }
        trackedPlayers.removeAll(toRemove);

        if (trackedPlayers.isEmpty()) onLost(); // No tracked players left? -> don't need this tracker anymore
    }

    private void onChunkResult(IsLoadedQuery query, IsLoadedQuery.Result result) {
        if (result != IsLoadedQuery.Result.LOADED) { // Probably lost all our tracked players
            onLost();
            return;
        }

        if (++resolveCount > yesCom.configHandler.AFK_RESOLVE_COUNT) {
            resolveCount = 0;

            // Max phase is really high as we want this to be precise, note this should occur very infrequently
            QuickResolver resolver = new QuickResolver(query.getChunkPosition(), dimension, 100,
                    IQuery.Priority.LOW, this::onRenderDistanceResult);
            // if (resolver)
        }
    }
}
