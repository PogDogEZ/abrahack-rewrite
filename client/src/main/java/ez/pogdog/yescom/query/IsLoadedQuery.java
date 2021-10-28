package ez.pogdog.yescom.query;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.packetlib.packet.Packet;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.util.BlockPosition;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.function.BiConsumer;

public class IsLoadedQuery implements IQuery {

    private final YesCom yesCom = YesCom.getInstance();

    private final BlockPosition position;
    private final Dimension dimension;
    private final Priority priority;
    private final Type type;
    private final BiConsumer<IsLoadedQuery, Result> callBack;

    private final float ticksPerQuery; // Optimisation

    private Player player;
    private int listenerID;

    private long startTime;
    private boolean finished;

    private boolean rescheduled;
    private boolean cancelled;
    private Result result;

    public IsLoadedQuery(BlockPosition position, Dimension dimension, Priority priority, Type type,
                         BiConsumer<IsLoadedQuery, Result> callBack) {
        this.position = position;
        this.dimension = dimension;
        this.priority = priority;
        this.type = type;
        this.callBack = callBack;

        ticksPerQuery = 1.0f / (float)yesCom.configHandler.QUERIES_PER_TICK;

        startTime = System.currentTimeMillis();
        finished = false;

        rescheduled = false;
        cancelled = false;
        result = null;
    }

    public IsLoadedQuery(ChunkPosition position, Dimension dimension, Priority priority, Type type,
                         BiConsumer<IsLoadedQuery, Result> callBack) {
        this(position.getPosition(8, 0, 8), dimension, priority, type, callBack);
    }

    @Override
    public String toString() {
        return String.format("IsLoadedQuery(position=%s, type=%s)", position, type.name());
    }

    @Override
    public String getName() {
        return "is_loaded";
    }

    @Override
    public HandleAction handle() {
        if (!yesCom.connectionHandler.isConnected() || yesCom.connectionHandler.getTimeSinceLastPacket() >= yesCom.configHandler.MAX_PACKET_TIME)
            return HandleAction.AWAIT;

        finished = false;
        result = null;

        if (cancelled) return HandleAction.REMOVE;

        switch (type) {
            case DIGGING: {
                player = yesCom.connectionHandler.sendPacket(new ClientPlayerActionPacket(PlayerAction.CANCEL_DIGGING,
                        position.toSerializable(), BlockFace.UP));
                if (player == null) { // It didn't send
                    return HandleAction.AWAIT;
                } else {
                    listenerID = player.addPacketListener(this::onPacket);

                    startTime = System.currentTimeMillis();
                    return HandleAction.START;
                }
            }
            case INVALID_MOVE: {
                player = yesCom.invalidMoveHandler.startQuery(this);
                if (player == null) {
                    return HandleAction.AWAIT;
                } else {
                    startTime = System.currentTimeMillis();
                    return HandleAction.START;
                }
            }
        }

        return HandleAction.REMOVE;
    }

    @Override
    public TickAction tick() {
        if (!yesCom.connectionHandler.isConnected() ||
                yesCom.connectionHandler.getTimeSinceLastPacket() >= yesCom.configHandler.MAX_PACKET_TIME) {
            startTime = System.currentTimeMillis(); // Reset because we probably aren't receiving packets right now or some other reason
            return TickAction.AWAIT;
        }

        switch (type) {
            case DIGGING: {
                if (System.currentTimeMillis() - startTime > yesCom.configHandler.DIGGING_TIMEOUT && result == null)
                    result = Result.UNLOADED;
                break;
            }
            case INVALID_MOVE: {
                break;
            }
        }

        if (rescheduled || cancelled) {
            startTime = System.currentTimeMillis();

            if (!cancelled) yesCom.queryHandler.addQuery(this);

            rescheduled = false;
            cancelled = false;
            finished = false;

            return TickAction.REMOVE;

        } else if (result != null) {
            finished = true;
            onFinished();

            callBack.accept(this, result);
            return TickAction.REMOVE;

        } else {
            return TickAction.AWAIT;
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public int getChannel() {
        return dimension.getMCDim();
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    public float getWeight() {
        return ticksPerQuery / Math.max(1.0f, yesCom.invalidMoveHandler.getAvailableAccounts(dimension));
    }

    /* ------------------------ Private Methods ------------------------ */

    private void onPacket(Packet packet) {
        switch (type) {
            case DIGGING: {
                if (packet instanceof ServerBlockChangePacket) {
                    ServerBlockChangePacket blockChange = (ServerBlockChangePacket)packet;

                    if (blockChange.getRecord().getPosition().equals(position.toSerializable())) result = Result.LOADED;
                }
                break;
            }
            case INVALID_MOVE: {
                break;
            }
        }
    }

    private void onFinished() {
        player.removePacketListener(listenerID);

        if (result == Result.LOADED) {
            yesCom.dataHandler.onLoaded(getChunkPosition(), dimension);
        } else {
            yesCom.dataHandler.onUnloaded(getChunkPosition(), dimension);
        }
    }

    /* ------------------------ Public Methods ------------------------ */

    public void reschedule() {
        yesCom.logger.debug(String.format("Rescheduling query %s.", this));
        rescheduled = true;
    }

    public void cancel() {
        yesCom.logger.debug(String.format("Cancelling query %s.", this));
        cancelled = true;
    }

    public void setResult(Result result) {
        if (this.result == null) this.result = result;
    }

    public int getTickingTime() {
        return (int)(System.currentTimeMillis() - startTime);
    }

    public BlockPosition getPosition() {
        return position;
    }

    public ChunkPosition getChunkPosition() {
        return new ChunkPosition(position.getX() >> 4, position.getZ() >> 4);
    }

    public Dimension getDimension() {
        return dimension;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        DIGGING, INVALID_MOVE;
    }

    public enum Result {
        LOADED, UNLOADED;
    }
}
