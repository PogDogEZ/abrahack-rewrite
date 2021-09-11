package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="account_action_response", id=YCRegistry.ID_OFFSET + 3, side=Packet.Side.CLIENT)
public class AccountActionResponsePacket extends Packet {

    private long actionID;
    private boolean successful;
    private String message;

    public AccountActionResponsePacket(long actionID, boolean successful, String message) {
        this.actionID = actionID;
        this.successful = successful;
        this.message = message;
    }

    public AccountActionResponsePacket() {
        this(0, true, "");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        actionID = Registry.UNSIGNED_INT.read(inputStream);
        successful = Registry.BOOLEAN.read(inputStream);
        message = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_INT.write(actionID, outputStream);
        Registry.BOOLEAN.write(successful, outputStream);
        Registry.STRING.write(message, outputStream);
    }

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
