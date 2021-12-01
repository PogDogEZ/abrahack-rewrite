package me.iska.jserver.user;

/**
 * A base permission.
 */
public interface IPermission {

    /**
     * @return the name of the permission.
     */
    String getName();

    /**
     * @return The level of the permission.
     */
    int getLevel();
}
