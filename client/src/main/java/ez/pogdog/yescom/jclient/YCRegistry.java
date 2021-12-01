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
    public static final ChatMessageType CHAT_MESSAGE = new ChatMessageType();
    public static final PlayerType PLAYER = new PlayerType();
    public static final ConfigRuleType CONFIG_RULE = new ConfigRuleType();
    public static final ParamDescriptionType PARAM_DESCRIPTION = new ParamDescriptionType();
    public static final ParameterType PARAMETER = new ParameterType();
    public static final ChunkStateType CHUNK_STATE = new ChunkStateType();
    public static final RenderDistanceType RENDER_DISTANCE = new RenderDistanceType();
    public static final TrackedPlayerType TRACKED_PLAYER = new TrackedPlayerType();
    public static final TrackingDataType TRACKING_DATA = new TrackingDataType();
    public static final TrackerType TRACKER = new TrackerType();

    public static void registerPackets() {
        Registry.KNOWN_PACKETS.add(YCInitRequestPacket.class);
        Registry.KNOWN_PACKETS.add(YCInitResponsePacket.class);
        Registry.KNOWN_PACKETS.add(YCExtendedResponsePacket.class);
        Registry.KNOWN_PACKETS.add(DataExchangePacket.class);
        Registry.KNOWN_PACKETS.add(ConfigActionPacket.class);
        Registry.KNOWN_PACKETS.add(TaskActionPacket.class);
        Registry.KNOWN_PACKETS.add(AccountActionPacket.class);
        Registry.KNOWN_PACKETS.add(PlayerActionPacket.class);
        Registry.KNOWN_PACKETS.add(TrackerActionPacket.class);
        Registry.KNOWN_PACKETS.add(InfoUpdatePacket.class);
        Registry.KNOWN_PACKETS.add(ActionRequestPacket.class);
        Registry.KNOWN_PACKETS.add(ActionResponsePacket.class);
        Registry.KNOWN_PACKETS.add(ConfigSyncPacket.class);
        Registry.KNOWN_PACKETS.add(TaskSyncPacket.class);

        Registry.KNOWN_TYPES.put(Dimension.class, DimensionType.class);
        Registry.KNOWN_TYPES.put(ChunkPosition.class, ChunkPositionType.class);
        Registry.KNOWN_TYPES.put(Position.class, PositionType.class);
        Registry.KNOWN_TYPES.put(Angle.class, AngleType.class);
        Registry.KNOWN_TYPES.put(IQuery.Priority.class, PriorityType.class);
        Registry.KNOWN_TYPES.put(Player.class, PlayerType.class);
        Registry.KNOWN_TYPES.put(TaskRegistry.ParamDescription.class, ParamDescriptionType.class);
    }
}
