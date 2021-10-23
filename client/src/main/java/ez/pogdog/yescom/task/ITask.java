package ez.pogdog.yescom.task;

import java.util.List;

public interface ITask {

    void onTick();
    void onFinished();

    /**
     * Task ID is a unique ID given to each task, assigned by the class that handles tasks.
     * @return The task ID.
     */
    int getID();

    /**
     * Only called by the class that assigns IDs to tasks, duh. Don't call this from anything else.
     * @param ID The unique ID given to represent this task.
     */
    void setID(int ID);

    /**
     * For network compatibility, says which parameters have what values for this task.
     * @return The parameters.
     */
    List<TaskRegistry.Parameter> getParameters(); // FIXME: Better way of doing this, maybe use reflection?

    boolean isFinished();

    int getTimeElapsed();
    float getProgress();

    /**
     * Mainly added for tasks that involve loaded chunk results, but can be used for other shit
     * @return Any formatted result info
     */
    String getFormattedResult(Object result);
}
