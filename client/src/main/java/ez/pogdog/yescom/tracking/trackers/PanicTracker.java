package ez.pogdog.yescom.tracking.trackers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.tracking.IResolver;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.tracking.resolvers.DimChangeResolver;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.Collections;
import java.util.List;

public class PanicTracker implements ITracker {

    private final YesCom yesCom = YesCom.getInstance();

    private final long trackerID;
    private final TrackedPlayer trackedPlayer;
    private IResolver currentResolver;

    public PanicTracker(long trackedID, TrackedPlayer trackedPlayer) {
        this.trackerID = trackedID;
        this.trackedPlayer = trackedPlayer;
    }

    @Override
    public void onTick() {
        if (currentResolver == null) {
            ChunkPosition possiblePosition = trackedPlayer.getRenderDistance().getCenterPosition();
            switch (trackedPlayer.getDimension()) {
                case NETHER: {
                    if (yesCom.connectionHandler.hasAccountsIn(Dimension.OVERWORLD)) {
                        currentResolver = new DimChangeResolver(
                                new ChunkPosition(possiblePosition.getX() * 8, possiblePosition.getZ() * 8),
                                Dimension.OVERWORLD);
                    } else {
                        doLogout();
                    }
                    break;
                }
                case OVERWORLD: {
                    if (yesCom.connectionHandler.hasAccountsIn(Dimension.NETHER)) {
                        currentResolver = new DimChangeResolver(
                                new ChunkPosition(possiblePosition.getX() / 8, possiblePosition.getZ() / 8),
                                Dimension.NETHER);
                    } else {
                        doLogout();
                    }
                    break;
                }
                case END: {
                    doLogout();
                    break;
                }
            }
        } else if (currentResolver.isComplete()) {
            RenderDistance newRenderDistance = currentResolver.getRenderDistance();

            if (newRenderDistance != null) {
                trackedPlayer.setRenderDistance(newRenderDistance);
                trackedPlayer.setDimension(currentResolver.getDimension());
                yesCom.trackingHandler.trackBasic(trackedPlayer);

            } else {
                doLogout();
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
    public List<TrackedPlayer> getTrackedPlayers() {
        return Collections.singletonList(trackedPlayer);
    }

    @Override
    public Health getHealth() {
        return Health.GOOD;
    }

    private void doLogout() {
        yesCom.trackingHandler.handleLogout(trackedPlayer);
        yesCom.trackingHandler.removeTracker(this);
    }
}
