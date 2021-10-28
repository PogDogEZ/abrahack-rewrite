package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.data.serializable.TrackedPlayer;

public interface ITracker {

    void onTick();
    void onLost();

    long getTrackerID();
    TrackedPlayer getTrackedPlayer();

    Health getHealth();

    enum Health {
        GOOD, ILL, DEAD;
    }
}
