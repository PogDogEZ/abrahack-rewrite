package ez.pogdog.yescom.network.packets;

import ez.pogdog.yescom.network.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * General action response packet sent by the client, used to indicate if an action was successful + an additional
 * message.
 */
@Packet.Info(name="action_response", id=YCRegistry.ID_OFFSET + 12, side=Packet.Side.BOTH)
public class ActionResponsePacket extends Packet {

    private long actionID;
    private boolean successful;
    private String message;

    public ActionResponsePacket(long actionID, boolean successful, String message) {
        this.actionID = actionID;
        this.successful = successful;
        this.message = message;
    }

    public ActionResponsePacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        actionID = Registry.LONG.read(inputStream);
        successful = Registry.BOOLEAN.read(inputStream);
        message = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.LONG.write(actionID, outputStream);
        Registry.BOOLEAN.write(successful, outputStream);
        Registry.STRING.write(message, outputStream);
    }

    /**
     * @return The unique action ID.
     */
    public long getActionID() {
        return actionID;
    }

    public void setActionID(long actionID) {
        this.actionID = actionID;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    /**
     * @return An extra message, could be an error or other.
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
