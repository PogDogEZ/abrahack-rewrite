package ez.pogdog.yescom.jclient;

import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.jclient.packets.*;
import ez.pogdog.yescom.jclient.types.*;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.task.TaskRegistry;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;
import ez.pogdog.yescom.util.Position;
import me.iska.jclient.network.packet.Registry;


public class YCRegistry {

    public static final int ID_OFFSET = 255;

    public static final ChunkPositionType CHUNK_POSITION = new ChunkPositionType();
    public static final PositionType POSITION = new PositionType();
    public static final AngleType ANGLE = new AngleType();
    public static final DimensionType DIMENSION = new DimensionType();
    public static final PriorityType PRIORITY = new PriorityType();
    public static final PlayerType PLAYER = new PlayerType();
    public static final ParamDescriptionType PARAM_DESCRIPTION = new ParamDescriptionType();
    public static final ParameterType PARAMETER = new ParameterType();
    public static final ChunkStateType CHUNK_STATE = new ChunkStateType();
    public static final RenderDistanceType RENDER_DISTANCE = new RenderDistanceType();
    public static final TrackedPlayerType TRACKED_PLAYER = new TrackedPlayerType();
    public static final TrackerType TRACKER = new TrackerType();

    public static void registerPackets() {
        Registry.knownPackets.add(YCInitRequestPacket.class);
        Registry.knownPackets.add(YCInitResponsePacket.class);
        Registry.knownPackets.add(YCExtendedResponsePacket.class);
        Registry.knownPackets.add(UpdateDataIDsPacket.class);
        Registry.knownPackets.add(DataRequestPacket.class);
        Registry.knownPackets.add(DataResponsePacket.class);
        Registry.knownPackets.add(DataPartPacket.class);
        Registry.knownPackets.add(ConfigActionPacket.class);
        Registry.knownPackets.add(TaskSyncPacket.class);
        Registry.knownPackets.add(TaskActionPacket.class);
        Registry.knownPackets.add(AccountActionPacket.class);
        Registry.knownPackets.add(AccountActionResponsePacket.class);
        Registry.knownPackets.add(PlayerActionPacket.class);
        Registry.knownPackets.add(ChunkStatesPacket.class);
        Registry.knownPackets.add(TrackerActionPacket.class);
        Registry.knownPackets.add(InfoUpdatePacket.class);
        Registry.knownPackets.add(OnlinePlayersActionPacket.class);

        Registry.knownTypes.put(Dimension.class, DimensionType.class);
        Registry.knownTypes.put(ChunkPosition.class, ChunkPositionType.class);
        Registry.knownTypes.put(Position.class, PositionType.class);
        Registry.knownTypes.put(Angle.class, AngleType.class);
        Registry.knownTypes.put(IQuery.Priority.class, PriorityType.class);
        Registry.knownTypes.put(Player.class, PlayerType.class);
        Registry.knownTypes.put(TaskRegistry.ParamDescription.class, ParamDescriptionType.class);
    }
}
