package ez.pogdog.yescom.network.packets;

import ez.pogdog.yescom.network.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A general action request packet, used to request more basic actions than things like adding accounts, such as sending
 * a chat message.
 */
@Packet.Info(name="action_request", id=YCRegistry.ID_OFFSET + 11, side=Packet.Side.BOTH)
public class ActionRequestPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private Action action;
    private long actionID;
    private byte[] data;

    public ActionRequestPacket(Action action, long actionID, byte[] data) {
        this.action = action;
        this.actionID = actionID;
        this.data = data.clone();
    }

    public ActionRequestPacket() {
        this(Action.SEND_CHAT_MESSAGE, -1, new byte[0]);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);
        actionID = Registry.LONG.read(inputStream);
        data = Registry.BYTES.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);
        Registry.LONG.write(actionID, outputStream);
        Registry.BYTES.write(data, outputStream);
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
     * @return The ID of the action.
     */
    public long getActionID() {
        return actionID;
    }

    public void setActionID(long actionID) {
        this.actionID = actionID;
    }

    /**
     * @return The data being sent with the action.
     */
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data.clone();
    }

    public enum Action {
        SEND_CHAT_MESSAGE,
        UNTRACK_PLAYER;
    }
}
