package me.iska.jserver.user;

import me.iska.jserver.exception.AuthenticationException;
import me.iska.jserver.exception.UserException;

import java.util.*;

/**
 * A group, these contain users, duh.
 */
public abstract class Group {

    private final Set<IPermission> permissions = new HashSet<>();

    private final String name;
    private final int id;

    public Group(String name, int id, IPermission... permissions) {
        this.name = name;
        this.id = id;
        this.permissions.addAll(Arrays.asList(permissions));
    }

    /**
     * Adds a default permission to this group. This permission is used for every user that this group contains.
     * @param permission The permission to add as default.
     */
    protected void addPermission(IPermission permission) {
        permissions.add(permission);
    }

    /**
     * Removes a default permission from this group.
     * @param permission The permission to remove.
     */
    protected void removePermission(IPermission permission) {
        permissions.remove(permission);
    }

    /* ----------------------------- Abstract methods ----------------------------- */

    /**
     * Adds a user to the group.
     * @param user The user to add.
     * @throws UserException Thrown for many reasons.
     */
    public abstract void addUser(User user) throws UserException;

    /**
     * Removes a user from the group.
     * @param user The user to remove.
     * @throws UserException Thrown for many reasons.
     */
    public abstract void removeUser(User user) throws UserException;

    public abstract User getUser(String username);
    public abstract User getUser(int id);

    public abstract List<User> getUsers();

    public abstract boolean isLoggedIn(User user);

    /**
     * Logs a user into this group.
     * @param user The user to login.
     * @throws AuthenticationException Thrown for many reasons.
     */
    public abstract void login(User user, String password) throws AuthenticationException;

    /**
     * Logs a user out of this group.
     * @param user The user to logout.
     * @throws AuthenticationException Thrown for many reasons.
     */
    public abstract void logout(User user) throws AuthenticationException;

    /* ----------------------------- Setters and getters ----------------------------- */

    public List<IPermission> getPermissions() {
        return new ArrayList<>(permissions);
    }

    public String getName() {
        return name;
    }

    /**
     * @return The group ID as assigned by the user manager.
     */
    public int getID() {
        return id;
    }
}
