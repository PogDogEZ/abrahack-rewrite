package ez.pogdog.yescom.network.packets.shared;

import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.Player;
import ez.pogdog.yescom.util.Position;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by the client/server to indicate a change in a player's state.
 */
@Packet.Info(name="player_action", id=YCRegistry.ID_OFFSET + 7, side=Packet.Side.BOTH)
public class PlayerActionPacket extends Packet {

    private Action action;

    private Player player;

    private String playerName;
    private String disconnectReason;

    private Position newPosition;
    private Angle newAngle;

    private int newDimension;

    private float newHealth;
    private int newHunger;
    private float newSaturation;

    public PlayerActionPacket(Action action, Player player, String playerName, String disconnectReason,
                              Position newPosition, Angle newAngle, int newDimension,
                              float newHealth, int newHunger, float newSaturation) {
        this.action = action;
        this.player = player;
        this.playerName = playerName;
        this.disconnectReason = disconnectReason;
        this.newPosition = newPosition;
        this.newAngle = newAngle;
        this.newDimension = newDimension;
        this.newHealth = newHealth;
        this.newHunger = newHunger;
        this.newSaturation = newSaturation;
    }

    public PlayerActionPacket(Player player, String disconnectReason) {
        this(Action.REMOVE, player, player.getUsername(), disconnectReason, player.getPosition(), player.getAngle(),
                player.getDimension(), player.getHealth(), player.getFood(), player.getSaturation());
    }

    public PlayerActionPacket(Player player) {
        this(Action.ADD, player, player.getUsername(), "", player.getPosition(), player.getAngle(),
                player.getDimension(), player.getHealth(), player.getFood(), player.getSaturation());
    }

    public PlayerActionPacket(String playerName, Position newPosition, Angle newAngle) {
        this(Action.UPDATE_POSITION, null, playerName, "", newPosition, newAngle, 0,
                20.0f, 20, 5.0f);
    }

    public PlayerActionPacket(String playerName, int newDimension) {
        this(Action.UPDATE_DIMENSION, null, playerName, "", new Position(0, 0,0),
                new Angle(0.0f, 0.0f), newDimension, 20.0f, 20, 5.0f);
    }

    public PlayerActionPacket(String playerName, float newHealth, int newHunger, float newSaturation) {
        this(Action.UPDATE_HEALTH, null, playerName, "", new Position(0, 0, 0),
                new Angle(0.0f, 0.0f), 0, newHealth, newHunger, newSaturation);
    }

    public PlayerActionPacket() {
        this(Action.ADD, null, "", "", new Position(0, 0, 0),
                new Angle(0.0f, 0.0f), 0, 20.0f, 20, 5.0f);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = new EnumType<>(Action.class).read(inputStream);

        switch (action) {
            case ADD: {
                player = YCRegistry.PLAYER.read(inputStream);
                break;
            }
            case REMOVE: {
                playerName = Registry.STRING.read(inputStream);
                disconnectReason = Registry.STRING.read(inputStream);
                break;
            }
            case UPDATE_POSITION: {
                playerName = Registry.STRING.read(inputStream);
                newPosition = YCRegistry.POSITION.read(inputStream);
                newAngle = YCRegistry.ANGLE.read(inputStream);
                break;
            }
            case UPDATE_DIMENSION: {
                playerName = Registry.STRING.read(inputStream);
                newDimension = Registry.SHORT.read(inputStream);
                break;
            }
            case UPDATE_HEALTH: {
                playerName = Registry.STRING.read(inputStream);
                newHealth = Registry.FLOAT.read(inputStream);
                newHunger = Registry.UNSIGNED_SHORT.read(inputStream);
                newSaturation = Registry.FLOAT.read(inputStream);
                break;
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        new EnumType<>(Action.class).write(action, outputStream);

        switch (action) {
            case ADD: {
                YCRegistry.PLAYER.write(player, outputStream);
                break;
            }
            case REMOVE: {
                Registry.STRING.write(playerName, outputStream);
                Registry.STRING.write(disconnectReason, outputStream);
                break;
            }
            case UPDATE_POSITION: {
                Registry.STRING.write(playerName, outputStream);
                YCRegistry.POSITION.write(newPosition, outputStream);
                YCRegistry.ANGLE.write(newAngle, outputStream);
                break;
            }
            case UPDATE_DIMENSION: {
                Registry.STRING.write(playerName, outputStream);
                Registry.SHORT.write((short)newDimension, outputStream);
                break;
            }
            case UPDATE_HEALTH: {
                Registry.STRING.write(playerName, outputStream);
                Registry.FLOAT.write(newHealth, outputStream);
                Registry.UNSIGNED_SHORT.write(newHunger, outputStream);
                Registry.FLOAT.write(newSaturation, outputStream);
                break;
            }
        }
    }

    /**
     * @return The action being performed.
     */
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * @return The username of the player.
     */
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * @return The reason the player was disconnected.
     */
    public String getDisconnectReason() {
        return disconnectReason;
    }

    public void setDisconnectReason(String disconnectReason) {
        this.disconnectReason = disconnectReason;
    }

    /**
     * @return The updated position of the player.
     */
    public Position getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(Position newPosition) {
        this.newPosition = newPosition;
    }

    /**
     * @return The updated angle of the player.
     */
    public Angle getNewAngle() {
        return newAngle;
    }

    public void setNewAngle(Angle newAngle) {
        this.newAngle = newAngle;
    }

    /**
     * @return The updated dimension of the player.
     */
    public int getNewDimension() {
        return newDimension;
    }

    public void setNewDimension(int newDimension) {
        this.newDimension = newDimension;
    }

    /**
     * @return The updated health of the player.
     */
    public float getNewHealth() {
        return newHealth;
    }

    public void setNewHealth(float newHealth) {
        this.newHealth = newHealth;
    }

    /**
     * @return The updated hunger of the player.
     */
    public int getNewHunger() {
        return newHunger;
    }

    public void setNewHunger(int newHunger) {
        this.newHunger = newHunger;
    }

    /**
     * @return The updated saturation of the player.
     */
    public float getNewSaturation() {
        return newSaturation;
    }

    public void setNewSaturation(float newSaturation) {
        this.newSaturation = newSaturation;
    }

    public enum Action {
        ADD, REMOVE,
        UPDATE_POSITION,
        UPDATE_DIMENSION,
        UPDATE_HEALTH;
    }
}
