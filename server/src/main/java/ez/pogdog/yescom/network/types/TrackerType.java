package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.util.Tracker;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class TrackerType extends Type<Tracker> {

    @Override
    public Tracker read(InputStream inputStream) throws IOException {
        long trackerID = Registry.LONG.read(inputStream);

        List<BigInteger> trackedPlayerIDs = new ArrayList<>();
        int IDsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < IDsToRead; ++index) trackedPlayerIDs.add(Registry.VAR_INTEGER.read(inputStream));

        return new Tracker(trackerID, trackedPlayerIDs);
    }

    @Override
    public void write(Tracker value, OutputStream outputStream) throws IOException {
        Registry.LONG.write(value.getTrackerID(), outputStream);

        Registry.UNSIGNED_SHORT.write(value.getTrackedPlayerIDs().size(), outputStream);
        for (BigInteger trackedPlayerID : value.getTrackedPlayerIDs()) Registry.VAR_INTEGER.write(trackedPlayerID, outputStream);
    }
}
