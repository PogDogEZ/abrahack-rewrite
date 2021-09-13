#!/usr/bin/env python3

"""
Networking types for network project. These are serialized data types that can be sent over a socket connection.
Excuse the bad serialization for some of them.

Updated use for latest proxy project:
 - Yeah ok so I wrote this when I didn't know about the Enum type in python, so excuse that.
 - I also don't know what the comments mean so excuse those too.
"""

from typing import Any, IO


class Type:

    @classmethod
    def read(cls, fileobj: IO) -> None:
        raise NotImplementedError("This function is not implemented in the base Type class.")

    @classmethod
    def write(cls, object: Any, fileobj: IO) -> None:
        raise NotImplementedError("This function is not implemented in the base Type class.")


class MetaEnum(type):  # This looks interesting like it mirrors the class below lol

    def __new__(cls, name: str, bases, body):
        for name in body:
            if isinstance(body[name], int) and not name.isupper():
                raise AssertionError("Derived enums from 'Enum' must have uppercase fields.")

        return super().__new__(cls, name, bases, body)

    def __repr__(self) -> str:
        return "MetaEnum()"


class Enum(Type, metaclass=MetaEnum):

    @classmethod
    def name_from_value(cls, value):
        for name, name_value in cls.__dict__.items():
            if name.isupper() and name_value == value:
                return name

    @classmethod
    def read(cls, fileobj: IO) -> int:  # Oops guess we can't import byte type
        return fileobj.read(1)[0]

    @classmethod
    def write(cls, enum: int, fileobj: IO) -> None:
        fileobj.write(bytes([enum]))
