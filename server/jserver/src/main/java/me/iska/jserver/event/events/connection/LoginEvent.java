package me.iska.jserver.event.events.connection;

import me.iska.jserver.event.EventCancellable;
import me.iska.jserver.network.Connection;

/**
 * Posted after a client login has been attempted, but before the results have been sent to the client.
 */
public class LoginEvent extends EventCancellable {

    private final Connection connection;

    private final String username;
    private final String password;
    private final String groupName;

    private boolean successful;
    private String message;

    public LoginEvent(Connection connection, String username, String password, String groupName, boolean successful, String message) {
        this.connection = connection;
        this.username = username;
        this.password = password;
        this.groupName = groupName;
        this.successful = successful;
        this.message = message;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getGroupName() {
        return groupName;
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
