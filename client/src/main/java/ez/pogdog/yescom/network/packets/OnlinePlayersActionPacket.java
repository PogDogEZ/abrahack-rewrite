package ez.pogdog.yescom.network.packets;

import ez.pogdog.yescom.network.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Used to inform the other party about players leaving and joining the server.
 */
@Packet.Info(name="online_players_action", id=YCRegistry.ID_OFFSET + 10, side=Packet.Side.BOTH)
public class OnlinePlayersActionPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private final Map<UUID, String> onlinePlayers = new HashMap<>();

    private Action action;

    public OnlinePlayersActionPacket(Action action, Map<UUID, String> onlinePlayers) {
        this.action = action;
        this.onlinePlayers.putAll(onlinePlayers);
    }

    public OnlinePlayersActionPacket(Map<UUID, String> onlinePlayers) {
        this(Action.ADD, onlinePlayers);
    }

    public OnlinePlayersActionPacket(List<UUID> onlinePlayers) {
        this(Action.REMOVE, new HashMap<>());

        for (UUID uuid : onlinePlayers) this.onlinePlayers.put(uuid, "");
    }

    public OnlinePlayersActionPacket() {
        this(Action.ADD, new HashMap<>());
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        onlinePlayers.clear();

        action = ACTION.read(inputStream);

        int onlinePlayersToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < onlinePlayersToRead; ++index) {
            UUID uuid = UUID.nameUUIDFromBytes(Registry.BYTES.read(inputStream));
            String name = action == Action.ADD ? Registry.STRING.read(inputStream) : "";
            onlinePlayers.put(uuid, name);
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);

        Registry.UNSIGNED_SHORT.write(onlinePlayers.size(), outputStream);
        for (Map.Entry<UUID, String> entry : onlinePlayers.entrySet()) {
            Registry.BYTES.write(ByteBuffer.allocate(16)
                    .putLong(entry.getKey().getMostSignificantBits())
                    .putLong(entry.getKey().getLeastSignificantBits())
                    .array(), outputStream);
            if (action == Action.ADD) Registry.STRING.write(entry.getValue(), outputStream);
        }
    }

    /**
     * @return A map of UUIDs to display names, indicating which players have joined or left the server.
     */
    public Map<UUID, String> getOnlinePlayers() {
        return new HashMap<>(onlinePlayers);
    }

    public void putOnlinePlayer(UUID uuid, String name) {
        onlinePlayers.put(uuid, name);
    }

    public void setOnlinePlayers(Map<UUID, String> onlinePlayers) {
        this.onlinePlayers.clear();
        this.onlinePlayers.putAll(onlinePlayers);
    }

    public void addOnlinePlayers(Map<UUID, String> onlinePlayers) {
        this.onlinePlayers.putAll(onlinePlayers);
    }

    public void removeOnlinePlayer(UUID uuid) {
        onlinePlayers.remove(uuid);
    }

    /**
     * @return The action that was performed (joining or leaving).
     */
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public enum Action {
        ADD, REMOVE;
    }
}