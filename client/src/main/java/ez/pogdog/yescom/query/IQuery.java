package ez.pogdog.yescom.query;

import ez.pogdog.yescom.YesCom;

/**
 * The template for a query. Queries are handled by the QueryHandler in the main thread, in synchronous with the tick.
 */
public interface IQuery {

    String getName();
    HandleAction handle();
    TickAction tick();

    boolean isFinished();

    /**
     * The priority of handling queries. Higher priority queries will be handled first.
     * @return The priority of this query.
     */
    default Priority getPriority() {
        return Priority.MEDIUM;
    }

    /**
     * How much "weight" each query takes up. If the weight reaches above 1.0f then the query handler will stop
     * processing queries that tick. This is to avoid sending too many packets at once.
     * @return The weight of this query.
     */
    default float getWeight() {
        return 1.0f / (float)YesCom.getInstance().configHandler.QUERIES_PER_TICK;
    }

    enum HandleAction {
        AWAIT, START, REMOVE;
    }

    enum TickAction {
        AWAIT, REMOVE;
    }

    enum Priority {
        USER, HIGH, MEDIUM, LOW;
    }
}
