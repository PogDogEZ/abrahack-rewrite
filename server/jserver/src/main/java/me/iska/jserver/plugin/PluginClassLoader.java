package me.iska.jserver.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class PluginClassLoader extends URLClassLoader {

    private final List<ClassLoader> dependencyClassLoaders = new ArrayList<>();

    public PluginClassLoader(URL url, ClassLoader parent) {
        super(new URL[] { url }, parent);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException error) {
            for (ClassLoader dependencyClassLoader : dependencyClassLoaders) {
                try {
                    return dependencyClassLoader.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }

            throw error;
        }
    }

    public void addDependencyClassLoader(ClassLoader dependencyClassLoader) {
        dependencyClassLoaders.add(dependencyClassLoader);
    }

    public void removeDependencyClassLoader(ClassLoader dependencyClassLoader) {
        dependencyClassLoaders.remove(dependencyClassLoader);
    }
}
