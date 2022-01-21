package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TrackingDataType extends Type<TrackedPlayer.TrackingData> {

    @Override
    public TrackedPlayer.TrackingData read(InputStream inputStream) throws IOException {
        Map<Long, BigInteger> renderDistances = new HashMap<>();
        int renderDistancesToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < renderDistancesToRead; ++index)
            renderDistances.put(Registry.LONG.read(inputStream), Registry.VAR_INTEGER.read(inputStream));

        return new TrackedPlayer.TrackingData(YesCom.getInstance().dataHandler, renderDistances);
    }

    @Override
    public void write(TrackedPlayer.TrackingData value, OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(0, outputStream);
        for (Map.Entry<Long, BigInteger> entry : value.getRenderDistances().entrySet()) {
            Registry.LONG.write(entry.getKey(), outputStream);
            Registry.VAR_INTEGER.write(entry.getValue(), outputStream);
        }
    }
}
