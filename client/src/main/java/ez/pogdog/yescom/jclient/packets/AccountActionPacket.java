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

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private Action action;
    private long actionID;
    private String username;

    private boolean legacy;

    private String password;

    private String accessToken;
    private String clientToken;

    public AccountActionPacket(Action action, int actionID, String username, boolean legacy, String password, String accessToken,
                               String clientToken) {
        this.action = action;
        this.actionID = actionID;
        this.username = username;
        this.legacy = legacy;
        this.password = password;
        this.accessToken = accessToken;
        this.clientToken = clientToken;
    }

    public AccountActionPacket(int actionID, String username, String password) {
        this(Action.ADD, actionID, username, true, password, "", "");
    }

    public AccountActionPacket(int actionID, String username, String accessToken, String clientToken) {
        this(Action.ADD, actionID, username, false, "", accessToken, clientToken);
    }

    public AccountActionPacket(int actionID, String username) {
        this(Action.REMOVE, actionID, username, false, "", "", "");
    }

    public AccountActionPacket() {
        this(Action.ADD, 0, "", false, "", "", "");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);
        actionID = Registry.UNSIGNED_INT.read(inputStream);
        username = Registry.STRING.read(inputStream);

        if (action == Action.ADD) {
            legacy = Registry.BOOLEAN.read(inputStream);

            if (legacy) {
                password = Registry.STRING.read(inputStream);
            } else {
                accessToken = Registry.STRING.read(inputStream);
                clientToken = Registry.STRING.read(inputStream);
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);
        Registry.UNSIGNED_INT.write(actionID, outputStream);
        Registry.STRING.write(username, outputStream);

        if (action == Action.ADD) {
            Registry.BOOLEAN.write(legacy, outputStream);

            if (legacy) {
                Registry.STRING.write(password, outputStream);
            } else {
                Registry.STRING.write(accessToken, outputStream);
                Registry.STRING.write(clientToken, outputStream);
            }
        }
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

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

    public enum Action {
        ADD, REMOVE;
    }
}
