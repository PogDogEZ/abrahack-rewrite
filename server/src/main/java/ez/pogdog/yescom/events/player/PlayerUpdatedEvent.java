package ez.pogdog.yescom.events.player;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.Player;
import ez.pogdog.yescom.util.Position;

public class PlayerUpdatedEvent extends ReporterEvent {

    private final Player player;
    private final UpdateType updateType;

    private final boolean canLogin;

    private final Position newPosition;
    private final Angle newAngle;

    private final int newDimension;

    private final float newHealth;
    private final int newHunger;
    private final float newSaturation;

    public PlayerUpdatedEvent(YCReporter reporter, Player player, boolean canLogin) {
        super(reporter);

        this.player = player;
        updateType = UpdateType.TOGGLE_LOGIN;

        this.canLogin = canLogin;

        newPosition = new Position(0, 0, 0);
        newAngle = new Angle(0, 0);

        newDimension = 0;

        newHealth = 20.0f;
        newHunger = 20;
        newSaturation = 5.0f;
    }

    public PlayerUpdatedEvent(YCReporter reporter, Player player, Position newPosition, Angle newAngle) {
        super(reporter);

        this.player = player;
        updateType = UpdateType.POSITION;

        canLogin = true;

        this.newPosition = newPosition;
        this.newAngle = newAngle;

        newDimension = 0;

        newHealth = 20.0f;
        newHunger = 20;
        newSaturation = 5.0f;
    }

    public PlayerUpdatedEvent(YCReporter reporter, Player player, int newDimension) {
        super(reporter);

        this.player = player;
        updateType = UpdateType.DIMENSION;

        canLogin = true;

        newPosition = new Position(0, 0, 0);
        newAngle = new Angle(0, 0);

        this.newDimension = newDimension;

        newHealth = 20.0f;
        newHunger = 20;
        newSaturation = 5.0f;
    }

    public PlayerUpdatedEvent(YCReporter reporter, Player player, float newHealth, int newHunger, float newSaturation) {
        super(reporter);

        this.player = player;
        updateType = UpdateType.HEALTH;

        canLogin = true;

        newPosition = new Position(0, 0, 0);
        newAngle = new Angle(0, 0);

        newDimension = 0;

        this.newHealth = newHealth;
        this.newHunger = newHunger;
        this.newSaturation = newSaturation;
    }

    public Player getPlayer() {
        return player;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    public boolean getCanLogin() {
        return canLogin;
    }

    public Position getNewPosition() {
        return newPosition;
    }

    public Angle getNewAngle() {
        return newAngle;
    }

    public int getNewDimension() {
        return newDimension;
    }

    public float getNewHealth() {
        return newHealth;
    }

    public int getNewHunger() {
        return newHunger;
    }

    public float getNewSaturation() {
        return newSaturation;
    }

    public enum UpdateType {
        TOGGLE_LOGIN, POSITION, DIMENSION, HEALTH;
    }
}
