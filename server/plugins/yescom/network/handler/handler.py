#!/usr/bin/env python3

import os

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.asymmetric.padding import OAEP, MGF1
from cryptography.hazmat.primitives.hashes import Hash, SHA256, SHA1
from cryptography.hazmat.primitives.serialization import load_der_public_key

from network.networking.handlers import Handler
from network.networking.packets import Packet
from plugins.yescom.network.packets.shared import YCInitRequestPacket, YCInitResponsePacket, YCExtendedResponsePacket
from .listening import Listener
from .reporting import Reporter


class YCHandler(Handler):

    def __init__(self, system, connection, yescom, handler_id: int) -> None:
        super().__init__(system, connection)

        self.yescom = yescom

        self._handler_id = handler_id
        self._handler_name = "unknown"

        self._new_handler = None

        self._identity_proof_nonce = b""
        self._identity_proof_signature = b""

        self._initialized = False

    def __repr__(self) -> str:
        return "YCHandler(name=%s, id=%i, connection=%s)" % (self._handler_name, self._handler_id, self.connection)

    def on_packet(self, packet: Packet) -> None:
        if isinstance(packet, YCInitRequestPacket):
            if self._initialized:
                raise Exception("Didn't expect YCInitRequestPacket as already initialized.")
            self._initialized = True

            self.yescom.logger.debug("Got yc_init_request from %r." % self.connection)

            self._handler_name = packet.handler_name

            yc_init_response = YCInitResponsePacket()
            yc_init_response.handler_id = self._handler_id

            self._new_handler = None

            if packet.client_type == YCInitRequestPacket.ClientType.LISTENING:
                self.yescom.logger.debug("%r is listening." % self)
                self._new_handler = Listener(self.system, self.connection, self.yescom, self._handler_id, self._handler_name)
                self.yescom.add_listener(self._new_handler)
                self._new_handler.do_full_sync()

            else:
                if not self.yescom.is_trusted(packet.handler_hash):
                    yc_init_response.rejected = True
                    yc_init_response.message = "Untrusted handler."

                elif not self.yescom.is_public_key_valid(packet.handler_hash, packet.handler_public_key):
                    yc_init_response.rejected = True
                    yc_init_response.message = "Invalid public key."

                else:
                    public_key = load_der_public_key(packet.handler_public_key)
                    self._identity_proof_nonce = os.urandom(32)
                    # Java MGF1 is SHA1 by default but fuck it I'm too lazy to change the OAEP parameters for the Java cipher
                    self._identity_proof_signature = public_key.encrypt(self._identity_proof_nonce, OAEP(MGF1(SHA1()),
                                                                                                         SHA256(),
                                                                                                         None))
                    self.yescom.update_public_key(packet.handler_hash, packet.handler_public_key)

                    if packet.client_type == YCInitRequestPacket.ClientType.REPORTING:
                        if packet.host_name != self.yescom.host_name:
                            self.yescom.logger.warn("%r attempted to connect with different host name (%s, %s)" %
                                                    (self.connection, packet.host_name, self.yescom.host_name))
                            yc_init_response.rejected = True
                            yc_init_response.message = "Invalid host name."

                        elif packet.host_port != self.yescom.host_port:
                            self.yescom.logger.warn("%r attempted to connect with different host port (%i, %i)" %
                                                    (self.connection, packet.host_port, self.yescom.host_port))
                            yc_init_response.rejected = True
                            yc_init_response.message = "Invalid host port."

                        else:
                            self.yescom.logger.debug("%r is reporting, waiting for extended init." % self)

                            yc_init_response.extended_init = True
                            yc_init_response.identity_proof_signature = self._identity_proof_signature

                            self._new_handler = Reporter(self.system, self.connection, self.yescom, self._handler_id,
                                                         self._handler_name)

                    elif packet.client_type == YCInitRequestPacket.ClientType.ARCHIVING:
                        self.yescom.logger.debug("%s is archiving, waiting for extended init." % self)

                        yc_init_response.extended_init = True
                        yc_init_response.identity_proof_signature = self._identity_proof_signature

            self.connection.send_packet(yc_init_response)

            if not yc_init_response.extended_init and not yc_init_response.rejected:
                self.connection.unregister_handler(self)
                self.connection.register_handler(self._new_handler)
            elif yc_init_response.rejected:
                self.connection.exit("Rejected: " + yc_init_response.message)

        elif isinstance(packet, YCExtendedResponsePacket):
            self.yescom.logger.debug("Got extended init response, verifying identity...")

            yc_init_response = YCInitResponsePacket()
            yc_init_response.extended_init = False
            yc_init_response.rejected = packet.identity_proof_nonce != self._identity_proof_nonce

            if yc_init_response.rejected:
                self.yescom.logger.debug("Failed identity verification.")
                yc_init_response.message = "Failed identity proof."
            else:
                self.yescom.logger.debug("Identity successfully verified.")
                self.connection.unregister_handler(self)
                self.connection.register_handler(self._new_handler)

                if isinstance(self._new_handler, Reporter):
                    self.yescom.add_reporter(self._new_handler)
                else:
                    self.yescom.add_archiver(self._new_handler)

            self.connection.send_packet(yc_init_response)
            if yc_init_response.rejected:
                self.connection.exit("Rejected: " + yc_init_response.message)
