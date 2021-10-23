package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.Dimension;
import ez.pogdog.yescom.util.Position;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="player_action", id=YCRegistry.ID_OFFSET + 12, side=Packet.Side.CLIENT)
public class PlayerActionPacket extends Packet {

    private Action action;

    private Player player;

    private String playerName;

    private Position newPosition;
    private Angle newAngle;

    private Dimension newDimension;

    private Player.FoodStats newStats;

    public PlayerActionPacket(Action action, Player player, String playerName, Position newPosition, Angle newAngle,
                              Dimension newDimension, Player.FoodStats newStats) {
        this.action = action;
        this.player = player;
        this.playerName = playerName;
        this.newPosition = newPosition;
        this.newAngle = newAngle;
        this.newDimension = newDimension;
        this.newStats = newStats;
    }

    public PlayerActionPacket(Action action, Player player) {
        this(action, player, player.getAuthService().getUsername(), player.getPosition(), player.getAngle(),
                player.getDimension(), player.getFoodStats());
    }

    public PlayerActionPacket(String playerName, Position newPosition, Angle newAngle) {
        this(Action.UPDATE_POSITION, null, playerName, newPosition, newAngle, Dimension.OVERWORLD, null);
    }

    public PlayerActionPacket(String playerName, Dimension newDimension) {
        this(Action.UPDATE_DIMENSION, null, playerName, new Position(0, 0,0), new Angle(0.0f, 0.0f),
                newDimension, null);
    }

    public PlayerActionPacket(String playerName, Player.FoodStats newStats) {
        this(Action.UPDATE_HEALTH, null, playerName, new Position(0, 0, 0), new Angle(0.0f, 0.0f),
                Dimension.OVERWORLD, newStats);
    }

    public PlayerActionPacket() {
        this(Action.ADD, null, "", new Position(0, 0, 0), new Angle(0.0f, 0.0f),
                Dimension.OVERWORLD, null);
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
                newDimension = Dimension.fromMC(Registry.SHORT.read(inputStream));
                break;
            }
            case UPDATE_HEALTH: {
                if (newStats == null) newStats = new Player.FoodStats(20.0f, 20, 5.0f);

                playerName = Registry.STRING.read(inputStream);
                newStats.setHealth(Registry.FLOAT.read(inputStream));
                newStats.setHunger(Registry.UNSIGNED_SHORT.read(inputStream));
                newStats.setSaturation(Registry.FLOAT.read(inputStream));
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
                Registry.SHORT.write((short)newDimension.getMCDim(), outputStream);
                break;
            }
            case UPDATE_HEALTH: {
                Registry.STRING.write(playerName, outputStream);
                Registry.FLOAT.write(newStats.getHealth(), outputStream);
                Registry.UNSIGNED_SHORT.write(newStats.getHunger(), outputStream);
                Registry.FLOAT.write(newStats.getSaturation(), outputStream);
                break;
            }
        }
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Position getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(Position newPosition) {
        this.newPosition = newPosition;
    }

    public Angle getNewAngle() {
        return newAngle;
    }

    public void setNewAngle(Angle newAngle) {
        this.newAngle = newAngle;
    }

    public Dimension getNewDimension() {
        return newDimension;
    }

    public void setNewDimension(Dimension newDimension) {
        this.newDimension = newDimension;
    }

    public Player.FoodStats getNewStats() {
        return newStats;
    }

    public void setNewStats(Player.FoodStats newStats) {
        this.newStats = newStats;
    }

    public enum Action {
        ADD, REMOVE,
        UPDATE_POSITION,
        UPDATE_DIMENSION,
        UPDATE_HEALTH;
    }
}
