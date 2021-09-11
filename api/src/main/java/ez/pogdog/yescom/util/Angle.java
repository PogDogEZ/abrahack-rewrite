package ez.pogdog.yescom.util;

import java.util.Objects;

public class Angle {

    private final float yaw;
    private final float pitch;

    public Angle(float yaw, float pitch) {
        // Fuck you
        this.yaw = (yaw % 360.0f >= 180.0f ? (yaw % 360.0f) - 360.0f : (yaw % 360.0f < -180.0f ? (yaw % 360.0f) + 360.0f : (yaw % 360.0f)));
        this.pitch = Math.max(-90.0f, Math.min(90.0f, pitch % 180.0f));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Angle angle = (Angle)other;
        return Float.compare(angle.yaw, yaw) == 0 && Float.compare(angle.pitch, pitch) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(yaw, pitch);
    }

    @Override
    public String toString() {
        return String.format("Angle(yaw=%.1f, pitch=%.1f)", yaw, pitch);
    }

    /**
     * Adds the given yaw and pitch to this angle.
     * @param yaw The amount of yaw.
     * @param pitch The amount of pitch.
     * @return The new angle.
     */
    public Angle add(float yaw, float pitch) {
        return new Angle(this.yaw + yaw, this.pitch + pitch);
    }

    public Angle add(Angle other) {
        return add(other.yaw, other.pitch);
    }

    /**
     * Subtracts the given yaw and pitch from this angle.
     * @param yaw The amount of yaw.
     * @param pitch The amount pitch.
     * @return The new angle.
     */
    public Angle subtract(float yaw, float pitch) {
        return new Angle(this.yaw - yaw, this.pitch - pitch);
    }

    public Angle subtract(Angle other) {
        return subtract(other.yaw, other.pitch);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
