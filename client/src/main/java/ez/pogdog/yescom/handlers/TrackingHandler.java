package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.tracking.ITracker;

import java.util.*;

/*
 * Handles assigning trackers, manages all running trackers, only handles CURRENT tracking
 */
public class TrackingHandler implements IHandler { // FIXME: Holy shit PLEASE overhaul this fucking class and the tracking system in general

    private final YesCom yesCom = YesCom.getInstance();

    private final Map<Long, ITracker> trackers = new HashMap<>();

    private long trackerID;

    public TrackingHandler() {
        trackerID = 0L;
    }

    @Override
    public void onTick() {
    }

    @Override
    public void onExit() {
    }
}
