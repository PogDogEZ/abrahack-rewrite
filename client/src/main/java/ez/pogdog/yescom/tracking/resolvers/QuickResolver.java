package ez.pogdog.yescom.tracking.resolvers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.tracking.IResolver;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class QuickResolver implements IResolver {

    private final YesCom yesCom = YesCom.getInstance();

    private final List<ChunkPosition> loadedCache = new ArrayList<>();
    private final List<ChunkPosition> unloadedCache = new ArrayList<>();

    private final ChunkPosition initialLoaded;
    private final Dimension dimension;
    private final int maxStage;
    private final Consumer<QuickResolver> callBack;

    private final float renderDistance;

    private int checksTaken;

    private double xCenter;
    private double zCenter;
    private float xDist;
    private float zDist;
    private float errorX;
    private float errorZ;

    private ChunkPosition xResolve;
    private ChunkPosition zResolve;
    private int phase;

    public QuickResolver(ChunkPosition initialLoaded, Dimension dimension, int maxPhase, Consumer<QuickResolver> callBack) {
        this.initialLoaded = initialLoaded;
        this.dimension = dimension;
        this.maxStage = Math.min(20, maxPhase); // Anything more than 20 is ludicrous
        this.callBack = callBack;

        renderDistance = (float)yesCom.configHandler.RENDER_DISTANCE;

        loadedCache.add(initialLoaded);

        checksTaken = 0;

        xCenter = initialLoaded.getX();
        zCenter = initialLoaded.getZ();
        xDist = renderDistance / 2.0f;
        zDist = renderDistance / 2.0f;
        errorX = 0.0f;
        errorZ = 0.0f;

        phase = 0;
        queryPositions();
    }

    @Override
    public boolean isComplete() {
        return phase >= maxStage;
    }

    @Override
    public int getChecksTaken() {
        return checksTaken;
    }

    @Override
    public RenderDistance getRenderDistance() {
        return new RenderDistance(new ChunkPosition((int)xCenter, (int)zCenter), (int)renderDistance, errorX, errorZ);
    }

    @Override
    public Dimension getDimension() {
        return dimension;
    }

    private void queryPositions() {
        if (xResolve != null || zResolve != null) return;
        if (isComplete()) {
            callBack.accept(this);
            return;
        }

        ++phase;

        xResolve = initialLoaded.add((int)xDist, 0);
        zResolve = initialLoaded.add(0, (int)zDist);

        if (xResolve.equals(zResolve)) {
            if (loadedCache.contains(xResolve)) {
                onLoaded(xResolve);
                onLoaded(zResolve);

            } else if (unloadedCache.contains(zResolve)) {
                onUnloaded(xResolve);
                onUnloaded(zResolve);

            } else {
                yesCom.queryHandler.addQuery(new IsLoadedQuery(xResolve, dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                        this::onResult));
            }

        } else {
            if (loadedCache.contains(xResolve)) {
                onLoaded(xResolve);
            } else if (unloadedCache.contains(xResolve)) {
                onUnloaded(xResolve);
            } else {
                yesCom.queryHandler.addQuery(new IsLoadedQuery(xResolve, dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                        this::onResult));
            }

            if (loadedCache.contains(zResolve)) {
                onLoaded(zResolve);
            } else if (unloadedCache.contains(zResolve)) {
                onUnloaded(zResolve);
            } else {
                yesCom.queryHandler.addQuery(new IsLoadedQuery(zResolve, dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                        this::onResult));
            }
        }
    }

    private void onResult(IsLoadedQuery query, IsLoadedQuery.Result result) {
        ++checksTaken;
        switch (result) {
            case LOADED: {
                onLoaded(query.getChunkPosition());
                break;
            }
            case UNLOADED: {
                onUnloaded(query.getChunkPosition());
                break;
            }
        }
    }

    private void onLoaded(ChunkPosition position) {
        if (position == null) return;
        if (!loadedCache.contains(position)) loadedCache.add(position);

        float dist = renderDistance / (float)Math.pow(2, phase);

        if (position.equals(xResolve)) {
            xDist += dist;

            xCenter += dist;
            xResolve = null;
        }

        if (position.equals(zResolve)) {
            zDist += dist;

            zCenter += dist;
            zResolve = null;
        }

        queryPositions();
    }

    private void onUnloaded(ChunkPosition position) {
        if (position == null) return;
        if (!unloadedCache.contains(position)) unloadedCache.add(position);

        float dist = (float)Math.floor(renderDistance) / (float)Math.pow(2, phase);

        if (position.equals(xResolve)) {
            xDist -= dist;

            xCenter -= dist;
            xResolve = null;
        }

        if (position.equals(zResolve)) {
            zDist -= dist;

            zCenter -= dist;
            zResolve = null;
        }

        queryPositions();
    }

    public ChunkPosition getInitialLoaded() {
        return initialLoaded;
    }
}
