package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.handlers.TrackingHandler;

public interface ITracker {

    TrackingHandler.TrackerTickResult onTick();
}
