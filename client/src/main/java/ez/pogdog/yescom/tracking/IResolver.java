package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.util.Dimension;

public interface IResolver {

    boolean isComplete();

    /**
     * Not really needed lol just cool for debug info.
     * @return The number of checks it took to resolve the render distance.
     */
    int getChecksTaken();

    RenderDistance getRenderDistance();
    Dimension getDimension();
}
