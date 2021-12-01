package me.iska.jclient.user;

/**
 * A permission for a user, contains the name of the permission and the level.
 */
public class Permission {

    private final String name;
    private final int level;

    public Permission(String name, int level) {
        this.name = name;
        this.level = level;
    }

    @Override
    public String toString() {
        return String.format("Permission(name=%s, level=%d)", name, level);
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }
}
