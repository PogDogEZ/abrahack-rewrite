package me.iska.jclient.impl.user;

import java.util.Objects;

public class Group {

    private final String name;
    private final int groupID;
    private final int defaultPermission;

    public Group(String name, int groupID, int defaultPermission) {
        this.name = name;
        this.groupID = groupID;
        this.defaultPermission = defaultPermission;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Group group = (Group)other;
        return groupID == group.groupID && defaultPermission == group.defaultPermission && name.equals(group.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, groupID, defaultPermission);
    }

    @Override
    public String toString() {
        return String.format("Group(name=%s, gid=%d)", name, groupID);
    }

    public String getName() {
        return name;
    }

    public int getGID() {
        return groupID;
    }

    public int getDefaultPermission() {
        return defaultPermission;
    }
}
