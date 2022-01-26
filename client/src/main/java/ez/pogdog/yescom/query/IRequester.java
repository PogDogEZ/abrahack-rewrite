package ez.pogdog.yescom.query;

/**
 * A requester requests queries, when a query is scheduled, the requester is stored with the query for reference.
 */
public interface IRequester {

    /**
     * @return The name of the requester.
     */
    String getRequesterName();
}
