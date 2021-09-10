package ez.pogdog.yescom.task;

import java.util.Map;

public interface ITask {

    void onTick();

    boolean isFinished();
    float getProgressPercent();

    /**
     * Calculates the duration until task completion.
     * @return Estimated duration until task completion (in seconds).
     */
    int getEstTimeToFinish();

    String getName();
    String getDescription();

    /**
     * Mainly added for tasks that involve loaded chunk results, but can be used for other shit
     * @return Any formatted result info
     */
    String getFormattedResult(Object result);

    /**
     * Returns information about the parameters as well as their names.
     * @return The descriptions.
     */
    Map<String, String> getParamDescriptions();
}
