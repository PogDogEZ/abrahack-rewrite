#!/usr/bin/env python3

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives._serialization import Encoding, PublicFormat
from cryptography.hazmat.primitives.asymmetric import dh
from cryptography.hazmat.primitives.serialization import load_der_public_key

import pclient.networking.packets

from pclient.networking.encryption import get_cipher_from_secrets, EncryptedSocketWrapper
from pclient.networking.handlers import Handler
from pclient.networking.handlers.default import DefaultHandler
from pclient.networking.packets import StartEncryptionPacket, EncryptionResponsePacket, \
    ClientCapabilitiesPacket, ClientCapabilitiesResponsePacket, LoginPacket, LoginResponsePacket, DisconnectPacket, \
    ServerInfoPacket
from pclient.networking.packets.packet import Packet, Side
from pclient.networking.types.enum import EncryptionType


class HandShakeHandler(Handler):

    @property
    def state(self):  # -> HandShakeHandler.State
        return self._state

    def __init__(self, connection) -> None:
        super().__init__(connection)

        self._state = HandShakeHandler.State.HAND_SHAKE

        self._a_private_key = None

    def __repr__(self) -> str:
        return "HandShakeHandler(connection=%r)" % self.connection

    def on_packet(self, packet: Packet) -> None:
        if packet.SIDE != self.side and packet.SIDE != Side.BOTH:
            raise Exception("Invalid packet received: target sides do not match.")

        if isinstance(packet, DisconnectPacket):
            self.connection.exit(packet.message)
            return

        if self._state == HandShakeHandler.State.HAND_SHAKE:
            if isinstance(packet, ServerInfoPacket):
                if packet.protocol_ver != self.connection.protocol_version:
                    raise Exception("Protocol mismatch, (server: %i, client: %i)" % (packet.protocol_ver,
                                                                                     self.connection.protocol_version))

                self.connection.logger.info("Connected to server '%s', host: %s:%i (protocol version %i)" %
                                            (packet.server_name, self.connection.host, self.connection.port,
                                             packet.protocol_ver))
                self.connection.logger.info("Server compression: %s, threshold: %i bytes" %
                                            (packet.compression(), packet.compression_threshold))

                self.connection.logger.info("Server has encryption enabled: %s" % packet.encryption())

                self.connection.server_name = packet.server_name

                self.connection.set_encryption(packet.encryption(), packet.cipher)
                self.connection.set_compression(packet.compression(), packet.compression_threshold)

                if packet.encryption():
                    self._start_encryption()
                else:
                    self._start_login()

            else:
                self.connection.exit("Invalid packet in stage HAND_SHAKE: %r." % packet)

        elif self._state == HandShakeHandler.State.ENCRYPTION:
            if isinstance(packet, EncryptionResponsePacket):
                self.connection.logger.debug("Have b_peer_public_key")
                b_peer_public_key = packet.b_peer_public_key

            # client.send_packet(start_encryption_packet)

                self.connection.logger.debug("Generating shared key...")
                final_key = self._a_private_key.exchange(load_der_public_key(b_peer_public_key, default_backend()))

                self.connection.logger.info("Shared secrets established between client and server.")
                self.connection.logger.info("Starting secured connection...")

                self.connection.logger.debug("Creating cipher...")
                cipher = get_cipher_from_secrets(self.connection.get_encryption_type(), final_key, default_backend(),
                                                 packet.iv)
                self.connection._sock = EncryptedSocketWrapper(self.connection._sock, cipher.encryptor(), cipher.decryptor())

                self.connection.logger.info("Secure connection has been established with the server.")

                self._start_login()

            else:
                self.connection.exit("Invalid packet in stage ENCRYPTION: %r." % packet)

        elif self._state == HandShakeHandler.State.LOGIN:
            if isinstance(packet, ClientCapabilitiesResponsePacket):
                if packet.rejected:
                    self.connection.exit("Client capabilities rejected.")
                else:
                    self.connection.logger.debug("Client capabilities accepted, authenticating...")

                    login_packet = LoginPacket()
                    login_packet.user = self.connection.get_user()
                    login_packet.password = self.connection.get_password()

                    self.connection.send_packet(login_packet)

                    self.connection.logger.debug("Waiting for server response...")

            elif isinstance(packet, LoginResponsePacket):
                if not packet.successful_authentication:
                    self.connection.exit(packet.message)
                else:
                    self.connection.logger.info("Successfully authenticated with %r: %r." %
                                                (self.connection.server_name, packet.message))
                    self.connection._handler = DefaultHandler(self.connection)

            else:
                self.connection.exit("Invalid packet in stage LOGIN: %r." % packet)

    def _start_encryption(self) -> None:
        self._state = HandShakeHandler.State.ENCRYPTION

        self.connection.logger.info("Cipher: %s" %
                                    EncryptionType.name_from_value(self.connection.get_encryption_type()).lower())
        self.connection.logger.info("Generating shared secret between client and server...")

        self.connection.logger.debug("Generating params...")
        params = dh.generate_parameters(generator=2, key_size=512, backend=default_backend())
        self.connection.logger.debug("Generating a_private_key...")
        self._a_private_key = params.generate_private_key()
        self.connection.logger.debug("Generating a_peer_public_key...")
        a_peer_public_key = self._a_private_key.public_key()

        start_encryption_packet = StartEncryptionPacket()
        start_encryption_packet.a_peer_public_key = a_peer_public_key.public_bytes(Encoding.DER,
                                                                                   PublicFormat.SubjectPublicKeyInfo)
        start_encryption_packet.param_g = params.parameter_numbers().g
        start_encryption_packet.param_p = params.parameter_numbers().p
        self.connection.send_packet(start_encryption_packet)

        self.connection.logger.debug("Waiting for server response...")

    def _start_login(self) -> None:
        self._state = HandShakeHandler.State.LOGIN

        self.connection.logger.debug("Sending client capabilities...")

        client_capabilities = ClientCapabilitiesPacket()
        client_capabilities.set_packets(list(map(lambda packet: (packet.ID, packet.NAME, packet.SIDE),
                                                 pclient.networking.packets.packets)))

        self.connection.send_packet(client_capabilities)

    class State:
        HAND_SHAKE = 0
        ENCRYPTION = 1
        LOGIN = 2
