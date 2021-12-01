package me.iska.jserver.managers;

import me.iska.jserver.IManager;
import me.iska.jserver.exception.PermissionException;
import me.iska.jserver.user.IPermission;
import me.iska.jserver.user.User;

public class PermissionManager implements IManager {

    @Override
    public void update() {
    }

    @Override
    public void exit() {
    }

    /**
     * Checks whether the current caller of a method has the specified permission.
     * @param permission The permission to check.
     * @return Whether the caller has the required permission.
     */
    public boolean hasPermission(IPermission permission) { // TODO: This
        for (StackTraceElement traceElement : Thread.currentThread().getStackTrace()) {
            String className = traceElement.getClassName();
        }

        return true;
    }

    /**
     * Checks whether the current caller of a method has the specified permission, if they do not, an exception is thrown.
     * @param permission The permission to check.
     * @throws PermissionException If the caller does not have the required permission.
     */
    public void checkPermission(IPermission permission) throws PermissionException {
        if (!hasPermission(permission)) throw new PermissionException("Insufficient permissions.");
    }
}
