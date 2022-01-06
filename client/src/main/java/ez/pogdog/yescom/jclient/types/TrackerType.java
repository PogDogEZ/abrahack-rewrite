package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
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

        int IDsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < IDsToRead; ++index) Registry.VAR_INTEGER.read(inputStream);

        return null; // yesCom.trackingHandler.getTracker(trackerID);
    }

    @Override
    public void write(ITracker value, OutputStream outputStream) throws IOException {
        Registry.LONG.write(value.getTrackerID(), outputStream);

        Registry.UNSIGNED_SHORT.write(value.getTrackedPlayers().size(), outputStream);
        for (TrackedPlayer trackedPlayer : value.getTrackedPlayers()) Registry.VAR_INTEGER.write(trackedPlayer.getID(), outputStream);
    }
}
