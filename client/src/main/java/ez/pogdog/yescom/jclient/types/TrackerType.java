package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.tracking.ITracker;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TrackerType extends Type<ITracker> {

    private final YesCom yesCom = YesCom.getInstance();

    @Override
    public ITracker read(InputStream inputStream) throws IOException {
        long trackerID = Registry.LONG.read(inputStream);
        TrackedPlayer trackedPlayer = YCRegistry.TRACKED_PLAYER.read(inputStream);

        return yesCom.trackingHandler.getTracker(trackerID);
    }

    @Override
    public void write(ITracker value, OutputStream outputStream) throws IOException {
        Registry.LONG.write(value.getTrackerID(), outputStream);
        YCRegistry.TRACKED_PLAYER.write(value.getTrackedPlayer(), outputStream);
    }
}
