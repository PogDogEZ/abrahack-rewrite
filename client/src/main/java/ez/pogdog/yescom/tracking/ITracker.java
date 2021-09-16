package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.handlers.TrackingHandler;

import java.util.UUID;

public interface ITracker {

    TrackingHandler.TrackerTickResult onTick();

    void onPossibleJoin(UUID uuid);
}
