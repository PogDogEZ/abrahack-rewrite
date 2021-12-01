package me.iska.jclient.user;

import java.util.ArrayList;
import java.util.List;

/**
 * A clientside representation of a user.
 */
public class User {

    private final List<Permission> permissions = new ArrayList<>();

    private final String username;
    private final int userID;
    private final Group group;

    public User(String username, int userID, Group group, List<Permission> permissions) {
        this.username = username;
        this.userID = userID;
        this.group = group;
        this.permissions.addAll(permissions);
    }

    @Override
    public String toString() {
        return String.format("User(name=%s, uid=%d)", username, userID);
    }

    public List<Permission> getPermissions() {
        return new ArrayList<>(permissions);
    }

    public String getUsername() {
        return username;
    }

    public int getID() {
        return userID;
    }

    public Group getGroup() {
        return group;
    }
}
