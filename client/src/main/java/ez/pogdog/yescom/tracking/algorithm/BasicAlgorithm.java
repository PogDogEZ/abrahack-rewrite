package ez.pogdog.yescom.tracking.algorithm;

import ez.pogdog.yescom.tracking.Tracker;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

public class BasicAlgorithm extends TrackingAlgorithm {

    public BasicAlgorithm(Tracker tracker, ChunkPosition renderDistanceCenter) {
        super(tracker, renderDistanceCenter, tracker.getPosition().getDimension(), false);
    }

    @Override
    public TickResult onTick() {
        return TickResult.Stagnant;
    }

    @Override
    public ChunkPosition getPlayerPosition() {
        return null;
    }

    @Override
    public Dimension getDimension() {
        return null;
    }
}
