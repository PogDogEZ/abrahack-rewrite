package me.iska.jserver.user.groups;

import me.iska.jserver.exception.AuthenticationException;
import me.iska.jserver.exception.UserException;
import me.iska.jserver.user.Group;
import me.iska.jserver.user.IPermission;
import me.iska.jserver.user.User;

import java.util.List;

/**
 * Acts as a proxy for a group, can be used to make aliases for groups.
 */
public class ProxyGroup extends Group {

    private final Group group;

    public ProxyGroup(String alias, int id, Group group) {
        super(alias, id);

        this.group = group;
    }

    @Override
    public void addUser(User user) throws UserException {
        group.addUser(user);
    }

    @Override
    public void removeUser(User user) throws UserException {
        group.removeUser(user);
    }

    @Override
    public User getUser(String username) {
        return group.getUser(username);
    }

    @Override
    public User getUser(int id) {
        return group.getUser(id);
    }

    @Override
    public List<User> getUsers() {
        return group.getUsers();
    }

    @Override
    public boolean isLoggedIn(User user) {
        return group.isLoggedIn(user);
    }

    @Override
    public void login(User user, String password) throws AuthenticationException {
        group.login(user, password);
    }

    @Override
    public void logout(User user) throws AuthenticationException {
        group.logout(user);
    }

    /**
     * @return The group this proxy is proxying.
     */
    public Group getGroup() {
        return group;
    }
}
