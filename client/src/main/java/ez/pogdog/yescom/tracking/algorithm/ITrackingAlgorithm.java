package ez.pogdog.yescom.tracking.algorithm;

import ez.pogdog.yescom.util.ChunkPosition;

import java.util.List;

public interface ITrackingAlgorithm {

    void onTick();

    List<ChunkPosition> getQueried();
}
