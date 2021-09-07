package ez.pogdog.yescom.handlers.task;

import java.util.Map;

public interface ITask {

    void onTick();

    boolean isFinished();

    String getName();
    String getDescription();

    /**
     * Returns information about the parameters as well as their names.
     * @return The descriptions.
     */
    Map<String, String> getParamDescriptions();
}
