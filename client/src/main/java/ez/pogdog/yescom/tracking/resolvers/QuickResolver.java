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

    private RenderDistance renderDistance;

    private int checksTaken;
    private int phase;
    private float distX;
    private float distZ;

    private ChunkPosition xResolve;
    private ChunkPosition zResolve;

    public QuickResolver(ChunkPosition initialLoaded, Dimension dimension, int maxPhase, Consumer<QuickResolver> callBack) {
        this.initialLoaded = initialLoaded;
        this.dimension = dimension;
        this.maxStage = Math.min((int)Math.ceil(Math.log(yesCom.configHandler.RENDER_DISTANCE) / Math.log(2)), maxPhase);
        this.callBack = callBack;

        loadedCache.add(initialLoaded);

        checksTaken = 0;
        phase = 0;

        float halfRender = yesCom.configHandler.RENDER_DISTANCE / 2.0f;
        distX = halfRender;
        distZ = halfRender;

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
        return renderDistance;
    }

    @Override
    public Dimension getDimension() {
        return dimension;
    }

    private void queryPositions() {
        if (xResolve != null || zResolve != null) return;
        if (isComplete()) {
            float error = yesCom.configHandler.RENDER_DISTANCE / (float)Math.pow(2, phase);
            float halfRender = yesCom.configHandler.RENDER_DISTANCE / 2.0f;

            renderDistance = yesCom.dataHandler.newRenderDistance(initialLoaded.subtract((int)(distX - halfRender), (int)(distZ - halfRender)),
                    yesCom.configHandler.RENDER_DISTANCE, error, error);

            callBack.accept(this);
            return;
        }

        ++phase;

        xResolve = initialLoaded.add((int)distX, 0);
        zResolve = initialLoaded.add(0, (int)distZ);

        boolean equal = xResolve.equals(zResolve);

        if (loadedCache.contains(xResolve)) {
            onLoaded(xResolve);
            xResolve = null;

        } else if (unloadedCache.contains(xResolve)) {
            onUnloaded(xResolve);
            xResolve = null;
        }

        if (loadedCache.contains(zResolve)) {
            onLoaded(zResolve);
            zResolve = null;

        } else if (unloadedCache.contains(zResolve)) {
            onUnloaded(zResolve);
            zResolve = null;
        }

        if (equal && xResolve != null && zResolve != null) {
            yesCom.queryHandler.addQuery(new IsLoadedQuery(xResolve, dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                    this::onResult));
        } else if (xResolve != null) {
            yesCom.queryHandler.addQuery(new IsLoadedQuery(xResolve, dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                    this::onResult));
        } else if (zResolve != null) {
            yesCom.queryHandler.addQuery(new IsLoadedQuery(zResolve, dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                    this::onResult));
        } else {
            queryPositions();
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

        if (query.getChunkPosition().equals(xResolve)) xResolve = null;
        if (query.getChunkPosition().equals(zResolve)) zResolve = null;

        queryPositions();
    }

    private void onLoaded(ChunkPosition position) {
        if (position == null) return;
        if (!loadedCache.contains(position)) loadedCache.add(position);

        float dist = yesCom.configHandler.RENDER_DISTANCE / (float)Math.pow(2, phase);
        if (position.equals(xResolve)) distX += dist;
        if (position.equals(zResolve)) distZ += dist;
    }

    private void onUnloaded(ChunkPosition position) {
        if (position == null) return;
        if (!unloadedCache.contains(position)) unloadedCache.add(position);

        float dist = yesCom.configHandler.RENDER_DISTANCE / (float)Math.pow(2, phase);
        if (position.equals(xResolve)) distX -= dist;
        if (position.equals(zResolve)) distZ -= dist;
    }

    public ChunkPosition getInitialLoaded() {
        return initialLoaded;
    }
}
