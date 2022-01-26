package ez.pogdog.yescom.tracking.resolvers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.query.queries.IsLoadedQuery;
import ez.pogdog.yescom.tracking.IResolver;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.List;

public class DimChangeResolver implements IResolver {

    private final YesCom yesCom = YesCom.getInstance();

    private final List<IsLoadedQuery> queries = new ArrayList<>();

    private final ChunkPosition startPosition;
    private final Dimension dimension;

    private QuickResolver quickResolver;

    private int checksTaken;

    public DimChangeResolver(ChunkPosition startPosition, Dimension dimension) {
        this.startPosition = startPosition;
        this.dimension = dimension;

        checksTaken = 0;
        quickResolver = null;
    }

    @Override
    public void resolve() {

        int renderDistance;

        switch (dimension) {
            default:
            case NETHER: {
                renderDistance = yesCom.configHandler.RENDER_DISTANCE / 2 - 1;
                break;
            }
            case OVERWORLD: {
                renderDistance = yesCom.configHandler.RENDER_DISTANCE - 1;
                break;
            }
        }

        int x = 0;
        int z = 0;
        int dx = 0;
        int dz = -1;

        /*
        for (int currentIndex = 0; currentIndex < 100; ++currentIndex) {
            if (-5 < x && x <= 5 && -5 < z && z <= 5) {
                ChunkPosition position = startPosition.add(x * renderDistance, z * renderDistance);

                if (!yesCom.trackingHandler.isChunkKnown(position, dimension)) {
                    IsLoadedQuery isLoadedQuery = new IsLoadedQuery(position, dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                            (query, result) -> {
                        ++checksTaken;
                        queries.remove(query);

                        synchronized (this) {
                            if (result == IsLoadedQuery.Result.LOADED) {
                                queries.forEach(IsLoadedQuery::cancel);
                                queries.clear();

                                quickResolver = new QuickResolver(query.getChunkPosition(), dimension, 4, resolver -> {});
                            }
                        }
                    });

                    synchronized (this) {
                        queries.add(isLoadedQuery);
                        yesCom.queryHandler.addQuery(isLoadedQuery);
                    }
                }
            }

            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 -z)) {
                int oldDx = dx;
                dx = -dz;
                dz = oldDx;
            }

            x += dx;
            z += dz;
        }
         */
    }

    @Override
    public boolean isComplete() {
        return queries.isEmpty() && (quickResolver == null || quickResolver.isComplete());
    }

    @Override
    public int getChecksTaken() {
        return checksTaken;
    }

    @Override
    public RenderDistance getRenderDistance() {
        return quickResolver == null ? null : quickResolver.getRenderDistance();
    }

    @Override
    public Dimension getDimension() {
        return dimension;
    }

    public ChunkPosition getStartPosition() {
        return startPosition;
    }
}
