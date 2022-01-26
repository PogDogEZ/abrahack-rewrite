package ez.pogdog.yescom.query;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.util.Dimension;

/**
 * The template for a query. Queries are handled by the QueryHandler in the main thread, in synchronous with the tick.
 */
public interface IQuery {

    /**
     * @return The name of this type of query.
     */
    String getName();

    /**
     * The dimension this query is relevant in.
     */
    Dimension getDimension();

    /**
     * @return The object that requested this query be scheduled.
     */
    IRequester getRequester();

    HandleAction handle();
    TickAction tick();

    /**
     * Cancels the query.
     */
    void cancel();

    /**
     * Reschedules the query.
     */
    void reschedule();

    boolean isFinished();

    /**
     * The priority of handling queries. Higher priority queries will be handled first.
     * @return The priority of this query.
     */
    default Priority getPriority() {
        return Priority.MEDIUM;
    }

    /* ------------------------ Classes ------------------------ */

    enum HandleAction {
        AWAIT, START, REMOVE;
    }

    enum TickAction {
        AWAIT, REMOVE;
    }

    enum Priority {
        EXTREME, HIGH, MEDIUM, LOW;
    }

    /**
     * Static information accessor for queries.
     */

    interface IQueryInfo {
        /**
         * The "weight" of a query.
         * @param query The query, can be null.
         * @param dimension The dimension.
         * @return The weight of the query.
         */
        float getWeight(IQuery query, Dimension dimension);

        // TODO: If it's inversely proportional to the weight, why not just calculate it form the weight instead?
        /**
         * The max throughput of queries for a given dimension, should be inversely proportional to the weight.
         * @param dimension The dimension.
         * @return The max throughput.
         */
        float getMaxThroughPut(Dimension dimension);
    }
}
