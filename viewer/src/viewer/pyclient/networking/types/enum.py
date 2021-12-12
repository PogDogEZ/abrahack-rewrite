#!/usr/bin/env python3

from . import Enum


class EncryptionType(Enum):  # FIXME: Bad place to have this
    """
    The encryption type that the server supports.
    """

    NONE = 0
    AES256 = 1
    BLOWFISH448 = 2
    CAST5 = 3
