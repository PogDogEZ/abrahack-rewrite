package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.util.Dimension;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrackedPlayerType extends Type<TrackedPlayer> {

    private final YesCom yesCom = YesCom.getInstance();

    @Override
    public TrackedPlayer read(InputStream inputStream) throws IOException {
        BigInteger trackedPlayerID = Registry.VARINT.read(inputStream);

        Map<UUID, Integer> possiblePlayers = new HashMap<>();
        int possiblePlayersToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < possiblePlayersToRead; ++index)
            possiblePlayers.put(UUID.nameUUIDFromBytes(Registry.BYTES.read(inputStream)), Registry.UNSIGNED_SHORT.read(inputStream));

        Map<Long, BigInteger> renderDistances = new HashMap<>();
        int renderDistancesToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < renderDistancesToRead; ++index)
            renderDistances.put(Registry.LONG.read(inputStream), Registry.VARINT.read(inputStream));

        RenderDistance renderDistance = YCRegistry.RENDER_DISTANCE.read(inputStream);
        Dimension dimension = YCRegistry.DIMENSION.read(inputStream);
        boolean loggedOut = Registry.BOOLEAN.read(inputStream);
        long foundAt = Registry.LONG.read(inputStream);

        TrackedPlayer trackedPlayer = new TrackedPlayer(trackedPlayerID, new TrackedPlayer.TrackingData(yesCom.dataHandler, renderDistances),
                renderDistance, dimension, loggedOut, foundAt);
        trackedPlayer.setPossiblePlayers(possiblePlayers);
        return trackedPlayer;
    }

    @Override
    public void write(TrackedPlayer value, OutputStream outputStream) throws IOException {
        Registry.VARINT.write(value.getID(), outputStream);

        Registry.UNSIGNED_SHORT.write(value.getPossiblePlayers().size(), outputStream);
        for (Map.Entry<UUID, Integer> entry : value.getPossiblePlayers().entrySet()) {
            Registry.BYTES.write(ByteBuffer.allocate(16)
                    .putLong(entry.getKey().getMostSignificantBits())
                    .putLong(entry.getKey().getLeastSignificantBits())
                    .array(), outputStream);
            Registry.UNSIGNED_SHORT.write(entry.getValue(), outputStream);
        }

        Registry.UNSIGNED_SHORT.write(0, outputStream); // TODO: More data efficient solution
        /*
        for (Map.Entry<Long, BigInteger> entry : value.getTrackingData().getRenderDistances().entrySet()) {
            Registry.LONG.write(entry.getKey(), outputStream);
            Registry.VARINT.write(entry.getValue(), outputStream);
        }
         */

        YCRegistry.RENDER_DISTANCE.write(value.getRenderDistance(), outputStream);
        YCRegistry.DIMENSION.write(value.getDimension(), outputStream);
        Registry.BOOLEAN.write(value.isLoggedOut(), outputStream);
        Registry.LONG.write(value.getFoundAt(), outputStream);
    }
}
