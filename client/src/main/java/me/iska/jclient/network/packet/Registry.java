package me.iska.jclient.network.packet;

import me.iska.jclient.network.packet.packets.ClientCapabilitiesPacket;
import me.iska.jclient.network.packet.packets.ClientCapabilitiesResponsePacket;
import me.iska.jclient.network.packet.packets.ConnectionInfoRequestPacket;
import me.iska.jclient.network.packet.packets.ConnectionInfoResponsePacket;
import me.iska.jclient.network.packet.packets.DisconnectPacket;
import me.iska.jclient.network.packet.packets.EncryptionResponsePacket;
import me.iska.jclient.network.packet.packets.InputPacket;
import me.iska.jclient.network.packet.packets.InputResponsePacket;
import me.iska.jclient.network.packet.packets.KeepAlivePacket;
import me.iska.jclient.network.packet.packets.KeepAliveResponsePacket;
import me.iska.jclient.network.packet.packets.LoginRequestPacket;
import me.iska.jclient.network.packet.packets.LoginResponsePacket;
import me.iska.jclient.network.packet.packets.PrintPacket;
import me.iska.jclient.network.packet.packets.ServerInfoPacket;
import me.iska.jclient.network.packet.packets.EncryptionRequestPacket;
import me.iska.jclient.network.packet.types.basic.BooleanType;
import me.iska.jclient.network.packet.types.basic.BytesType;
import me.iska.jclient.network.packet.types.basic.DoubleType;
import me.iska.jclient.network.packet.types.basic.FloatType;
import me.iska.jclient.network.packet.types.basic.IntType;
import me.iska.jclient.network.packet.types.basic.LongType;
import me.iska.jclient.network.packet.types.basic.ShortType;
import me.iska.jclient.network.packet.types.basic.StringType;
import me.iska.jclient.network.packet.types.basic.VarIntType;
import me.iska.jclient.network.packet.types.extended.GroupSpecType;
import me.iska.jclient.network.packet.types.extended.PacketSpecType;
import me.iska.jclient.network.packet.types.extended.UserSpecType;

import java.util.ArrayList;
import java.util.List;

public class Registry {

    public static final List<Class<? extends Packet>> knownPackets = new ArrayList<>();

    public static final BooleanType BOOLEAN = new BooleanType();
    public static final BytesType BYTES = new BytesType();
    public static final DoubleType DOUBLE = new DoubleType();
    public static final FloatType FLOAT = new FloatType();
    public static final IntType INT = new IntType();
    public static final IntType.Unsigned UNSIGNED_INT = new IntType.Unsigned();
    public static final LongType LONG = new LongType();
    public static final ShortType SHORT = new ShortType();
    public static final ShortType.Unsigned UNSIGNED_SHORT = new ShortType.Unsigned();
    public static final StringType STRING = new StringType();
    public static final VarIntType VARINT = new VarIntType();

    public static final PacketSpecType PACKET_SPEC = new PacketSpecType();
    public static final UserSpecType USER = new UserSpecType();
    public static final GroupSpecType GROUP = new GroupSpecType();

    static {
        knownPackets.add(ServerInfoPacket.class);
        knownPackets.add(EncryptionRequestPacket.class);
        knownPackets.add(EncryptionResponsePacket.class);
        knownPackets.add(ClientCapabilitiesPacket.class);
        knownPackets.add(ClientCapabilitiesResponsePacket.class);
        knownPackets.add(LoginRequestPacket.class);
        knownPackets.add(LoginResponsePacket.class);
        knownPackets.add(ConnectionInfoRequestPacket.class);
        knownPackets.add(ConnectionInfoResponsePacket.class);
        knownPackets.add(PrintPacket.class);
        knownPackets.add(InputPacket.class);
        knownPackets.add(InputResponsePacket.class);
        knownPackets.add(KeepAlivePacket.class);
        knownPackets.add(KeepAliveResponsePacket.class);
        knownPackets.add(DisconnectPacket.class);
    }
}
