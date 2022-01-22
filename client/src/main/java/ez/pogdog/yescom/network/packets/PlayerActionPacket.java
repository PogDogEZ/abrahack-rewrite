package ez.pogdog.yescom.network.packets;

import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.Dimension;
import ez.pogdog.yescom.util.Position;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Sent by the client/server to indicate a change in a player's state.
 */
@Packet.Info(name="player_action", id=YCRegistry.ID_OFFSET + 7, side=Packet.Side.BOTH)
public class PlayerActionPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private Action action;

    private String playerName;
    private UUID uuid;
    private String displayName;

    private String disconnectReason;

    private boolean canLogin;

    private Position newPosition;
    private Angle newAngle;

    private Dimension newDimension;

    private Player.FoodStats newStats;

    public PlayerActionPacket(Action action, String playerName, UUID uuid, String displayName,
                              String disconnectReason, boolean canLogin, Position newPosition, Angle newAngle,
                              Dimension newDimension, Player.FoodStats newStats) {
        this.action = action;
        this.playerName = playerName;
        this.uuid = uuid;
        this.displayName = displayName;
        this.disconnectReason = disconnectReason;
        this.canLogin = canLogin;
        this.newPosition = newPosition;
        this.newAngle = newAngle;
        this.newDimension = newDimension;
        this.newStats = newStats;
    }

    public PlayerActionPacket(Action action, Player player) {
        this(action, player.getAuthService().getUsername(), player.getAuthService().getSelectedProfile().getId(),
                player.getAuthService().getSelectedProfile().getName(), "", player.getCanLogin(), player.getPosition(),
                player.getAngle(), player.getDimension(), player.getFoodStats());
    }

    public PlayerActionPacket(String username, UUID uuid, String displayName) {
        this(Action.ADD, username, uuid, displayName, "", true, new Position(0, 0, 0),
                new Angle(0, 0), Dimension.OVERWORLD, null);
    }

    public PlayerActionPacket(String username) {
        this(Action.REMOVE, username, null, "", "", true, new Position(0, 0, 0),
                new Angle(0, 0), Dimension.OVERWORLD, null);
    }

    public PlayerActionPacket(Player player, String disconnectReason) {
        this(Action.LOGOUT, player.getAuthService().getUsername(), player.getAuthService().getSelectedProfile().getId(),
                player.getAuthService().getSelectedProfile().getName(), disconnectReason, player.getCanLogin(), player.getPosition(),
                player.getAngle(), player.getDimension(), player.getFoodStats());
    }

    public PlayerActionPacket(String playerName, Position newPosition, Angle newAngle) {
        this(Action.UPDATE_POSITION, playerName, null, "", "", true, newPosition,
                newAngle, Dimension.OVERWORLD, null);
    }

    public PlayerActionPacket(String playerName, Dimension newDimension) {
        this(Action.UPDATE_DIMENSION, playerName, null, "", "", true,
                new Position(0, 0,0), new Angle(0.0f, 0.0f), newDimension, null);
    }

    public PlayerActionPacket(String playerName, Player.FoodStats newStats) {
        this(Action.UPDATE_HEALTH, playerName, null, "", "", true,
                new Position(0, 0, 0), new Angle(0.0f, 0.0f), Dimension.OVERWORLD, newStats);
    }

    public PlayerActionPacket() {
        this(Action.ADD, "", null, "", "", true, new Position(0, 0, 0),
                new Angle(0.0f, 0.0f), Dimension.OVERWORLD, null);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);
        playerName = Registry.STRING.read(inputStream);

        switch (action) {
            case ADD: {
                uuid = UUID.nameUUIDFromBytes(Registry.BYTES.read(inputStream));
                displayName = Registry.STRING.read(inputStream);
                break;
            }
            case LOGOUT: {
                disconnectReason = Registry.STRING.read(inputStream);
                break;
            }
            case TOGGLE_LOGIN: {
                canLogin = Registry.BOOLEAN.read(inputStream);
                break;
            }
            case UPDATE_POSITION: {
                newPosition = YCRegistry.POSITION.read(inputStream);
                newAngle = YCRegistry.ANGLE.read(inputStream);
                break;
            }
            case UPDATE_DIMENSION: {
                newDimension = Dimension.fromMC(Registry.SHORT.read(inputStream));
                break;
            }
            case UPDATE_HEALTH: {
                if (newStats == null) newStats = new Player.FoodStats(20.0f, 20, 5.0f);

                newStats.setHealth(Registry.FLOAT.read(inputStream));
                newStats.setHunger(Registry.UNSIGNED_SHORT.read(inputStream));
                newStats.setSaturation(Registry.FLOAT.read(inputStream));
                break;
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);
        Registry.STRING.write(playerName, outputStream);

        switch (action) {
            case ADD: {
                Registry.BYTES.write(ByteBuffer.allocate(16)
                        .putLong(uuid.getMostSignificantBits())
                        .putLong(uuid.getLeastSignificantBits())
                        .array(), outputStream);
                Registry.STRING.write(displayName, outputStream);
                break;
            }
            case LOGOUT: {
                Registry.STRING.write(disconnectReason, outputStream);
                break;
            }
            case TOGGLE_LOGIN: {
                Registry.BOOLEAN.write(canLogin, outputStream);
                break;
            }
            case UPDATE_POSITION: {
                YCRegistry.POSITION.write(newPosition, outputStream);
                YCRegistry.ANGLE.write(newAngle, outputStream);
                break;
            }
            case UPDATE_DIMENSION: {
                Registry.SHORT.write((short)newDimension.getMCDim(), outputStream);
                break;
            }
            case UPDATE_HEALTH: {
                Registry.FLOAT.write(newStats.getHealth(), outputStream);
                Registry.UNSIGNED_SHORT.write(newStats.getHunger(), outputStream);
                Registry.FLOAT.write(newStats.getSaturation(), outputStream);
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
     * @return The username of the player.
     */
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * @return The UUID of the player.
     */
    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * @return The in-game display name of the player.
     */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
     * @return Whether or not the player can log in automatically.
     */
    public boolean getCanLogin() {
        return canLogin;
    }

    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
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
    public Dimension getNewDimension() {
        return newDimension;
    }

    public void setNewDimension(Dimension newDimension) {
        this.newDimension = newDimension;
    }

    /**
     * @return The new food stats of the player.
     */
    public Player.FoodStats getNewStats() {
        return newStats;
    }

    public void setNewStats(Player.FoodStats newStats) {
        this.newStats = newStats;
    }

    public enum Action {
        ADD, REMOVE,
        LOGIN, LOGOUT,
        TOGGLE_LOGIN,
        UPDATE_POSITION,
        UPDATE_DIMENSION,
        UPDATE_HEALTH;
    }
}
