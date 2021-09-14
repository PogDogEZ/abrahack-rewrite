package ez.pogdog.yescom.tracking.algorithm;

import ez.pogdog.yescom.tracking.Tracker;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.List;

public class AFKAlgorithm implements ITrackingAlgorithm {

    private final Tracker tracker;

    public AFKAlgorithm(Tracker tracker, ChunkPosition renderDistanceCenter) {
        this.tracker = tracker;
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
