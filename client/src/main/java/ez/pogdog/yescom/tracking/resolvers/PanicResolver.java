package ez.pogdog.yescom.tracking.resolvers;

import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.tracking.IResolver;
import ez.pogdog.yescom.util.Dimension;

public class PanicResolver implements IResolver {

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public int getChecksTaken() {
        return 0;
    }

    @Override
    public RenderDistance getRenderDistance() {
        return null;
    }

    @Override
    public Dimension getDimension() {
        return null;
    }
}
