package ez.pogdog.yescom.query;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.queries.IsLoadedQuery;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.tracking.IResolver;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.util.Dimension;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains information about different queries.
 */
public class QueryRegistry {

    public static final Map<Class<? extends IQuery>, IQuery.IQueryInfo> QUERIES = new HashMap<>();

    static {
        QUERIES.put(IsLoadedQuery.class, new IQuery.IQueryInfo() {
            @Override
            public float getWeight(IQuery query, Dimension dimension) {
                YesCom yesCom = YesCom.getInstance();

                IsLoadedQuery.Type type = yesCom.configHandler.TYPE;
                if (query != null) {
                    if (!(query instanceof IsLoadedQuery)) return 0.0f;
                    type = ((IsLoadedQuery)query).getType();
                }

                switch (type) {
                    case DIGGING: {
                        break;
                    }
                    case INVALID_MOVE: {
                        float accounts = Math.max(1.0f, yesCom.invalidMoveHandler.getAvailableAccounts(dimension));
                        return ((float)yesCom.configHandler.LOADED_QUERIES_PER_TICK / 20.0f) / accounts;
                    }
                }

                return 0.0f;
            }

            @Override
            public float getMaxThroughPut(Dimension dimension) {
                YesCom yesCom = YesCom.getInstance();

                switch (yesCom.configHandler.TYPE) {
                    case DIGGING: { // TODO: Accounts in dimension
                        return 20.0f * (float)yesCom.configHandler.LOADED_QUERIES_PER_TICK;
                    }
                    case INVALID_MOVE: {
                        return 20.0f * (float)yesCom.configHandler.LOADED_QUERIES_PER_TICK *
                                yesCom.invalidMoveHandler.getAvailableAccounts(dimension);
                    }
                }

                return 0.0f;
            }
        });
    }
}
