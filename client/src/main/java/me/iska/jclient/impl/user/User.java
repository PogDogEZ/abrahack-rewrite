package me.iska.jclient.impl.user;

import java.util.Objects;

public class User {

    private final String username;
    private final int userID;
    private final int permission;
    private final Group group;

    public User(String username, int userID, int permission, Group group) {
        this.username = username;
        this.userID = userID;
        this.permission = permission;
        this.group = group;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        User user = (User)other;
        return userID == user.userID && permission == user.permission && username.equals(user.username) && group.equals(user.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, userID, permission, group);
    }

    @Override
    public String toString() {
        return String.format("User(name=%s, uid=%d)", username, userID);
    }

    public String getUsername() {
        return username;
    }

    public int getUID() {
        return userID;
    }

    public int getPermission() {
        return permission;
    }

    public Group getGroup() {
        return group;
    }
}
