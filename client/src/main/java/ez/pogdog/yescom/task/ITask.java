package ez.pogdog.yescom.task;

import ez.pogdog.yescom.query.IRequester;
import ez.pogdog.yescom.util.ChunkPosition;

import java.util.List;

public interface ITask extends IRequester {

    @Override
    default String getRequesterName() {
        return "task";
    }

    void tick();
    void finish();

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
     * For network compatibility, gives the task's "registered task", a class defining how this class behaves.
     * @return The registered task.
     */
    TaskRegistry.RegisteredTask getRegisteredTask();

    /**
     * For network compatibility, says which parameters have what values for this task.
     * @return The parameters.
     */
    List<TaskRegistry.Parameter> getParameters(); // FIXME: Better way of doing this, maybe use reflection?

    boolean isFinished();

    /**
     * @return The results this task has produced.
     */
    List<Object> getResults();

    int getTimeElapsed();

    boolean hasProgress();
    float getProgress();

    boolean hasCurrentPosition();
    ChunkPosition getCurrentPosition();
}
