package ez.pogdog.yescom.tracking;

import java.util.UUID;

/**
 * Basic layout for different types of trackers (if we want).
 * This can be removed, modified, I'm just laying shit out that MAY be useful
 */
public interface ITracker {

    void onTick();

    /**
     * For when we lose the person we are following
     */
    boolean isLost();

    /**
     * @return The tracker's ID
     */
    UUID getID();

    /**
     * Mainly for later, if we reassign a archived tracked player back to a new tracker, we will want to know
     * what player we set it to so we can update it's entry instead of creating a new one.
     *
     * EX: If we detect KiwiSlider logs back on, we assign a new tracker to his login spot, and specify that it is KiwiSlider we are tracking
     * for when we lose him again; we just update all the info on him
     * @return Tracked player the tracker was assigned to
     */
    TrackedPlayer getTrackedPlayer();

}
