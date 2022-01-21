package ez.pogdog.yescom.tracking.trackers;

import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.tracking.ITracker;

import java.util.List;

/**
 * Responsible for tracking AFK players.
 */
public class AFKTracker implements ITracker {

    @Override
    public void onTick() {

    }

    @Override
    public void onLost() {

    }

    @Override
    public long getTrackerID() {
        return 0;
    }

    @Override
    public List<TrackedPlayer> getTrackedPlayers() {
        return null;
    }
}
