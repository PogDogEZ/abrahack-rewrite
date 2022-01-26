package ez.pogdog.yescom.network.packets;

import ez.pogdog.yescom.network.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent to the client/server to request that an account either be added or removed. Should support legacy auth (providing
 * just the username and password) as well as the new auth (providing the access and client tokens).
 */
@Packet.Info(name="account_action", id=YCRegistry.ID_OFFSET + 6, side=Packet.Side.BOTH)
public class AccountActionPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private Action action;
    private long actionID;
    private String username;

    private boolean legacy;

    private String password;

    private String accessToken;
    private String clientToken;

    public AccountActionPacket(Action action, long actionID, String username, boolean legacy, String password, String accessToken,
                               String clientToken) {
        this.action = action;
        this.actionID = actionID;
        this.username = username;
        this.legacy = legacy;
        this.password = password;
        this.accessToken = accessToken;
        this.clientToken = clientToken;
    }

    public AccountActionPacket(long actionID, String username, String password) {
        this(Action.LOGIN, actionID, username, true, password, "", "");
    }

    public AccountActionPacket(long actionID, String username, String accessToken, String clientToken) {
        this(Action.LOGIN, actionID, username, false, "", accessToken, clientToken);
    }

    public AccountActionPacket(long actionID, String username) {
        this(Action.LOGOUT, actionID, username, false, "", "", "");
    }

    public AccountActionPacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);
        actionID = Registry.LONG.read(inputStream);
        username = Registry.STRING.read(inputStream);

        if (action == Action.LOGIN) {
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
        Registry.LONG.write(actionID, outputStream);
        Registry.STRING.write(username, outputStream);

        if (action == Action.LOGIN) {
            Registry.BOOLEAN.write(legacy, outputStream);

            if (legacy) {
                Registry.STRING.write(password, outputStream);
            } else {
                Registry.STRING.write(accessToken, outputStream);
                Registry.STRING.write(clientToken, outputStream);
            }
        }
    }

    /**
     * @return The action to perform.
     */
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * @return The action ID, this is a unique number that is used to identify the action.
     */
    public long getActionID() {
        return actionID;
    }

    public void setActionID(long actionID) {
        this.actionID = actionID;
    }

    /**
     * @return The username of the account.
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return Whether the legacy auth is being used.
     */
    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    /**
     * @return The password of the account.
     */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return The access token of the account.
     */
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * @return The client token of the account.
     */
    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

    public enum Action {
        LOGIN, LOGOUT;
    }
}
