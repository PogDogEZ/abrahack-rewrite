package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.query.IRequester;
import ez.pogdog.yescom.util.Dimension;

/**
 * Resolvers are responsible for determining the render distance of one (or more) players.
 */
public interface IResolver extends IRequester {

    @Override
    default String getRequesterName() {
        return "resolver";
    }

    void resolve();

    boolean isComplete();

    /**
     * Not really needed lol just cool for debug info.
     * @return The number of checks it took to resolve the render distance.
     */
    int getChecksTaken();

    RenderDistance getRenderDistance();
    Dimension getDimension();
}
