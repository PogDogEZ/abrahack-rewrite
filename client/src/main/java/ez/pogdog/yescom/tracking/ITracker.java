package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.data.serializable.TrackedPlayer;

import java.util.List;

/**
 * Interface for trackers.
 */
public interface ITracker {

    /**
     * Called in sync tick by the tracking handler.
     */
    void onTick();

    /**
     * Called when the tracker has lost the target.
     */
    void onLost();

    /**
     * @return The unique ID of the tracker.
     */
    long getTrackerID();

    /**
     * @return The potential targets of the tracker.
     */
    List<TrackedPlayer> getTrackedPlayers();

    /**
     * @return How "healthy" the tracker is, not used rn.
     */
    Health getHealth();

    enum Health {
        GOOD, ILL, DEAD;
    }
}
