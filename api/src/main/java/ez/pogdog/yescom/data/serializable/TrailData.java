package ez.pogdog.yescom.data.serializable;

import ez.pogdog.yescom.data.ISerializable;

import java.math.BigInteger;
import java.util.*;

public class TrailData implements ISerializable {

    private final long ID;
    private final SortedMap<BigInteger, RenderDistance> renderDistances = new TreeMap<>();

    public TrailData(long ID) {
        this.ID = ID;
    }

    public void addRenderDistance(RenderDistance renderDistance) {
        renderDistances.put(renderDistance.getID(), renderDistance);
    }

    public boolean addRenderDistance(long ID) {
        //TODO make this look up the RenderDistance by ID and add it
        return true;
    }

    public void addRenderDistances(List<RenderDistance> renderDistances) {
        for (RenderDistance renderDistance : renderDistances)
            addRenderDistance(renderDistance);
    }

    public long[] getRenderDistanceIds() {
        long[] result = new long[renderDistances.size()];

        int index = -1;
        for (BigInteger bigInteger : renderDistances.keySet())
            result[++index] = bigInteger.longValue();

        return result;
    }

    public long getTrailID() {
        return ID;
    }

    public int getSize() {
        return renderDistances.size();
    }

    public boolean isEmpty() {
        return renderDistances.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("TrailData(ID = %s, RenderDistances = %s)", ID, renderDistances.size());
    }

    @Override
    public BigInteger getID() {
        return null;
    }
}
