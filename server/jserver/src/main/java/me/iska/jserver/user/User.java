package me.iska.jserver.user;

import me.iska.jserver.exception.UserException;

import java.util.*;

public class User {

    private final Set<IPermission> permissions = new HashSet<>();

    private final String username;
    private final int id;

    private Group group; // The group this user belongs to, effectively final

    public User(String username, int id, IPermission... permissions) {
        this.username = username;
        this.id = id;
        this.permissions.addAll(Arrays.asList(permissions));
    }

    protected void addPermission(IPermission permission) {
        permissions.add(permission);
    }

    protected void removePermission(IPermission permission) {
        permissions.remove(permission);
    }

    /* ----------------------------- Setters and getters ----------------------------- */

    public List<IPermission> getPermissions() {
        List<IPermission> permissions = new ArrayList<>(this.permissions);
        if (group != null) {
            group.getPermissions().forEach(permission -> {
                if (!permissions.contains(permission)) permissions.add(permission); // Prevent duplicates
            });
        }
        return permissions;
    }

    public String getUsername() {
        return username;
    }

    public int getID() {
        return id;
    }

    public Group getGroup() {
        return group;
    }

    /**
     * Sets the group that this user belongs to.
     * @param group The group to set.
     * @throws UserException If the user is already assigned to a group.
     */
    public void setGroup(Group group) throws UserException {
        if (this.group == null) {
            this.group = group;
        } else {
            throw new UserException("User is already registered to a group.");
        }
    }

    public boolean isLoggedIn() {
        return group != null && group.isLoggedIn(this);
    }
}
