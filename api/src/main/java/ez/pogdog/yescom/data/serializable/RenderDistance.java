package ez.pogdog.yescom.data.serializable;

import ez.pogdog.yescom.data.ISerializable;
import ez.pogdog.yescom.util.ChunkPosition;

import java.math.BigInteger;
import java.util.Objects;

public class RenderDistance implements ISerializable {

    private final BigInteger renderDistanceID;
    private final ChunkPosition centerPosition;
    private final int renderDistance;
    private final float errorX;
    private final float errorZ;

    public RenderDistance(BigInteger renderDistanceID, ChunkPosition centerPosition, int renderDistance, float errorX, float errorZ) {
        this.renderDistanceID = renderDistanceID;
        this.centerPosition = centerPosition;
        this.renderDistance = renderDistance;
        this.errorX = errorX;
        this.errorZ = errorZ;
    }

    public RenderDistance(ChunkPosition centerPosition, int renderDistance, float errorX, float errorZ) {
        this(BigInteger.ZERO, centerPosition, renderDistance, errorX, errorZ);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        RenderDistance that = (RenderDistance)other;
        return renderDistance == that.renderDistance && Float.compare(that.errorX, errorX) == 0 && Float.compare(that.errorZ, errorZ) == 0 && renderDistanceID.equals(that.renderDistanceID) && centerPosition.equals(that.centerPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(renderDistanceID, centerPosition, renderDistance, errorX, errorZ);
    }

    @Override
    public String toString() {
        return String.format("RenderDistance(center=%s, error=%.1f)", centerPosition, errorX);
    }

    @Override
    public BigInteger getID() {
        return renderDistanceID;
    }

    /**
     * Returns whether this render distance contains a certain position.
     * @param chunkPosition The position to check for.
     * @return Whether the positions lies within this render distance.
     */
    public boolean contains(ChunkPosition chunkPosition) {
        return Math.abs(centerPosition.getX() - chunkPosition.getX()) < Math.ceil(renderDistance / 2.0f + errorX) &&
                Math.abs(centerPosition.getZ() - chunkPosition.getZ()) < Math.ceil(renderDistance / 2.0f + errorZ);
    }

    /**
     * Returns the maximum chunk position of this render distance.
     * @return The max chunk position.
     */
    public ChunkPosition getMax() {
        return centerPosition.add(Math.floorDiv(renderDistance, 2), Math.floorDiv(renderDistance, 2));
    }

    /**
     * Returns the minimum chunk position of this render distance.
     * @return The min chunk position.
     */
    public ChunkPosition getMin() {
        return centerPosition.subtract(Math.floorDiv(renderDistance, 2), Math.floorDiv(renderDistance, 2));
    }

    /**
     * Returns the center position of this render distance.
     * @return The center chunk position.
     */
    public ChunkPosition getCenterPosition() {
        return centerPosition;
    }

    /**
     * Returns the actual render distance (13, 14, etc).
     * @return The render distance of the server.
     */
    public int getRenderDistance() {
        return renderDistance;
    }

    /**
     * Returns how much error this render distance had when it was found.
     * @return The x error.
     */
    public float getErrorX() {
        return errorX;
    }

    /**
     * Returns how much error this render distance had when it was found.
     * @return The z error.
     */
    public float getErrorZ() {
        return errorZ;
    }
}