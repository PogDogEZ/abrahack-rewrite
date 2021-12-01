package me.iska.jserver.managers;

import me.iska.jserver.IManager;
import me.iska.jserver.JServer;
import me.iska.jserver.exception.PluginException;
import me.iska.jserver.plugin.IPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class PluginManager implements IManager {

    private final File PLUGINS_DIR = Paths.get("plugins").toFile();

    private final JServer jServer = JServer.getInstance();
    private final Logger logger = JServer.getLogger();

    private final List<IPlugin> plugins = new ArrayList<>();

    public PluginManager() {
        if (!PLUGINS_DIR.exists()) {
            logger.fine("Plugins directory does not exist, creating one.");
            if (!PLUGINS_DIR.mkdir()) logger.warning("Could not create plugins directory.");
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void exit() {
        plugins.forEach(plugin -> {
            try {
                plugin.unload();
            } catch (PluginException error) {
                logger.warning(String.format("Couldn't unload plugin %s:", plugin.getClass().getAnnotation(IPlugin.Info.class).name()));
                logger.throwing(PluginManager.class.getName(), "exit", error);
            }
        });
    }

    private void loadPlugin(File file, JarFile jarFile) throws IOException, PluginException {
        Attributes mainAttributes = jarFile.getManifest().getMainAttributes();
        String mainClass = mainAttributes.getValue("Plugin-Main-Class");
        if (mainClass == null) throw new PluginException("No main class specified in manifest.");

        logger.fine(String.format("Loading plugin %s...", jarFile.getName()));
        logger.finer(String.format("Main class: %s", mainClass));

        URLClassLoader classLoader = new URLClassLoader(new URL[] { file.toPath().toUri().toURL() }, getClass().getClassLoader());

        logger.finer("Loading main class...");
        Class<?> pluginClass;
        try {
            pluginClass = classLoader.loadClass(mainClass);
        } catch (ClassNotFoundException error) {
            throw new PluginException("Couldn't load the main class.");
        }

        if (!pluginClass.isAnnotationPresent(IPlugin.Info.class)) throw new PluginException("Main class is not annotated with @Plugin.Info.");
        IPlugin.Info info = pluginClass.getAnnotation(IPlugin.Info.class);

        logger.fine(String.format("Plugin name: %s, version: %s", info.name(), info.version()));

        logger.finer("Creating instance...");
        IPlugin plugin;
        try {
            plugin = (IPlugin)pluginClass.newInstance();
        } catch (InstantiationException | IllegalAccessException error) {
            throw new PluginException("Couldn't instantiate the main class.");
        }

        plugins.add(plugin);

        logger.fine("Done.");
    }

    /**
     * Loads the plugins, duh.
     */
    @SuppressWarnings("ConstantConditions")
    public void loadPlugins() {
        logger.info("Loading plugins...");

        if (!PLUGINS_DIR.exists() || !PLUGINS_DIR.isDirectory()) {
            logger.warning("Plugins directory is not a directory or does not exist.");
            return;
        }

        for (File file : PLUGINS_DIR.listFiles()) {
            JarFile jarFile;
            try {
                jarFile = new JarFile(file);
            } catch (IOException error) {
                continue;
            }

            try {
                loadPlugin(file, jarFile);
            } catch (IOException | PluginException error) {
                logger.warning(String.format("Couldn't load the plugin %s:", file));
                logger.throwing(PluginManager.class.getName(), "loadPlugins", error);
            }
        }

        for (IPlugin plugin : plugins) {
            // TODO: Check dependencies

            try {
                plugin.load();
            } catch (PluginException error) {
                logger.warning(String.format("Couldn't load plugin %s:", plugin.getClass().getAnnotation(IPlugin.Info.class).name()));
                logger.throwing(PluginManager.class.getName(), "loadPlugins", error);
            }
        }

        logger.info(String.format("Done, %d plugin(s) loaded.", plugins.size()));
    }
}
