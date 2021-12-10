#!/usr/bin/env python3

from . import Enum


class EncryptionType(Enum):
    NONE = 0
    AES256 = 1
    BLOWFISH448 = 2
    CAST5 = 3
