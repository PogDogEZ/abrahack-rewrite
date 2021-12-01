package ez.pogdog.yescom.util;

import java.util.UUID;

/**
 * A player that will be coord exploiting.
 */
public class Player {

    private final String username;
    private final UUID uuid;
    private final String displayName;

    private Position position;
    private Angle angle;
    private int dimension;

    private float health;
    private int food;
    private float saturation;

    public Player(String username, UUID uuid, String displayName) {
        this.username = username;
        this.uuid = uuid;
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return String.format("Player(username=%s, displayName=%s)", username, displayName);
    }

    public String getUsername() {
        return username;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Angle getAngle() {
        return angle;
    }

    public void setAngle(Angle angle) {
        this.angle = angle;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public int getFood() {
        return food;
    }

    public void setFood(int food) {
        this.food = food;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }
}
