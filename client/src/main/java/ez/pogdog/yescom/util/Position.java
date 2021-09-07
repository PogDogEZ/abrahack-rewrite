package ez.pogdog.yescom.util;

import java.util.Objects;

public class Position {

    private final double x;
    private final double y;
    private final double z;

    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Position position = (Position)other;
        return Double.compare(position.x, x) == 0 && Double.compare(position.y, y) == 0 && Double.compare(position.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("Position(x=%.2f, y=%.2f, z=%.2f)", x, y, z);
    }

    /**
     * Gets the distance to a set of coordinates.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return The distance.
     */
    public float getDistance(double x, double y, double z) {
        double xDiff = this.x - x;
        double yDiff = this.y - y;
        double zDiff = this.z - z;

        return (float)Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
    }

    public float getDistance(Position other) {
        return getDistance(other.x, other.y, other.z);
    }

    /**
     * Returns the distance to the center of the block position.
     * @param blockPos The block position.
     * @return The distance.
     */
    public float getDistance(BlockPosition blockPos) {
        return getDistance(blockPos.toPositionCenter());
    }

    /**
     * Adds the given x, y and z coordinates to this position.
     * @param x The x amount.
     * @param y The y amount.
     * @param z The z amount.
     * @return The new position.
     */
    public Position add(double x, double y, double z) {
        return new Position(this.x + x, this.y + y, this.z + z);
    }

    public Position add(Position other) {
        return add(other.x, other.y, other.z);
    }

    public Position add(BlockPosition blockPos) {
        return add(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    /**
     * Subtracts the given x, y and z coordinates from this position.
     * @param x The x amount.
     * @param y The y amount.
     * @param z The z amount.
     * @return The new position.
     */
    public Position subtract(double x, double y, double z) {
        return new Position(this.x - x, this.y - y, this.z - z);
    }

    public Position subtract(Position other) {
        return subtract(other.x, other.y, other.z);
    }

    public Position subtract(BlockPosition blockPos) {
        return subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public BlockPosition toBlockPosition() {
        return new BlockPosition(this);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
