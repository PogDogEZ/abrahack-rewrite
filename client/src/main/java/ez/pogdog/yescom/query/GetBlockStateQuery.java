package ez.pogdog.yescom.query;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.packetlib.packet.Packet;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.util.BlockPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.function.BiConsumer;

/**
 * Query which returns the BlockState of any specified block position.
 * In the process of resolving the BlockState the server also loads the chunk.
 * Some tasks may ignore the query result and only utilize the chunk loading capabilities.
 */
public class GetBlockStateQuery implements IQuery {

    private final YesCom yesCom = YesCom.getInstance();

    private final BlockPosition position;
    private final Dimension dimension;
    private final Priority priority;
    private final BiConsumer<GetBlockStateQuery, BlockState> callBack;

    private Player player;
    private int listenerID;

    private long startTime;
    private boolean finished;

    private BlockState result;

    public GetBlockStateQuery(BlockPosition position, Dimension dimension, Priority priority,
                              BiConsumer<GetBlockStateQuery, BlockState> callBack) {
        this.position = position;
        this.dimension = dimension;
        this.priority = priority;
        this.callBack = callBack;

        startTime = System.currentTimeMillis();
        finished = false;

        result = null;
    }

    @Override
    public String toString() {
        return String.format("GetBlockStateQuery(position=%s, BlockState=%s)", position,
                (result == null ? "N/A" : result.getId() + " " + result.getId()));
    }

    @Override
    public String getName() {
        return "get_block_state";
    }

    @Override
    public HandleAction handle() {
        if (!yesCom.connectionHandler.isConnected() || yesCom.connectionHandler.getTimeSinceLastPacket() >= yesCom.configHandler.MAX_PACKET_TIME)
            return HandleAction.AWAIT;

        result = null;

        player = yesCom.connectionHandler.sendPacket(new ClientPlayerPlaceBlockPacket(position.toSerializable(),
                BlockFace.UP, Hand.MAIN_HAND, 0, 0, 0)); // Credit to Hobrin for finding this exploit
        if (player == null) { // It didn't send
            return HandleAction.AWAIT;
        } else {
            listenerID = player.addPacketListener(this::onPacket);

            startTime = System.currentTimeMillis();
            return HandleAction.START;
        }
    }

    @Override
    public TickAction tick() {
        if (!yesCom.connectionHandler.isConnected() ||
                yesCom.connectionHandler.getTimeSinceLastPacket() >= yesCom.configHandler.MAX_PACKET_TIME) {
            startTime = System.currentTimeMillis(); // Reset because we probably aren't receiving packets right now or some other reason
            return TickAction.AWAIT;
        }

        if (result != null) {
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
        return 1.0f / (float)yesCom.configHandler.QUERIES_PER_TICK / yesCom.invalidMoveHandler.getAvailableAccounts(dimension);
    }

    /* ------------------------ Private Methods ------------------------ */

    private void onPacket(Packet packet) {
        if (packet instanceof ServerBlockChangePacket) {
            ServerBlockChangePacket blockChange = (ServerBlockChangePacket) packet;

            if (blockChange.getRecord().getPosition().equals(position.toSerializable()))
                result = blockChange.getRecord().getBlock();
        }
    }

    private void onFinished() {
        player.removePacketListener(listenerID);
    }

    /* ------------------------ Public Methods ------------------------ */

    public int getTickingTime() {
        return (int)(System.currentTimeMillis() - startTime);
    }

    public BlockPosition getPosition() {
        return position;
    }

    public Dimension getDimension() {
        return dimension;
    }
}
