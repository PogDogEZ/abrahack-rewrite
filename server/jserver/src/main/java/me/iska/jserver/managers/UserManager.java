package me.iska.jserver.managers;

import me.iska.jserver.IManager;
import me.iska.jserver.JServer;
import me.iska.jserver.exception.AuthenticationException;
import me.iska.jserver.user.Group;
import me.iska.jserver.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UserManager implements IManager {

    private final Logger logger = JServer.getLogger();

    private final List<Group> groups = new ArrayList<>();

    private int currentGroupID;

    public UserManager() {
        currentGroupID = 0;
    }

    @Override
    public void update() {
    }

    @Override
    public void exit() {
    }

    /**
     * Logs a user in given the authentication details.
     * @param username The username of the user.
     * @param password The password of the user.
     * @param groupName The name of the group the user is in.
     * @return The user that was logged in.
     * @throws AuthenticationException If the user could not be logged in.
     */
    public User login(String username, String password, String groupName) throws AuthenticationException {
        Group group;
        User user;
        try {
            group = getGroup(groupName);
            user = group.getUser(username);
        } catch (IllegalArgumentException error) {
            throw new AuthenticationException(error.getMessage());
        }

        group.login(user, password);
        return user;
    }

    public void logout(User user) throws AuthenticationException {
        if (!user.isLoggedIn()) throw new AuthenticationException("Attempted to logout a user that is not logged in");
        if (user.getGroup() == null) throw new AuthenticationException("Attempted to logout a user that is not in a group");

        user.getGroup().logout(user);
    }

    /**
     * Returns a group registered under the given name.
     * @param name The name of the group.
     * @return The group if found.
     * @throws IllegalArgumentException If the group is not found.
     */
    public Group getGroup(String name) {
        return groups.stream()
                .filter(group -> group.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Group by name '%s' not found.", name)));
    }

    /**
     * Returns a group registered under the given ID.
     * @param groupID The ID of the group.
     * @return The group if found.
     * @throws IllegalArgumentException If the group is not found.
     */
    public Group getGroup(int groupID) {
        return groups.stream()
                .filter(group -> group.getID() == groupID)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Group by ID %d not found.", groupID)));
    }

    public void addGroup(Group group) { // TODO: Permissions check
        if (!groups.contains(group)) {
            logger.fine(String.format("Adding group: %s.", group));
            groups.add(group);

            ++currentGroupID;
        }
    }

    public int getCurrentGroupID() {
        return currentGroupID;
    }
}
