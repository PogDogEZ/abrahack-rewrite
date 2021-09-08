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
     * Returns information about the parameters as well as their names.
     * @return The descriptions.
     */
    Map<String, String> getParamDescriptions();
}
