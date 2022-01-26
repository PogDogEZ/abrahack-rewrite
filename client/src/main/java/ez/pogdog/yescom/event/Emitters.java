package ez.pogdog.yescom.event;

import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.util.Pair;

import java.util.UUID;

/**
 * Global emitters for YesCom.
 */
public class Emitters {

    /* ------------------------ Server stuff ------------------------ */

    /**
     * Emits when a player joins the server.
     */
    @SuppressWarnings("rawtypes")
    public static final Emitter<Pair> PLAYER_JOIN_EMITTER = new Emitter<>(Pair.class);

    /**
     * Called when a player leaves the server.
     */
    public static final Emitter<UUID> PLAYER_LEAVE_EMITTER = new Emitter<>(UUID.class);

    /* ------------------------ Data stuff ------------------------ */

    /**
     * Emits when a chunk state is discovered.
     */
    public static final Emitter<ChunkState> CHUNK_STATE_EMITTER = new Emitter<>(ChunkState.class);

    /**
     * Emits when a render distance is discovered.
     */
    public static final Emitter<RenderDistance> RENDER_DISTANCE_EMITTER = new Emitter<>(RenderDistance.class);

    /**
     * Emits when a tracked player is discovered.
     */
    public static final Emitter<TrackedPlayer> TRACKED_PLAYER_EMITTER = new Emitter<>(TrackedPlayer.class);
}
