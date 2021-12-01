package me.iska.jclient.network.packet;

import me.iska.jclient.network.packet.packets.*;
import me.iska.jclient.network.packet.types.basic.*;
import me.iska.jclient.network.packet.types.extended.GroupType;
import me.iska.jclient.network.packet.types.extended.PermissionType;
import me.iska.jclient.network.packet.types.extended.UserType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains registered packets (and type but this isn't really used).
 */
public class Registry {

    public static final List<Class<? extends Packet>> KNOWN_PACKETS = new ArrayList<>();
    public static final Map<Class<?>, Class<? extends Type<?>>> KNOWN_TYPES = new HashMap<>();

    public static final BooleanType BOOLEAN = new BooleanType();
    public static final BytesType BYTES = new BytesType();
    public static final DoubleType DOUBLE = new DoubleType();
    public static final FloatType FLOAT = new FloatType();
    public static final IntType INTEGER = new IntType();
    public static final IntType.Unsigned UNSIGNED_INTEGER = new IntType.Unsigned();
    public static final LongType LONG = new LongType();
    public static final ShortType SHORT = new ShortType();
    public static final ShortType.Unsigned UNSIGNED_SHORT = new ShortType.Unsigned();
    public static final StringType STRING = new StringType();
    public static final VarIntType VAR_INTEGER = new VarIntType();

    public static final PermissionType PERMISSION = new PermissionType();
    public static final UserType USER = new UserType();
    public static final GroupType GROUP = new GroupType();

    static {
        KNOWN_PACKETS.add(ServerInfoPacket.class);
        KNOWN_PACKETS.add(EncryptionRequestPacket.class);
        KNOWN_PACKETS.add(EncryptionResponsePacket.class);
        KNOWN_PACKETS.add(ClientCapabilitiesPacket.class);
        KNOWN_PACKETS.add(ClientCapabilitiesResponsePacket.class);
        KNOWN_PACKETS.add(LoginRequestPacket.class);
        KNOWN_PACKETS.add(LoginResponsePacket.class);
        KNOWN_PACKETS.add(ConnInfoRequestPacket.class);
        KNOWN_PACKETS.add(ConnInfoResponsePacket.class);
        KNOWN_PACKETS.add(PrintPacket.class);
        KNOWN_PACKETS.add(InputPacket.class);
        KNOWN_PACKETS.add(InputResponsePacket.class);
        KNOWN_PACKETS.add(KeepAlivePacket.class);
        KNOWN_PACKETS.add(KeepAliveResponsePacket.class);
        KNOWN_PACKETS.add(DisconnectPacket.class);

        KNOWN_TYPES.put(Boolean.class, BooleanType.class);
        KNOWN_TYPES.put(Double.class, DoubleType.class);
        KNOWN_TYPES.put(Float.class, FloatType.class);
        KNOWN_TYPES.put(Integer.class, IntType.class);
        KNOWN_TYPES.put(Long.class, LongType.class);
        KNOWN_TYPES.put(Short.class, ShortType.class);
        KNOWN_TYPES.put(String.class, StringType.class);
    }
}
