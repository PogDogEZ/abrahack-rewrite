package ez.pogdog.yescom.network;

import ez.pogdog.yescom.network.packets.listening.ReporterActionPacket;
import ez.pogdog.yescom.network.packets.listening.ReporterSyncPacket;
import ez.pogdog.yescom.network.packets.reporting.ConfigSyncPacket;
import ez.pogdog.yescom.network.packets.reporting.TaskSyncPacket;
import ez.pogdog.yescom.network.packets.shared.*;
import ez.pogdog.yescom.network.types.*;
import ez.pogdog.yescom.util.*;
import ez.pogdog.yescom.util.task.parameter.ParamDescription;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.util.ArrayList;
import java.util.List;

public class YCRegistry {

    public static final int ID_OFFSET = 255;

    public static final List<Class<? extends Packet>> LISTENER_PACKETS = new ArrayList<>();
    public static final List<Class<? extends Packet>> REPORTER_PACKETS = new ArrayList<>();
    public static final List<Class<? extends Packet>> SHARED_PACKETS = new ArrayList<>();

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
        LISTENER_PACKETS.add(ReporterActionPacket.class);
        LISTENER_PACKETS.add(ReporterSyncPacket.class);

        REPORTER_PACKETS.add(ConfigSyncPacket.class);
        REPORTER_PACKETS.add(TaskSyncPacket.class);

        SHARED_PACKETS.add(YCInitRequestPacket.class);
        SHARED_PACKETS.add(YCInitResponsePacket.class);
        SHARED_PACKETS.add(YCExtendedResponsePacket.class);
        SHARED_PACKETS.add(DataExchangePacket.class);
        SHARED_PACKETS.add(ConfigActionPacket.class);
        SHARED_PACKETS.add(TaskActionPacket.class);
        SHARED_PACKETS.add(AccountActionPacket.class);
        SHARED_PACKETS.add(PlayerActionPacket.class);
        SHARED_PACKETS.add(TrackerActionPacket.class);
        SHARED_PACKETS.add(InfoUpdatePacket.class);
        SHARED_PACKETS.add(OnlinePlayersActionPacket.class);
        SHARED_PACKETS.add(ActionRequestPacket.class);
        SHARED_PACKETS.add(ActionResponsePacket.class);

        Registry.KNOWN_PACKETS.addAll(LISTENER_PACKETS);
        Registry.KNOWN_PACKETS.addAll(REPORTER_PACKETS);
        Registry.KNOWN_PACKETS.addAll(SHARED_PACKETS);

        Registry.KNOWN_TYPES.put(Dimension.class, DimensionType.class);
        Registry.KNOWN_TYPES.put(ChunkPosition.class, ChunkPositionType.class);
        Registry.KNOWN_TYPES.put(Position.class, PositionType.class);
        Registry.KNOWN_TYPES.put(Angle.class, AngleType.class);
        Registry.KNOWN_TYPES.put(Priority.class, PriorityType.class);
        Registry.KNOWN_TYPES.put(Player.class, PlayerType.class);
        Registry.KNOWN_TYPES.put(ParamDescription.class, ParamDescriptionType.class);
    }
}
