package me.iska.jserver;

import me.iska.jserver.event.EventBus;
import me.iska.jserver.exception.UserException;
import me.iska.jserver.managers.ConnectionManager;
import me.iska.jserver.managers.PermissionManager;
import me.iska.jserver.managers.PluginManager;
import me.iska.jserver.managers.UserManager;
import me.iska.jserver.user.groups.SystemGroup;
import me.iska.jserver.user.permissions.DefaultPermission;

import java.io.File;
import java.util.logging.Logger;

public class JServer {

    private static JServer instance;
    private static Logger logger = Logger.getLogger("jserver");

    public static JServer getInstance() {
        return instance;
    }

    public static Logger getLogger() {
        return logger;
    }

    public final EventBus eventBus = new EventBus();

    public final ConnectionManager connectionManager;
    public final PermissionManager permissionManager;
    public final PluginManager pluginManager;
    public final UserManager userManager;

    private final String serverName;
    private final int protocolVersion;
    private final File workingDirectory;

    public JServer(String serverName, int protocolVersion, File workingDirectory, String host, int port) {
        instance = this;

        this.serverName = serverName;
        this.protocolVersion = protocolVersion;
        this.workingDirectory = workingDirectory;

        logger.info(String.format("JServer name %s, protocol version %d.", serverName, protocolVersion));

        logger.fine("Starting managers...");
        connectionManager = new ConnectionManager(host, port);
        permissionManager = new PermissionManager();
        pluginManager = new PluginManager();
        userManager = new UserManager();

        logger.fine("Adding system group...");
        try {
            userManager.addGroup(new SystemGroup(userManager.getCurrentGroupID()));
        } catch (UserException error) {
            logger.severe("Error while adding system group:");
            logger.throwing(JServer.class.getSimpleName(), "JServer", error);
            exit();
        }

        pluginManager.loadPlugins();

        logger.info("JServer started.");
    }

    /**
     * Exits the server.
     */
    public void exit() {
        long startTime = System.currentTimeMillis();
        // permissionManager.checkPermission(new DefaultPermission(DefaultPermission.Level.ADMIN));

        connectionManager.exit();
        permissionManager.exit();
        pluginManager.exit();
        userManager.exit();

        logger.info(String.format("Shutdown took %dms.", System.currentTimeMillis() - startTime));
    }

    public String getServerName() {
        return serverName;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }
}
