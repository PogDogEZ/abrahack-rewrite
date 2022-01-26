package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IRequester;
import ez.pogdog.yescom.util.Dimension;

import java.util.List;

/**
 * Interface for trackers.
 */
public interface ITracker extends IRequester {

    @Override
    default String getRequesterName() {
        return "tracker";
    }

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
     * @return Specified by different classes of tracker.
     */
    String getName();

    Dimension getDimension();

    /**
     * @return The priority the tracker has, this could vary.
     */
    IQuery.Priority getPriority();

    float getMaxSpeed();
    float getMinSpeed();

    /**
     * Sets the current queryrate limit for this tracker.
     * @param queryRateLimit The limit.
     */
    void setQueryRateLimit(float queryRateLimit);

    /**
     * @return The potential targets of the tracker.
     */
    List<TrackedPlayer> getTrackedPlayers();
}
