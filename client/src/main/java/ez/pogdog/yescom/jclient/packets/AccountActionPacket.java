package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="account_action", id=YCRegistry.ID_OFFSET + 10, side=Packet.Side.SERVER)
public class AccountActionPacket extends Packet {

    private Action action;
    private long actionID;
    private String username;
    private String password;

    public AccountActionPacket(Action action, int actionID, String username, String password) {
        this.action = action;
        this.actionID = actionID;
        this.username = username;
        this.password = password;
    }

    public AccountActionPacket() {
        this(Action.ADD, 0, "", "");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = new EnumType<>(Action.class).read(inputStream);
        actionID = Registry.UNSIGNED_INT.read(inputStream);
        username = Registry.STRING.read(inputStream);

        if (action == Action.ADD) password = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        new EnumType<>(Action.class).write(action, outputStream);
        Registry.UNSIGNED_INT.write(actionID, outputStream);
        Registry.STRING.write(username, outputStream);

        if (action == Action.ADD) Registry.STRING.write(password, outputStream);
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public long getActionID() {
        return actionID;
    }

    public void setActionID(long actionID) {
        this.actionID = actionID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public enum Action {
        ADD, REMOVE;
    }
}
