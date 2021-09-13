#!/usr/bin/env python3
import base64
import os
import time

from typing import Tuple

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives._serialization import Encoding, PublicFormat
from cryptography.hazmat.primitives.asymmetric import dh
from cryptography.hazmat.primitives.serialization import load_pem_public_key, load_der_public_key

import network.networking.packets

from network.impl.event.network import StartEncryptionEvent, ClientCapabilitiesEvent, BeginAuthenticationEvent, \
    AuthenticationFailedEvent, AuthenticationSuccessEvent
from network.impl.user import User
from network.networking.encryption import get_cipher_from_secrets, EncryptedSocketWrapper
from network.networking.handlers import Handler
from network.networking.handlers.default import DefaultHandler
from network.networking.packets import StartEncryptionPacket, Packet, Side, EncryptionResponsePacket, \
    ClientCapabilitiesPacket, ClientCapabilitiesResponsePacket, LoginPacket, LoginResponsePacket, DisconnectPacket, \
    ServerInfoPacket


class HandShakeHandler(Handler):

    def __init__(self, system, connection) -> None:
        super().__init__(system, connection)

        self.private_key = None
        self.peer_public_key = None

    def __repr__(self) -> str:
        return "HandShakeHandler(connection=%r)" % self.connection

    def _authenticate(self, user: User, password: str) -> Tuple[bool, str]:
        if not self.system.get_authentication():
            return True, self.system.login_message

        try:
            if user is not None:
                username = user.username
                if user.group is None:
                    group = self.system.default_group
                else:
                    group = user.group
            else:
                username = ""
                group = self.system.default_group

            # group.name since the client won't know about the group IDs yet
            user = self.system.get_group_by_name(group.name).get_user_by_name(username)

            self.system.log_in_user(user, password, self.connection)
        except Exception as error:
            return False, repr(error)

        return True, self.system.login_message

    def on_packet(self, packet: Packet) -> None:
        if packet.SIDE != self.side and packet.SIDE != Side.BOTH:
            raise Exception("Invalid packet received: target sides do not match.")

        if isinstance(packet, StartEncryptionPacket):
            # FIXME: Only allow 1 StartEncryptionPacket
            if not self.system.get_encrypted():
                self.connection.exit("An encrypted connection was attempted while server encryption is not enabled.")
                return

            self.system.logger.log("Starting secured connection with %r." % self.connection)

            self.system.event_bus.post(StartEncryptionEvent(self.connection))

            self.system.logger.debug("Getting params from connection.")
            params = dh.DHParameterNumbers(packet.param_p, packet.param_g).parameters(default_backend())

            self.system.logger.debug("Have a_peer_public_key")
            a_peer_public_key = packet.a_peer_public_key

            self.system.logger.debug("Generating b_private_key...")
            b_private_key = params.generate_private_key()
            self.system.logger.debug("Generating b_peer_public_key...")
            b_peer_public_key = b_private_key.public_key()

            self.system.logger.debug("Generating iv...")
            iv = os.urandom(32)

            encryption_reponse = EncryptionResponsePacket()
            encryption_reponse.b_peer_public_key = b_peer_public_key.public_bytes(Encoding.DER,
                                                                                  PublicFormat.SubjectPublicKeyInfo)
            encryption_reponse.iv = iv
            self.connection.send_packet(encryption_reponse)
            self.system.logger.debug("Generating shared key...")
            final_key = b_private_key.exchange(load_der_public_key(a_peer_public_key, default_backend()))

            self.system.logger.debug("Creating cipher...")
            cipher = get_cipher_from_secrets(self.system.get_encryption_type(), final_key, default_backend(), iv)

            self.connection.flush_packets()  # Flush the packets first
            self.connection._sock = EncryptedSocketWrapper(self.connection._sock, cipher.encryptor(),
                                                           cipher.decryptor())
            self.system.logger.debug("Packets have been flushed and EncryptedSocketWrapper ready.")

            self.system.logger.log("Secure connection established with %r." % self.connection)

        elif isinstance(packet, ClientCapabilitiesPacket):
            rejected = False

            for supported in network.networking.packets.packets:  # Check if we support all the packets they want
                if not supported in packet.get_accepted():
                    self.system.logger.debug("Unsupported: %s (%s %i)." % (supported, supported.NAME, supported.ID))
                    rejected = True

            """
            for accepted in packet.get_accepted():  # Check if they support all the packets we want
                if not accepted in network.networking.packets.packets:
                    rejected = True
            """

            if len(packet.get_unknown()):
                for unknown in packet.get_unknown():
                    self.system.logger.debug("Found unknown: %s." % (unknown,))
                rejected = True

            """
            if None in packet.get_accepted():  # This means we couldn't decode one of the packets they told us about
                self.system.logger.debug("Found None in accepted.")
                rejected = True
            """

            event = self.system.event_bus.post(ClientCapabilitiesEvent(packet.get_accepted(), rejected, self.connection))

            self.system.logger.debug("Rejected: %s" % event.get_rejected())

            capabilities_response = ClientCapabilitiesResponsePacket()
            capabilities_response.rejected = event.get_rejected()

            self.connection.send_packet(capabilities_response)

            if capabilities_response.rejected:
                self.connection.exit("Client capabilities rejected.")

        elif isinstance(packet, LoginPacket):
            self.system.event_bus.post(BeginAuthenticationEvent(self.connection))

            login_response = LoginResponsePacket()
            login_response.successful_authentication, login_response.message = self._authenticate(packet.user,
                                                                                                  packet.password)

            self.connection.send_packet(login_response)

            if not login_response.successful_authentication:
                event = self.system.event_bus.post(AuthenticationFailedEvent(login_response.message, self.connection))

                login_response.message = event.get_reason()

                if not event.get_cancelled():
                    self.connection.exit("Unsuccessful authentication: '%s'" % login_response.message)
                    return

            self.system.event_bus.post(AuthenticationSuccessEvent(self.connection))

            self.connection.handler = DefaultHandler(self.system, self.connection)  # Switch to a different handler

        elif isinstance(packet, DisconnectPacket):  # The proper way to handle disconnects
            self.connection.exit("Client disconnect, reason: '%s'" % packet.message)

            # self.system.logger.info("Client: %r disconnect, reason: %s" % (self.connection, packet.message))

    def start_handshake(self) -> None:
        server_info = ServerInfoPacket()
        server_info.server_name = self.system.server_name
        server_info.protocol_ver = self.system.protocol_version

        if self.system.get_encrypted():
            server_info.set_encryption_bit()
        server_info.cipher = self.system.get_encryption_type()

        if self.system.get_compression():
            server_info.set_compression_bit()

        if self.system.get_authentication():
            server_info.set_authentication_bit()

        server_info.compression_threshold = self.system.get_compression_threshold()

        self.system.logger.debug("Sending server info packet...")
        # The client connection will not yet know about the server compression
        self.connection.send_packet(server_info, force=True, no_compression=True)
