package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.handlers.IHandler;
import ez.pogdog.yescom.tracking.Tracker;
import ez.pogdog.yescom.tracking.algorithm.ITrackingAlgorithm;
import ez.pogdog.yescom.tracking.TrackedPlayer;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;

/*
 * Handles assigning trackers, manages all running trackers, only handles CURRENT tracking
 */
public class TrackingHandler implements IHandler {

    private final ArrayList<Tracker> activeTrackers = new ArrayList<>();

    /**
     * We add lost trackers to this ArrayList to prevent deleting useful data due to player logging out for short period of time.
     * If the tracker remains lost for over 24 hours it is turned into a TrackedPlayer.
     */
    private final ArrayList<Tracker> lostTrackers = new ArrayList<>();

    private final ArrayList<TrackedPlayer> trackedPlayers = new ArrayList<>();

    /* ------------------------ Implementations ------------------------ */

    @Override
    public void onTick() {
        activeTrackers.forEach(tracker -> {
            switch (tracker.tick()) {
                case Good:
                    break;
                case Lost:
                    activeTrackers.remove(tracker);
                    lostTrackers.add(tracker);
                    break;
                case Found:
                    lostTrackers.remove(tracker);
                    activeTrackers.add(tracker);
                    break;
            }
        });
    }

    @Override
    public void onExit() {
        activeTrackers.forEach(tracker -> {
            trackedPlayers.add(new TrackedPlayer(tracker.getPosition()));
        });

        lostTrackers.forEach(tracker -> {
            trackedPlayers.add(new TrackedPlayer(tracker.getPosition()));
        });

        //TODO serialize data and save it to "data/trackers"
    }

    /* ------------------------ Tracking Methods ------------------------ */

    public void addTracker(ChunkPosition chunk, Dimension dimension) {
        activeTrackers.add(new Tracker(chunk, dimension));
    }

    public void continueTracker(TrackedPlayer trackedPlayer, Dimension dimension) {
        activeTrackers.add(new Tracker(trackedPlayer, dimension));
    }

    public enum TrackerTickResult {
        Good, Lost, Found;
    }
}
