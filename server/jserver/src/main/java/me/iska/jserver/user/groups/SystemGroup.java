package me.iska.jserver.user.groups;

import me.iska.jserver.exception.AuthenticationException;
import me.iska.jserver.exception.UserException;
import me.iska.jserver.user.Group;
import me.iska.jserver.user.User;
import me.iska.jserver.user.permissions.DefaultPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for logging in system users.
 */
public class SystemGroup extends Group {

    private final List<User> users = new ArrayList<>();
    private final List<User> onlineUsers = new ArrayList<>();

    public SystemGroup(int id) throws UserException {
        super("system", id);

        addPermission(new DefaultPermission(DefaultPermission.Level.SYSTEM)); // Pretty obviously, but these are system users

        users.add(new User("system", 0));
        // users.add(new User(""));

        for (User user : users) user.setGroup(this); // Register all users under this group
    }

    @Override
    public void addUser(User user) throws UserException {
        throw new UserException("Cannot add new users to system group.");
    }

    @Override
    public void removeUser(User user) throws UserException {
        throw new UserException("Cannot remove users from the system group.");
    }

    @Override
    public User getUser(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("User by name %s not found.", username)));
    }

    @Override
    public User getUser(int id) {
        return users.stream()
                .filter(user -> user.getID() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("User by id %d not found.", id)));
    }

    @Override
    public List<User> getUsers() {
        return new ArrayList<>(users);
    }

    @Override
    public boolean isLoggedIn(User user) {
        return onlineUsers.contains(user);
    }

    @Override
    public void login(User user, String password) throws AuthenticationException {
        if (!onlineUsers.contains(user)) {
            onlineUsers.add(user);
        } else {
            throw new AuthenticationException("User is already logged in.");
        }
    }

    @Override
    public void logout(User user) throws AuthenticationException {
        throw new AuthenticationException("Cannot logout system user.");
    }
}
