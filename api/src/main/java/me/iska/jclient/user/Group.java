package me.iska.jclient.user;

import java.util.ArrayList;
import java.util.List;

/**
 * A clientside representation of a group.
 */
public class Group {

    private final List<Permission> permissions = new ArrayList<>();

    private final String name;
    private final int groupID;

    public Group(String name, int groupID, List<Permission> permissions) {
        this.name = name;
        this.groupID = groupID;
        this.permissions.addAll(permissions);
    }

    @Override
    public String toString() {
        return String.format("Group(name=%s, gid=%d)", name, groupID);
    }

    public List<Permission> getPermissions() {
        return new ArrayList<>(permissions);
    }

    public String getName() {
        return name;
    }

    public int getID() {
        return groupID;
    }
}
