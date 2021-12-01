package ez.pogdog.yescom.util.task;

import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.task.parameter.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * A task that is currently active.
 */
public class ActiveTask {

    private final List<Parameter> parameters = new ArrayList<>();
    private final List<String> results = new ArrayList<>();

    private final RegisteredTask registeredTask;
    private final int taskID;

    private boolean loadedChunkTask;

    private float progress;
    private int timeElapsed;
    private ChunkPosition currentPosition;

    public ActiveTask(RegisteredTask registeredTask, int taskID, List<Parameter> parameters, float progress, int timeElapsed,
                      List<String> results) {
        this.registeredTask = registeredTask;
        this.taskID = taskID;
        this.parameters.addAll(parameters);

        this.progress = progress;
        this.timeElapsed = timeElapsed;
        this.results.addAll(results);
    }

    @Override
    public String toString() {
        return String.format("ActiveTask(registeredTask=%s, ID=%d)", registeredTask, taskID);
    }

    /**
     * @return The parameters that the task was initialized with.
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * @return The list of formatted results.
     */
    public List<String> getResults() {
        return results;
    }

    public void addResult(String result) {
        results.add(result);
    }

    public void setResults(List<String> results) {
        this.results.clear();
        this.results.addAll(results);
    }

    public void addResults(List<String> results) {
        this.results.addAll(results);
    }

    public void removeResult(String result) {
        results.remove(result);
    }

    /**
     * @return The registered task that this task is based on.
     */
    public RegisteredTask getRegisteredTask() {
        return registeredTask;
    }

    /**
     * @return The unique ID of this task.
     */
    public int getID() {
        return taskID;
    }

    /**
     * @return Whether or not this is a loaded chunk task.
     */
    public boolean isLoadedChunkTask() {
        return loadedChunkTask;
    }

    public void setLoadedChunkTask(boolean loadedChunkTask) {
        this.loadedChunkTask = loadedChunkTask;
    }

    /**
     * @return The progress of this task.
     */
    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    /**
     * @return The time elapsed in milliseconds.
     */
    public int getTimeElapsed() {
        return timeElapsed;
    }

    public void setTimeElapsed(int timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    /**
     * @return The current position this task is scanning at, only applicable if this is a loaded chunk task.
     */
    public ChunkPosition getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(ChunkPosition currentPosition) {
        this.currentPosition = currentPosition;
    }
}
