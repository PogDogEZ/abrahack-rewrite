package me.iska.jserver.plugin;

import me.iska.jserver.exception.PluginException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A plugin is a class that implements this interface.
 */
public interface IPlugin {

    void load() throws PluginException;
    void unload() throws PluginException;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Info {
        /**
         * The plugin's name.
         */
        String name();

        /**
         * The plugin's version.
         */
        String version();

        /**
         * The dependencies of the plugin.
         */
        String[] dependencies();
    }
}
