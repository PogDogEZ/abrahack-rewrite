package ez.pogdog.yescom.handlers.tracking;

import ez.pogdog.yescom.handlers.IHandler;
import ez.pogdog.yescom.tracking.Tracker;
import ez.pogdog.yescom.tracking.algorithm.ITrackingAlgorithm;
import ez.pogdog.yescom.tracking.TrackedPlayer;
import ez.pogdog.yescom.util.ChunkPosition;

import java.util.ArrayList;

/*
 * Handles assigning trackers, manages all running trackers, only handles CURRENT tracking
 */
public class TrackingHandler implements IHandler {

    public ArrayList<Tracker> activeTrackers = new ArrayList<>();

    /* ------------------------ Implementations ------------------------ */

    @Override
    public void onTick() {

    }

    @Override
    public void onExit() {

    }

    /* ------------------------ Tracking Methods ------------------------ */

    public void addTracker(ChunkPosition chunk) {
        activeTrackers.add(new Tracker(chunk));
    }

    public void continueTracker(TrackedPlayer trackedPlayer) {
        activeTrackers.add(new Tracker(trackedPlayer));
    }
}
