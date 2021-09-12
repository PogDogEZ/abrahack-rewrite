package ez.pogdog.yescom.jclient;

import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.jclient.packets.AccountActionPacket;
import ez.pogdog.yescom.jclient.packets.AccountActionResponsePacket;
import ez.pogdog.yescom.jclient.packets.LoadedChunkPacket;
import ez.pogdog.yescom.jclient.packets.PlayerActionPacket;
import ez.pogdog.yescom.jclient.packets.TaskActionPacket;
import ez.pogdog.yescom.jclient.packets.TaskSyncPacket;
import ez.pogdog.yescom.jclient.packets.YCInitRequestPacket;
import ez.pogdog.yescom.jclient.packets.YCInitResponsePacket;
import ez.pogdog.yescom.jclient.types.AngleType;
import ez.pogdog.yescom.jclient.types.ChunkPositionType;
import ez.pogdog.yescom.jclient.types.ParamDescriptionType;
import ez.pogdog.yescom.jclient.types.PlayerType;
import ez.pogdog.yescom.jclient.types.PositionType;
import ez.pogdog.yescom.task.TaskRegistry;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Position;
import me.iska.jclient.network.packet.Registry;


public class YCRegistry {

    public static final int ID_OFFSET = 255;

    public static final ChunkPositionType CHUNK_POSITION = new ChunkPositionType();
    public static final PositionType POSITION = new PositionType();
    public static final AngleType ANGLE = new AngleType();
    public static final PlayerType PLAYER = new PlayerType();
    public static final ParamDescriptionType PARAM_DESCRIPTION = new ParamDescriptionType();

    public static void registerPackets() {
        Registry.knownPackets.add(YCInitRequestPacket.class);
        Registry.knownPackets.add(YCInitResponsePacket.class);
        Registry.knownPackets.add(TaskSyncPacket.class);
        Registry.knownPackets.add(TaskActionPacket.class);
        Registry.knownPackets.add(AccountActionPacket.class);
        Registry.knownPackets.add(AccountActionResponsePacket.class);
        Registry.knownPackets.add(PlayerActionPacket.class);
        Registry.knownPackets.add(LoadedChunkPacket.class);

        Registry.knownTypes.put(ChunkPosition.class, ChunkPositionType.class);
        Registry.knownTypes.put(Position.class, PositionType.class);
        Registry.knownTypes.put(Angle.class, AngleType.class);
        Registry.knownTypes.put(Player.class, PlayerType.class);
        Registry.knownTypes.put(TaskRegistry.ParamDescription.class, ParamDescriptionType.class);
    }
}
