package ez.pogdog.yescom.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * An intermediary tracker class.
 */
public class Tracker {

    private final List<BigInteger> trackedPlayerIDs = new ArrayList<>();

    private final long trackerID;

    public Tracker(long trackerID, List<BigInteger> trackedPlayerIDs) {
        this.trackerID = trackerID;
        this.trackedPlayerIDs.addAll(trackedPlayerIDs);
    }

    public List<BigInteger> getTrackedPlayerIDs() {
        return new ArrayList<>(trackedPlayerIDs);
    }

    public void addTrackedPlayerID(BigInteger trackedPlayerID) {
        trackedPlayerIDs.add(trackedPlayerID);
    }

    public void setTrackedPlayerIDs(List<BigInteger> trackedPlayerIDs) {
        this.trackedPlayerIDs.clear();
        this.trackedPlayerIDs.addAll(trackedPlayerIDs);
    }

    public void addTrackedPlayerIDs(List<BigInteger> trackedPlayerIDs) {
        this.trackedPlayerIDs.addAll(trackedPlayerIDs);
    }

    public void removeTrackedPlayerID(BigInteger trackedPlayerID) {
        trackedPlayerIDs.remove(trackedPlayerID);
    }

    public long getTrackerID() {
        return trackerID;
    }
}
