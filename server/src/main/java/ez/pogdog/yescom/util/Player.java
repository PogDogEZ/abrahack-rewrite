package ez.pogdog.yescom.util;

import java.util.UUID;

/**
 * A player that will be coord exploiting.
 */
public class Player {

    private final String username;
    private final UUID uuid;
    private final String displayName;

    private boolean canLogin;
    private boolean loggedIn;

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

        canLogin = true;
        loggedIn = false;
        position = new Position(0, 0, 0);
        angle = new Angle(0, 0);
        dimension = 0;
        health = 20.0f;
        food = 20;
        saturation = 5.0f;
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

    public boolean getCanLogin() {
        return canLogin;
    }

    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
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
