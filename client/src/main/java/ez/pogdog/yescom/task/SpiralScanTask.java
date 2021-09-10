package ez.pogdog.yescom.task;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.HashMap;
import java.util.Map;

/**
 * Written by NathanW 9/8/21
 */
public class SpiralScanTask implements ITask {

    private final YesCom yesCom = YesCom.getInstance();

    private final ChunkPosition startPos;
    private final int chunkSkip;
    private final Dimension dimension;

    private int currentQueries;
    private int currentIndex;
    private int[] nextSpiral;

    private SpiralAlgorithm spiral;

    /**
     * @param startPos The center of the spiral
     * @param chunkSkip The server render distance * 2
     * @param dimension The dimension to scan in
     */
    public SpiralScanTask(ChunkPosition startPos, int chunkSkip, Dimension dimension) {
        this.startPos = new ChunkPosition(startPos.getX() / chunkSkip, startPos.getZ() / chunkSkip);
        this.chunkSkip = chunkSkip;
        this.dimension = dimension;
        this.spiral = new SpiralAlgorithm();
        nextSpiral = new int[] {0,0};

        yesCom.logger.debug("Starting spiral scan task.");
        yesCom.logger.debug(String.format("Start: %s.", startPos));
        yesCom.logger.debug("Dimension: " + dimension);

        currentQueries = 0;
        currentIndex = 0;
    }

    /* ------------------------ Implementations ------------------------ */

    @Override
    public void onTick() {
        while (currentQueries < 200) {
            tickSpiral();
            ++currentIndex;

            synchronized (this) {
                ++currentQueries;

                yesCom.queryHandler.addQuery(new IsLoadedQuery(getNextSpiral().getPosition(),
                        dimension, IQuery.Priority.LOW, yesCom.configHandler.TYPE,
                        (query, result) -> {
                            synchronized (this) {
                                --currentQueries;

                                if (result == IsLoadedQuery.Result.LOADED)
                                    onLoaded(new ChunkPosition(query.getPosition().getX() / 16, query.getPosition().getZ() / 16));
                            }
                        }));
            }
        }
        yesCom.logger.infoDisappearing(String.format("Scanning: %d, %d / %d, %d, %s, %dms.",
                yesCom.queryHandler.getWaitingSize(), yesCom.queryHandler.getTickingSize(),
                currentQueries, currentIndex, getNextSpiral(), yesCom.connectionHandler.getTimeSinceLastPacket()));
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public float getProgressPercent() {
        return 0;
    }

    @Override
    public int getEstTimeToFinish() {
        return 0;
    }

    @Override
    public String getName() {
        return "spiral_scan";
    }

    @Override
    public String getDescription() {
        return "A scan task that spirals out from a given coordinate";
    }

    @Override
    public String getFormattedResult(Object result) {
        return String.format("Found loaded (spiral): %s (dim %s).", result, dimension);
    }

    @Override
    public Map<String, String> getParamDescriptions() {
        Map<String, String> params = new HashMap<>();

        params.put("startPos", "The starting position for the scan.");
        params.put("chunkSkip", "The number of chunks to linearly skip while scanning (recommended the render distance of the server * 2).");
        params.put("dimension", "The dimension to scan in.");

        return params;
    }

    /* ------------------------ Private methods ------------------------ */

    private void onLoaded(ChunkPosition chunkPos) {
        yesCom.logger.info(getFormattedResult(chunkPos));

        //TODO: Can remove me later, just for testing and felt like afking it
        yesCom.saveHandler.saveUncompressed(getFormattedResult(chunkPos.getPosition()), "SpiralScanLog");
        //yesCom.playerTrackingHandler.onLoaded(chunkPos, dimension);
        //yesCom.saveHandler.onLoaded(chunkPos, dimension);
    }

    private ChunkPosition getNextSpiral() {
        return new ChunkPosition(startPos.getX() + (nextSpiral[0] * chunkSkip),
                startPos.getZ() + (nextSpiral[1] * chunkSkip));
    }

    private void tickSpiral() {
        nextSpiral = spiral.next();
    }

    public ChunkPosition getStartPos() {
        return startPos;
    }

    /**
     * Spiral algorithm - NOTE: USED INTERNET FOR HELP, JUST FOR PROOF OF CONCEPT
     * TODO: Put this somewhere else/clean up? Algorithm interface? More efficient?
     */
    private class SpiralAlgorithm {
        int radius, side, step;

        public SpiralAlgorithm() {
            this.radius = 0;
            this.side = 0;
            this.step = 0;
        }

        public int[] next() {
            int[] i = {0, 0};
            if (radius == 0) {
                radius++;
                step = -radius;
                return i;
            }
            switch (side) {
                case 0:
                    i = new int[]{radius, -step};
                    break;
                case 1:
                    i = new int[]{-step, -radius};
                    break;
                case 2:
                    i = new int[]{-radius, step};
                    break;
                case 3:
                    i = new int[]{step, radius};
                    break;
            }
            step++;
            if (step >= radius) {
                side++;
                if (side % 4 == 0) {
                    radius++;
                    side = 0;
                }
                step = -radius;
            }
            return i;
        }
    }
}
