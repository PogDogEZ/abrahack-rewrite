package ez.pogdog.yescom.util;

import java.util.Objects;

public class ChunkPosition {

    private final int x;
    private final int z;

    public ChunkPosition(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public ChunkPosition(ChunkPosition position) {
        this(position.x, position.z);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ChunkPosition that = (ChunkPosition)other;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return String.format("ChunkPosition(x=%d, z=%d)", x, z);
    }

    /**
     * Adds given x and z coordinates to this position.
     * @param x The x amount.
     * @param z The z amount.
     * @return A new chunk position.
     */
    public ChunkPosition add(int x, int z) {
        return new ChunkPosition(this.x + x, this.z + z);
    }

    public ChunkPosition add(ChunkPosition other) {
        return add(other.x, other.z);
    }

    /**
     * Subtracts the given x and z from the coordinates of this position.
     * @param x The x amount.
     * @param z The z amount.
     * @return A new chunk position.
     */
    public ChunkPosition subtract(int x, int z) {
        return new ChunkPosition(this.x - x, this.z - z);
    }

    public ChunkPosition subtract(ChunkPosition other) {
        return subtract(other.x, other.z);
    }

    /**
     * Returns a block position in the chunk this position details, with given x, y and z offsets.
     * @param xOffset The x offset.
     * @param yOffset The y offset.
     * @param zOffset The z offset.
     * @return The block position.
     */
    public BlockPosition getPosition(int xOffset, int yOffset, int zOffset) {
        return new BlockPosition((x << 4) + xOffset, yOffset, (z << 4) + zOffset);
    }

    public BlockPosition getPosition(BlockPosition offset) {
        return getPosition(offset.getX(), offset.getY(), offset.getZ());
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }
}
