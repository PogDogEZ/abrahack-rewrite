package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.ChunkPosition;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public class RenderDistanceType extends Type<RenderDistance> {

    @Override
    public RenderDistance read(InputStream inputStream) throws IOException {
        BigInteger renderDistanceID = Registry.VAR_INTEGER.read(inputStream);
        ChunkPosition centerPosition = YCRegistry.CHUNK_POSITION.read(inputStream);
        int renderDistance = Registry.UNSIGNED_SHORT.read(inputStream);
        float errorX = Registry.FLOAT.read(inputStream);
        float errorZ = Registry.FLOAT.read(inputStream);

        return new RenderDistance(renderDistanceID, centerPosition, renderDistance, errorX, errorZ);
    }

    @Override
    public void write(RenderDistance value, OutputStream outputStream) throws IOException {
        Registry.VAR_INTEGER.write(value.getID(), outputStream);
        YCRegistry.CHUNK_POSITION.write(value.getCenterPosition(), outputStream);
        Registry.UNSIGNED_SHORT.write(value.getRenderDistance(), outputStream);
        Registry.FLOAT.write(value.getErrorX(), outputStream);
        Registry.FLOAT.write(value.getErrorZ(), outputStream);
    }
}
