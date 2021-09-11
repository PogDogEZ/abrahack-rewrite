package ez.pogdog.yescom.task;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

/**
 * Yeah uh I don't think I really need to explain this.
 */
public interface ILoadedChunkTask extends ITask {

    YesCom yesCom = YesCom.getInstance();

    /**
     * Should be called when a loaded chunk is found.
     * @param chunkPosition The position of the chunk
     * @param dimension The dimension.
     */
    default void onLoaded(ChunkPosition chunkPosition, Dimension dimension) {
        if (yesCom.handler != null) yesCom.handler.onLoadedChunk(chunkPosition, dimension);
    }
}
