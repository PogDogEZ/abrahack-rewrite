package ez.pogdog.yescom.tracking.algorithm;

import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.List;

public interface ITrackingAlgorithm {

    TickResult onTick();

    ChunkPosition getPlayerPosition();

    Dimension getDimension();

    enum TickResult {
        Moved, Stagnant, Lost;
    }
}
