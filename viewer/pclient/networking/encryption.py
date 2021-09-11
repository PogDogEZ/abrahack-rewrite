#!/usr/bin/env python3

import socket

from cryptography.hazmat.backends.openssl.backend import Backend
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

from pclient.networking.types.enum import EncryptionType


def get_cipher_from_secrets(encryption_enum: EncryptionType, shared_key: bytes, backend: Backend, iv: bytes) -> Cipher:
    # I guess this could happen out of stupidity from future me <--- UPDATE: it did damn
    if encryption_enum == EncryptionType.NONE:
        raise ValueError("Encryption type enum cannot be NONE.")

    elif encryption_enum == EncryptionType.AES256:
        if len(shared_key) > 32:
            shared_key = shared_key[:32]

        if len(iv) > algorithms.AES.block_size // 8:
            iv = iv[:algorithms.AES.block_size // 8]

        return Cipher(algorithms.AES(shared_key), modes.CFB8(iv), backend)  # CFB mode

    elif encryption_enum == EncryptionType.BLOWFISH448:
        if len(shared_key) > 56:
            shared_key = shared_key[:56]

        if len(iv) > algorithms.Blowfish.block_size // 8:
            iv = iv[:algorithms.Blowfish.block_size // 8]

        return Cipher(algorithms.Blowfish(shared_key), modes.CFB(iv), backend)

    elif encryption_enum == EncryptionType.CAST5:
        if len(shared_key) > 16:
            shared_key = shared_key[:16]

        if len(iv) > algorithms.CAST5.block_size // 8:
            iv = iv[:algorithms.CAST5.block_size // 8]

        return Cipher(algorithms.CAST5(shared_key), modes.CFB(iv), backend)

    raise ValueError("Invalid encryption type enum.")


class EncryptedSocketWrapper:

    def __init__(self, conn: socket.socket, encryptor, decryptor) -> None:
        # assert isinstance(conn, socket.socket), "Param 'conn' must be of type socket.socket"

        self.conn = conn
        self.encryptor = encryptor
        self.decryptor = decryptor

    # Wrap useful functions from socket.socket

    def settimeout(self, timeout: float) -> None:
        self.conn.settimeout(timeout)

    def send(self, data: bytes) -> int:
        return self.conn.send(self.encryptor.update(data))

    def sendall(self, data: bytes) -> None:
        self.conn.sendall(self.encryptor.update(data))

    def recv(self, length: int) -> int:
        return self.decryptor.update(self.conn.recv(length))

    def fileno(self) -> int:
        return self.conn.fileno()

    def close(self) -> None:
        self.conn.close()

    def shutdown(self, how: int) -> None:
        self.conn.shutdown(how)
