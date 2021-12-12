#!/usr/bin/env python3

import logging

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives._serialization import Encoding, PublicFormat
from cryptography.hazmat.primitives.asymmetric import dh
from cryptography.hazmat.primitives.serialization import load_der_public_key

from . import Handler
from .default import DefaultHandler
from .. import packets
from ..encryption import get_cipher_from_secrets, EncryptedSocketWrapper
from ..packets import Packet, Side, EncryptionRequestPacket, EncryptionResponsePacket, ClientCapabilitiesPacket, \
    ClientCapabilitiesResponsePacket, LoginRequestPacket, LoginResponsePacket, DisconnectPacket, ServerInfoPacket
from ..types.enum import EncryptionType


class LoginHandler(Handler):
    """
    Handles server logins, duh.
    """

    @property
    def state(self):  # -> HandShakeHandler.State
        """
        :return: The state the handler is currently in.
        """

        return self._state

    def __init__(self, connection) -> None:
        super().__init__(connection)

        self._state = LoginHandler.State.HAND_SHAKE

        self._a_private_key = None

    def __repr__(self) -> str:
        return "HandShakeHandler(connection=%r)" % self.connection

    def on_packet(self, packet: Packet) -> None:
        if packet.SIDE != self.side and packet.SIDE != Side.BOTH:
            raise Exception("Invalid packet received: target sides do not match.")

        if isinstance(packet, DisconnectPacket):
            self.connection.exit(packet.message)
            return

        if self._state == LoginHandler.State.HAND_SHAKE:
            if isinstance(packet, ServerInfoPacket):
                if packet.protocol_ver != self.connection.protocol_version:
                    raise Exception("Protocol mismatch, (server: %i, client: %i)" % (packet.protocol_ver,
                                                                                     self.connection.protocol_version))

                logging.info("Connected to server %r, host: %s:%i (protocol version %i)" %
                                            (packet.server_name, self.connection.host, self.connection.port,
                                             packet.protocol_ver))
                logging.info("Server compression: %s, threshold: %i bytes" % (packet.compression,
                                                                              packet.compression_threshold))

                logging.info("Server has encryption enabled: %s" % packet.encryption)

                self.connection.server_info.server_name = packet.server_name

                self.connection.server_info.encryption = packet.encryption
                self.connection.server_info.encryption_type = packet.encryption_type

                self.connection.server_info.compression = packet.compression
                self.connection.server_info.compression_threshold = packet.compression_threshold

                self.connection.server_info.authentication = packet.authentication

                if packet.encryption:
                    self._start_encryption()
                else:
                    self._start_login()

            else:
                self.connection.exit("Invalid packet in stage HAND_SHAKE: %r." % packet)

        elif self._state == LoginHandler.State.ENCRYPTION:
            if isinstance(packet, EncryptionResponsePacket):
                logging.debug("Have b_peer_public_key")
                b_peer_public_key = packet.b_peer_public_key

                logging.debug("Generating shared key...")
                final_key = self._a_private_key.exchange(load_der_public_key(b_peer_public_key, default_backend()))

                logging.info("Shared secrets established between client and server.")
                logging.info("Starting secured connection...")

                logging.debug("Creating cipher...")
                cipher = get_cipher_from_secrets(self.connection.server_info.encryption_type, final_key,
                                                 default_backend(), packet.init_vector)

                # noinspection PyTypeChecker
                self.connection._sock = EncryptedSocketWrapper(self.connection._sock, cipher.encryptor(),
                                                               cipher.decryptor())

                logging.info("Secure connection has been established with the server.")

                self._start_login()

            else:
                self.connection.exit("Invalid packet in stage ENCRYPTION: %r." % packet)

        elif self._state == LoginHandler.State.LOGIN:
            if isinstance(packet, ClientCapabilitiesResponsePacket):
                if packet.rejected:
                    self.connection.exit("Client capabilities rejected.")
                else:
                    logging.debug("Client capabilities accepted, authenticating...")
                    if self.connection.server_info.authentication:
                        self.connection.send_packet(LoginRequestPacket(self.connection.account[0],
                                                                       self.connection.account[1],
                                                                       self.connection.account[2]))
                    logging.debug("Waiting for server response...")

            elif isinstance(packet, LoginResponsePacket):
                if not packet.success:
                    self.connection.exit(packet.message)
                else:
                    logging.info("Successfully authenticated with %r: %r." % (self.connection.server_info.server_name,
                                                                              packet.message))
                    self.connection.handler = DefaultHandler(self.connection)
                    self.connection.user = packet.user

            else:
                self.connection.exit("Invalid packet in stage LOGIN: %r." % packet)

    def _start_encryption(self) -> None:
        self._state = LoginHandler.State.ENCRYPTION

        logging.info("Cipher: %s" % EncryptionType.name_from_value(self.connection.server_info.encryption_type).lower())
        logging.info("Generating shared secret between client and server...")

        logging.debug("Generating params...")
        params = dh.generate_parameters(generator=2, key_size=512, backend=default_backend())
        logging.debug("Generating a_private_key...")
        self._a_private_key = params.generate_private_key()
        logging.debug("Generating a_peer_public_key...")
        a_peer_public_key = self._a_private_key.public_key()

        start_encryption_packet = EncryptionRequestPacket()
        start_encryption_packet.a_peer_public_key = a_peer_public_key.public_bytes(Encoding.DER,
                                                                                   PublicFormat.SubjectPublicKeyInfo)
        start_encryption_packet.param_g = params.parameter_numbers().g
        start_encryption_packet.param_p = params.parameter_numbers().p
        self.connection.send_packet(start_encryption_packet)

        logging.debug("Waiting for server response...")

    def _start_login(self) -> None:
        self._state = LoginHandler.State.LOGIN

        logging.debug("Sending client capabilities...")
        self.connection.send_packet(ClientCapabilitiesPacket(list(map(lambda packet: (packet.ID, packet.NAME,
                                                                                      packet.SIDE), packets.packets))))

    class State:
        HAND_SHAKE = 0
        ENCRYPTION = 1
        LOGIN = 2
