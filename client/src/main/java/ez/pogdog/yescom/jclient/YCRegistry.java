package ez.pogdog.yescom.jclient;

import ez.pogdog.yescom.jclient.packets.AccountActionPacket;
import ez.pogdog.yescom.jclient.packets.AccountActionResponsePacket;
import ez.pogdog.yescom.jclient.packets.LoadedChunkPacket;
import ez.pogdog.yescom.jclient.packets.PlayerActionPacket;
import ez.pogdog.yescom.jclient.packets.YCInitRequestPacket;
import ez.pogdog.yescom.jclient.packets.YCInitResponsePacket;
import ez.pogdog.yescom.jclient.types.AngleType;
import ez.pogdog.yescom.jclient.types.ChunkPositionType;
import ez.pogdog.yescom.jclient.types.PlayerType;
import ez.pogdog.yescom.jclient.types.PositionType;
import me.iska.jclient.network.packet.Registry;


public class YCRegistry {

    public static final int ID_OFFSET = 255;

    public static final ChunkPositionType CHUNK_POSITION = new ChunkPositionType();
    public static final PositionType POSITION = new PositionType();
    public static final AngleType ANGLE = new AngleType();
    public static final PlayerType PLAYER = new PlayerType();

    public static void registerPackets() {
        Registry.knownPackets.add(YCInitRequestPacket.class);
        Registry.knownPackets.add(YCInitResponsePacket.class);
        Registry.knownPackets.add(AccountActionPacket.class);
        Registry.knownPackets.add(AccountActionResponsePacket.class);
        Registry.knownPackets.add(PlayerActionPacket.class);
        Registry.knownPackets.add(LoadedChunkPacket.class);
    }
}
