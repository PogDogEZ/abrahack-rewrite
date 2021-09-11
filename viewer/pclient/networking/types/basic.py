#!/usr/bin/env python3

import struct
from typing import IO

from . import Type


class Integer(Type):

    @classmethod
    def read(cls, fileobj: IO) -> int:
        return int().from_bytes(fileobj.read(4), "big", signed=True)

    @classmethod
    def write(cls, integer: int, fileobj: IO) -> None:
        fileobj.write(int(integer).to_bytes(4, "big", signed=True))


class UnsignedInteger(Type):

    @classmethod
    def read(cls, fileobj: IO) -> int:
        return int().from_bytes(fileobj.read(4), "big", signed=False)

    @classmethod
    def write(cls, integer: int, fileobj: IO) -> None:
        fileobj.write(int(integer).to_bytes(4, "big", signed=False))


class Long(Type):

    @classmethod
    def read(cls, fileobj: IO) -> int:
        return int().from_bytes(fileobj.read(8), "big", signed=True)

    @classmethod
    def write(cls, long: int, fileobj: IO) -> None:
        fileobj.write(int(long).to_bytes(8, "big", signed=True))


class UnsignedLong(Type):

    @classmethod
    def read(cls, fileobj: IO) -> int:
        return int().from_bytes(fileobj.read(8), "big", signed=False)

    @classmethod
    def write(cls, long: int, fileobj: IO) -> None:
        fileobj.write(int(long).to_bytes(8, "big", signed=False))


class Short(Type):

    @classmethod
    def read(cls, fileobj: IO) -> int:
        return int().from_bytes(fileobj.read(2), "big", signed=True)

    @classmethod
    def write(cls, short: int, fileobj: IO) -> None:
        fileobj.write(int(short).to_bytes(2, "big", signed=True))


class UnsignedShort(Type):

    @classmethod
    def read(cls, fileobj: IO) -> int:
        return int().from_bytes(fileobj.read(2), "big", signed=False)

    @classmethod
    def write(cls, short: int, fileobj: IO) -> None:
        fileobj.write(int(short).to_bytes(2, "big", signed=False))


class VarInt(Type):

    @classmethod
    def read(cls, fileobj: IO) -> int:
        read_length = fileobj.read(1)[0]
        read = fileobj.read(read_length)
        varint = 0
        for index, value in enumerate(read):
            varint += value * (256**index)
        return varint

    @classmethod
    def write(cls, varint: int, fileobj: IO) -> None:
        data_bytes = bytearray()
        varint = divmod(varint, 256)
        while True:
            data_bytes.append(varint[1])
            if varint[0] < 256: break
            varint = divmod(varint[0], 256)
        data_bytes.append(varint[0])
        fileobj.write(bytes([len(data_bytes)]) + data_bytes)


class Boolean(Type):

    @classmethod
    def read(cls, fileobj: IO) -> bool:
        return bool(fileobj.read(1)[0])

    @classmethod
    def write(cls, boolean: bool, fileobj: IO) -> None:
        fileobj.write(bytes([1 if boolean else 0]))


class Float(Type):

    @classmethod
    def read(cls, fileobj: IO) -> float:
        return struct.unpack("f", fileobj.read(4))

    @classmethod
    def write(cls, floating: float, fileobj: IO) -> None:
        fileobj.write(struct.pack("f", floating))


class Double(Type):

    @classmethod
    def read(cls, fileobj: IO) -> float:
        return struct.unpack("d", fileobj.read(8))

    @classmethod
    def write(cls, double: float, fileobj: IO) -> None:
        fileobj.write(struct.pack("d", double))


class String(Type):

    @classmethod
    def read(cls, fileobj: IO) -> str:
        string_length = UnsignedShort.read(fileobj)
        return fileobj.read(string_length).decode()

    @classmethod
    def write(cls, string: str, fileobj: IO) -> None:
        string = string.encode()
        UnsignedShort.write(len(string), fileobj)
        fileobj.write(string)


class Bytes(Type):

    @classmethod
    def read(cls, fileobj: IO) -> bytes:
        bytes_length = UnsignedShort.read(fileobj)
        return fileobj.read(bytes_length)

    @classmethod
    def write(cls, bytes_array: bytes, fileobj: IO) -> None:
        UnsignedShort.write(len(bytes_array), fileobj)
        fileobj.write(bytes_array)
