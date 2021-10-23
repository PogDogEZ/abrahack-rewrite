#!/usr/bin/env python3

import sys
import datetime

from enum import Enum
from typing import Any


class LogLevel(Enum):
    DEBUG = 0
    INFO = 1
    WARN = 2
    ERROR = 3
    FATAL = 4


class Logger:  # TODO: PyDoc

    @staticmethod
    def get_time() -> str:
        now = datetime.datetime.now()
        # return "[%i-%i-%i %.2i:%.2i:%.2i.%.2s]" % (now.year, now.month, now.day, now.hour, now.minute, now.second,
        #                                            str(now.microsecond))
        return "[%.2i:%.2i:%.2i.%.2s]" % (now.hour, now.minute, now.second, str(now.microsecond))

    @property
    def name(self) -> str:
        return self._name

    def __init__(self, name: str, log_file=None,
                 log_level: LogLevel = LogLevel.INFO,
                 show_name: bool = True,
                 show_time: bool = True,
                 colour: bool = True) -> None:
        self._name = name

        self.log_file = log_file

        self.log_level = log_level

        self.show_name = show_name
        self.show_time = show_time
        self.colour = colour

    def print(self, *message: Any, channel: int = 0, sep: str = " ", end: str = "\n") -> None:
        message = list(message)
        message.insert(0, "[CHANNEL-%i]" % channel)

        if self.show_name:
            message.insert(0, "[%s]" % self._name)
        if self.show_time:
            message.insert(0, self.get_time())

        message.append(end)
        sys.stdout.write(sep.join(message))

    def log(self, *message: Any, sep: str = " ", end: str = "\n", level: LogLevel = LogLevel.INFO) -> None:
        if level == LogLevel.DEBUG:
            self.debug(*message, sep=sep, end=end)
        elif level == LogLevel.INFO:
            self.info(*message, sep=sep, end=end)
        elif level == LogLevel.WARN:
            self.warn(*message, sep=sep, end=end)
        elif level == LogLevel.ERROR:
            self.error(*message, sep=sep, end=end)
        elif level == LogLevel.FATAL:
            self.fatal(*message, sep=sep, end=end)
        else:
            self.print(*message, channel=level.value, sep=sep, end=end)

    def debug(self, *message: Any, sep: str = " ", end: str = "\n") -> None:
        if self.log_level.value > LogLevel.DEBUG.value:
            return

        message = list(message)
        message.insert(0, "[DEBUG]")

        if self.show_name:
            message.insert(0, "[%s]" % self._name)
        if self.show_time:
            message.insert(0, self.get_time())

        message.append(end)  # "\u001b[101m"
        sys.stdout.write(
            ("\u001b[34m" if self.colour else "") +
            sep.join(message) +
            ("\u001b[0m" if self.colour else "")
        )

    def info(self, *message: Any, sep: str = " ", end: str = "\n") -> None:
        if self.log_level.value > LogLevel.INFO.value:
            return

        message = list(message)
        message.insert(0, "[INFO]")

        if self.show_name:
            message.insert(0, "[%s]" % self._name)
        if self.show_time:
            message.insert(0, self.get_time())

        message.append(end)
        sys.stdout.write(sep.join(message))

    def warn(self, *message: Any, sep: str = " ", end: str = "\n") -> None:
        if self.log_level.value > LogLevel.WARN.value:
            return

        message = list(message)
        message.insert(0, "[WARN]")

        if self.show_name:
            message.insert(0, "[%s]" % self._name)
        if self.show_time:
            message.insert(0, self.get_time())

        message.append(end)
        sys.stdout.write(
            ("\u001b[91m" if self.colour else "") +
            sep.join(message) +
            ("\u001b[0m" if self.colour else "")
        )

    def error(self, *message: Any, sep: str = " ", end: str = "\n") -> None:
        if self.log_level.value > LogLevel.ERROR.value:
            return

        message = list(message)
        message.insert(0, "[ERROR]")

        if self.show_name:
            message.insert(0, "[%s]" % self._name)
        if self.show_time:
            message.insert(0, self.get_time())

        message.append(end)
        sys.stdout.write(
            ("\u001b[31m" if self.colour else "") +
            sep.join(message) +
            ("\u001b[0m" if self.colour else "")
        )

    def fatal(self, *message: Any, sep: str = " ", end: str = "\n") -> None:
        if self.log_level.value > LogLevel.FATAL.value:
            return

        message = list(message)
        message.insert(0, "[FATAL]")

        if self.show_name:
            message.insert(0, "[%s]" % self._name)
        if self.show_time:
            message.insert(0, self.get_time())

        message.append(end)
        sys.stdout.write(
            ("\u001b[41m\u001b[30m" if self.colour else "") +
            sep.join(message) +
            ("\u001b[0m" if self.colour else "")
        )
