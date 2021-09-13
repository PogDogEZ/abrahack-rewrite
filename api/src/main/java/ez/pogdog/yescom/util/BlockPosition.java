package ez.pogdog.yescom.util;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;

import java.util.Objects;

public class BlockPosition {

    private final int x;
    private final int y;
    private final int z;

    public BlockPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPosition(BlockPosition position) {
        this(position.x, position.y, position.z);
    }

    public BlockPosition(Position position) {
        this(position.getX(), position.getY(), position.getZ());
    }

    public BlockPosition(double x, double y, double z) {
        this((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
    }

    public BlockPosition(ez.pogdog.yescom.util.Position position) {
        this(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        BlockPosition that = (BlockPosition)other;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("BlockPosition(x=%d, y=%d, z=%d)", x, y, z);
    }

    /**
     * Adds the given x, y and z coordinates to this position.
     * @param x The x amount.
     * @param y The y amount.
     * @param z The z amount.
     * @return The new position.
     */
    public BlockPosition add(int x, int y, int z) {
        return new BlockPosition(this.x + x, this.y + y, this.z + z);
    }

    public BlockPosition add(BlockPosition other) {
        return add(other.x, other.y, other.z);
    }

    public BlockPosition add(ez.pogdog.yescom.util.Position position) {
        return add(position.toBlockPosition());
    }

    /**
     * Subtracts the given x, y and z coordinates from this position.
     * @param x The x amount.
     * @param y The y amount.
     * @param z The z amount.
     * @return The new position.
     */
    public BlockPosition subtract(int x, int y, int z) {
        return new BlockPosition(this.x - x, this.y - y, this.z - z);
    }

    public BlockPosition subtract(BlockPosition other) {
        return subtract(other.x, other.y, other.z);
    }

    public BlockPosition subtract(ez.pogdog.yescom.util.Position position) {
        return subtract(position.toBlockPosition());
    }

    public float getDistanceTo(BlockPosition otherPosition) {
        return (float) Math.sqrt(
                Math.pow(this.x - otherPosition.getX(), 2) + Math.pow(this.z - otherPosition.getZ(), 2));
    }

    /**
     * Returns the coordinates this block position points to as a position.
     * @return The position.
     */
    public ez.pogdog.yescom.util.Position toPosition() {
        return new ez.pogdog.yescom.util.Position(x, y, z);
    }

    /**
     * Returns the center of this block position as a position.
     * @return The position.
     */
    public ez.pogdog.yescom.util.Position toPositionCenter() {
        return new ez.pogdog.yescom.util.Position(x + 0.5, y + 0.5, z + 0.5);
    }

    /**
     * Returns an MCProtocolLib compatible block position, which is usable with packets.
     * @return The new position.
     */
    public Position toSerializable() {
        return new Position(x, y, z);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
