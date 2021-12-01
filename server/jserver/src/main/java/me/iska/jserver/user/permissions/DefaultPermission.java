package me.iska.jserver.user.permissions;

import me.iska.jserver.user.IPermission;

import java.util.Arrays;
import java.util.Objects;

/**
 * The default permission level, determines what "level" a user is, i.e. admin, user, etc.
 */
public class DefaultPermission implements IPermission {

    private final Level level;

    public DefaultPermission(Level level) {
        this.level = level;
    }

    public DefaultPermission(int level) {
        this(Arrays.stream(Level.values())
                .filter(permLevel -> permLevel.getLevel() == level)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid permission level %d.", level))));
    }

    public DefaultPermission() {
        this(Level.USER);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        DefaultPermission that = (DefaultPermission)other;
        return level == that.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level);
    }

    @Override
    public String toString() {
        return String.format("DefaultPermission(level=%s)", level.name());
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public int getLevel() {
        return level.getLevel();
    }

    /**
     * The user permission level.
     */
    public enum Level {
        USER(0),
        ADMIN(1),
        SYSTEM(2);

        private final int level;

        Level(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }
}
