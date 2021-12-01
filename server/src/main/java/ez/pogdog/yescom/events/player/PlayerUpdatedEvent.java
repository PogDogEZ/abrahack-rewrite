package ez.pogdog.yescom.events.player;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.Player;
import ez.pogdog.yescom.util.Position;

public class PlayerUpdatedEvent extends ReporterEvent {

    private final Player player;
    private final UpdateType updateType;

    private final Position newPosition;
    private final Angle newAngle;

    private final int newDimension;

    private final float newHealth;
    private final int newHunger;
    private final float newSaturation;

    public PlayerUpdatedEvent(YCReporter reporter, Player player, Position newPosition, Angle newAngle) {
        super(reporter);

        this.player = player;
        updateType = UpdateType.POSITION;

        this.newPosition = newPosition;
        this.newAngle = newAngle;

        this.newDimension = 0;

        this.newHealth = 20.0f;
        this.newHunger = 20;
        this.newSaturation = 5.0f;
    }

    public PlayerUpdatedEvent(YCReporter reporter, Player player, int newDimension) {
        super(reporter);

        this.player = player;
        updateType = UpdateType.DIMENSION;

        this.newPosition = new Position(0, 0, 0);
        this.newAngle = new Angle(0, 0);

        this.newDimension = newDimension;

        this.newHealth = 20.0f;
        this.newHunger = 20;
        this.newSaturation = 5.0f;
    }

    public PlayerUpdatedEvent(YCReporter reporter, Player player, float newHealth, int newHunger, float newSaturation) {
        super(reporter);

        this.player = player;
        updateType = UpdateType.HEALTH;

        this.newPosition = new Position(0, 0, 0);
        this.newAngle = new Angle(0, 0);

        this.newDimension = 0;

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
        POSITION, DIMENSION, HEALTH;
    }
}
