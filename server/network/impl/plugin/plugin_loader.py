#!/usr/bin/env python3

import os

from importlib.machinery import SourceFileLoader

from network.impl.plugin import BasePlugin


class PluginLoader:

    PYTHON_EXTENSIONS = [".py"]

    def __init__(self, system, directory: str) -> None:
        self.system = system
        self.directory = directory

        self.found_plugins = []

        self.system.logger.info("Checking for plugins in directory '%s'..." % self.directory)

        try:
            self._crawl(self.directory)
        except Exception as error:
            self.system.logger.error("An error occurred while checking for plugins, %r." % error)
            return

        self.system.logger.debug("Plugins found: %r." % self.found_plugins)

        self.system.logger.info("Found %i plugin(s)." % len(self.found_plugins))
        self.system.logger.info("Attempting to load all plugins...")

        self.load_plugins()

        self.system.logger.info("Done.")

    # -------------------- File functions -------------------- #

    def _crawl(self, directory: str) -> None:
        for file in os.listdir(directory):
            file = os.path.join(directory, file)
            if self._is_python_lib(file):
                self.found_plugins.append(file)
            elif os.path.isdir(file):
                self._crawl(file)

    def _is_python_lib(self, file: str) -> bool:
        if os.path.isdir(file):
            if "__init__.py" in os.listdir(file):
                return True
        elif os.path.splitext(file)[1] in self.PYTHON_EXTENSIONS:
            return True

        return False

    # -------------------- Load -------------------- #

    def _load(self, file: str) -> BasePlugin:
        self.system.logger.debug("Attempting to import plugin...")

        if os.path.isdir(file):
            plugin = SourceFileLoader(os.path.split(file)[-1], os.path.join(file, "__init__.py")).load_module()
        else:
            plugin = SourceFileLoader(os.path.splitext(os.path.split(file)[-1])[0], file).load_module()

        self.system.logger.debug("Plugin successfully imported.")
        self.system.logger.debug("Checking for dict 'plugin'...")

        if not hasattr(plugin, "plugin"):
            raise AttributeError("Plugin has no plugin data present.")

        if not isinstance(plugin.plugin, dict):
            raise TypeError("Plugin data must be of type 'dict'.")

        self.system.logger.debug("Done, initializing main plugin class...")

        plugin_main_class = plugin.plugin["class"]

        self.system.logger.debug("Done.")

        return plugin_main_class(self.system)

    def load_plugins(self) -> None:
        for file in self.found_plugins:
            try:
                self.system.register_plugin(self._load(file))
            except Exception as error:
                self.system.logger.error("An error occurred while attempting to load plugin from file '%s': %r" % (file, error))
                raise error

