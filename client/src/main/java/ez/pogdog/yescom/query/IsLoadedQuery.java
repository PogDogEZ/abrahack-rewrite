package ez.pogdog.yescom.query;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.packetlib.packet.Packet;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.util.BlockPosition;

import java.util.function.BiConsumer;

public class IsLoadedQuery implements IQuery {

    private final YesCom yesCom = YesCom.getInstance();

    private final BlockPosition position;
    private final int dimension;
    private final Priority priority;
    private final Type type;
    private final BiConsumer<IsLoadedQuery, Result> callBack;

    private Player player;
    private int listenerID;

    private long startTime;
    private boolean finished;

    private boolean cancelled;
    private Result result;

    public IsLoadedQuery(BlockPosition position, int dimension, Priority priority, Type type,
                         BiConsumer<IsLoadedQuery, Result> callBack) {
        this.position = position;
        this.dimension = dimension;
        this.priority = priority;
        this.type = type;
        this.callBack = callBack;

        startTime = System.currentTimeMillis();
        finished = false;

        cancelled = false;
        result = null;
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

        result = null;

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

        if (cancelled) {
            startTime = System.currentTimeMillis();
            cancelled = false;
            finished = false;

            yesCom.queryHandler.addQuery(this);

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
    public Priority getPriority() {
        return priority;
    }

    @Override
    public float getWeight() {
        return (float)yesCom.configHandler.TICKS_PER_QUERY / yesCom.invalidMoveHandler.getAvailableAccounts(dimension);
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
    }

    /* ------------------------ Public Methods ------------------------ */

    public void cancel() {
        yesCom.logger.debug(String.format("Cancelled query %s.", this));
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

    public int getDimension() {
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
