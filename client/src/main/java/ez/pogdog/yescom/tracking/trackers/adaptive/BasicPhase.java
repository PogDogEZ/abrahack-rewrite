package ez.pogdog.yescom.tracking.trackers.adaptive;

import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.util.ChunkPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BasicPhase implements IPhase {

    protected final List<Offset> offsets = new ArrayList<>();

    protected final int updateTime;

    public BasicPhase(int updateTime, List<Offset> offsets) {
        this.updateTime = updateTime;
        this.offsets.addAll(offsets);
    }

    @Override
    public int getUpdateTime() {
        return updateTime;
    }

    @Override
    public int getMaxOffsets() {
        return offsets.size();
    }

    @Override
    public ChunkPosition getOffset(int index, float distance) {
        Offset offset = offsets.get(Math.min(offsets.size() - 1, index));
        return new ChunkPosition((int)(offset.getOffsetX() * distance), (int)(offset.getOffsetZ() * distance));
    }

    @Override
    public IsLoadedQuery.Result getResult(int index) {
        return offsets.get(Math.min(offsets.size() - 1, index)).getExpectedResult();
    }

    public static class Offset {

        private final float offsetX;
        private final float offsetZ;
        private final IsLoadedQuery.Result expectedResult;

        public Offset(float offsetX, float offsetZ, IsLoadedQuery.Result expectedResult) {
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
            this.expectedResult = expectedResult;
        }

        public float getOffsetX() {
            return offsetX;
        }

        public float getOffsetZ() {
            return offsetZ;
        }

        public IsLoadedQuery.Result getExpectedResult() {
            return expectedResult;
        }
    }
}
