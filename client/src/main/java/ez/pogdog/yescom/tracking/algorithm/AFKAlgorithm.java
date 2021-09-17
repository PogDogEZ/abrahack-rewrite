package ez.pogdog.yescom.tracking.algorithm;

import ez.pogdog.yescom.tracking.Tracker;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

public class AFKAlgorithm extends TrackingAlgorithm {

    public AFKAlgorithm(Tracker tracker, ChunkPosition renderDistanceCenter) {
        super(tracker, renderDistanceCenter, tracker.getPosition().getDimension());
    }

    @Override
    public TickResult onTick() {
        return TickResult.Stagnant;
    }

    @Override
    public ChunkPosition getPlayerPosition() {
        return null;
    }

    // TODO: broken, plz fix
    @Override
    public Dimension getDimension() {
        return null;
    }
    /**
    @Override
    public Dimension getDimension() {
        scanForPlayer(position.getPosition(), Dimension.OVERWORLD);
        return null;
    }
    */
}
