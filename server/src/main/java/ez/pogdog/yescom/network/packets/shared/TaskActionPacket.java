package ez.pogdog.yescom.network.packets.shared;

import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.task.ActiveTask;
import ez.pogdog.yescom.util.task.parameter.Parameter;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Sent by either party to perform an action on a task.
 */
@Packet.Info(name="task_action", id=YCRegistry.ID_OFFSET + 5, side=Packet.Side.BOTH)
public class TaskActionPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private final List<Parameter> taskParameters = new ArrayList<>();

    private Action action;
    private long actionID;
    private String taskName;
    private int taskID;
    private boolean loadedChunkTask;

    private float progress;
    private int timeElapsed;
    private ChunkPosition currentPosition;
    private String result;

    public TaskActionPacket(Action action, long actionID, String taskName, int taskID, List<Parameter> taskParameters,
                            boolean loadedChunkTask, float progress, int timeElapsed, ChunkPosition currentPosition,
                            String result) {
        this.action = action;
        this.actionID = actionID;
        this.taskName = taskName;
        this.taskID = taskID;
        this.loadedChunkTask = loadedChunkTask;
        this.progress = progress;
        this.timeElapsed = timeElapsed;
        this.currentPosition = currentPosition;
        this.result = result;
        this.taskParameters.addAll(taskParameters);
    }

    public TaskActionPacket(long actionID, String taskName, List<Parameter> taskParameters) {
        this(Action.START, actionID, taskName, 0, taskParameters, false, 0.0f, 0,
                new ChunkPosition(0, 0), "");
    }

    public TaskActionPacket(String taskName, List<Parameter> taskParameters) {
        this(-1, taskName, taskParameters);
    }

    public TaskActionPacket(Action action, long actionID, ActiveTask task) {
        this(action, actionID, task.getRegisteredTask().getName(), task.getID(), task.getParameters(), false,
                0.0f, 0, new ChunkPosition(0, 0), "");
    }

    public TaskActionPacket(Action action, ActiveTask task) {
        this(action, -1, task);
    }

    public TaskActionPacket(Action action, long actionID, ActiveTask task, boolean loadedChunkTask, ChunkPosition currentPosition) {
        this(action, actionID, task.getRegisteredTask().getName(), task.getID(), task.getParameters(), loadedChunkTask,
                task.getProgress(), task.getTimeElapsed(), currentPosition, "");
    }

    public TaskActionPacket(Action action, ActiveTask task, boolean loadedChunkTask, ChunkPosition currentPosition) {
        this(action, -1, task, loadedChunkTask, currentPosition);
    }

    public TaskActionPacket(Action action, long actionID, int taskID, List<Parameter> taskParameters) {
        this(action, actionID, "", taskID, taskParameters, false, 0.0f, 0,
                new ChunkPosition(0, 0), "");
    }

    public TaskActionPacket(Action action, long actionID, int taskID) {
        this(action, actionID, taskID, new ArrayList<>());
    }

    public TaskActionPacket(Action action, int taskID) {
        this(action, -1, taskID);
    }

    public TaskActionPacket(int taskID, boolean loadedChunkTask, float progress, int timeElapsed, ChunkPosition currentPosition) {
        this(Action.UPDATE, -1, "", taskID, new ArrayList<>(), loadedChunkTask, progress, timeElapsed, currentPosition, "");
    }

    public TaskActionPacket(int taskID, float progress, int timeElapsed, ChunkPosition currentPosition) {
        this(taskID, true, progress, timeElapsed, currentPosition);
    }

    public TaskActionPacket(ActiveTask task, float progress, int timeElapsed, ChunkPosition currentPosition) {
        this(task.getID(), progress, timeElapsed, currentPosition);
    }

    public TaskActionPacket(int taskID, float progress, int timeElapsed) {
        this(taskID, false, progress, timeElapsed, new ChunkPosition(0, 0));
    }

    public TaskActionPacket(ActiveTask task, float progress, int timeElapsed) {
        this(task.getID(), progress, timeElapsed, new ChunkPosition(0, 0));
    }

    public TaskActionPacket(int taskID, String result) {
        this(Action.RESULT, -1, "", taskID, new ArrayList<>(), false, 0.0f, 0,
                new ChunkPosition(0, 0), result);
    }

    public TaskActionPacket(ActiveTask task, String result) {
        this(task.getID(), result);
    }

    public TaskActionPacket() {
        this(Action.ADD, -1, "", 0, new ArrayList<>(), false, 0.0f, 0,
                new ChunkPosition(0, 0), "");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);
        actionID = Registry.LONG.read(inputStream);

        switch (action) {
            case START: {
                taskName = Registry.STRING.read(inputStream);

                taskParameters.clear();
                int paramsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index = 0; index < paramsToRead; ++index) taskParameters.add(YCRegistry.PARAMETER.read(inputStream));
                break;
            }
            case ADD: {
                taskID = Registry.UNSIGNED_SHORT.read(inputStream);
                taskName = Registry.STRING.read(inputStream);

                taskParameters.clear();
                int paramsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index = 0; index < paramsToRead; ++index) taskParameters.add(YCRegistry.PARAMETER.read(inputStream));
                break;
            }
            case STOP:
            case REMOVE: {
                taskID = Registry.UNSIGNED_SHORT.read(inputStream);
                break;
            }
            case UPDATE: {
                taskID = Registry.UNSIGNED_SHORT.read(inputStream);
                loadedChunkTask = Registry.BOOLEAN.read(inputStream);
                progress = Registry.FLOAT.read(inputStream);
                timeElapsed = Registry.INTEGER.read(inputStream);

                if (loadedChunkTask) currentPosition = YCRegistry.CHUNK_POSITION.read(inputStream);
                break;
            }
            case RESULT: {
                taskID = Registry.UNSIGNED_SHORT.read(inputStream);
                result = Registry.STRING.read(inputStream);
                break;
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);
        Registry.LONG.write(actionID, outputStream);

        switch (action) {
            case START: {
                Registry.STRING.write(taskName, outputStream);

                Registry.UNSIGNED_SHORT.write(taskParameters.size(), outputStream);
                for (Parameter parameter : taskParameters) YCRegistry.PARAMETER.write(parameter, outputStream);
                break;
            }
            case ADD: {
                Registry.UNSIGNED_SHORT.write(taskID, outputStream);
                Registry.STRING.write(taskName, outputStream);

                Registry.UNSIGNED_SHORT.write(taskParameters.size(), outputStream);
                for (Parameter parameter : taskParameters) YCRegistry.PARAMETER.write(parameter, outputStream);
                break;
            }
            case STOP:
            case REMOVE: {
                Registry.UNSIGNED_SHORT.write(taskID, outputStream);
                break;
            }
            case UPDATE: {
                Registry.UNSIGNED_SHORT.write(taskID, outputStream);
                Registry.BOOLEAN.write(loadedChunkTask, outputStream);
                Registry.FLOAT.write(progress, outputStream);
                Registry.INTEGER.write(timeElapsed, outputStream);

                if (loadedChunkTask) YCRegistry.CHUNK_POSITION.write(currentPosition, outputStream);
                break;
            }
            case RESULT: {
                Registry.UNSIGNED_SHORT.write(taskID, outputStream);
                Registry.STRING.write(result, outputStream);
                break;
            }
        }
    }

    /**
     * @return The parameters used to start the task.
     */
    public List<Parameter> getTaskParameters() {
        return new ArrayList<>(taskParameters);
    }

    public void addTaskParameter(Parameter parameter) {
        taskParameters.add(parameter);
    }

    public void setTaskParameters(List<Parameter> parameters) {
        taskParameters.clear();
        taskParameters.addAll(parameters);
    }

    public void addTaskParameters(List<Parameter> parameters) {
        taskParameters.addAll(parameters);
    }

    public void removeTaskParameter(Parameter parameter) {
        taskParameters.remove(parameter);
    }

    /**
     * @return The action being performed.
     */
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * @return The ID of the action being performed.
     */
    public long getActionID() {
        return actionID;
    }

    public void setActionID(long actionID) {
        this.actionID = actionID;
    }

    /**
     * @return The name of the registered task.
     */
    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * @return The unique ID of the task.
     */
    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    /**
     * @return Whether or not the task is a loaded chunk task.
     */
    public boolean isLoadedChunkTask() {
        return loadedChunkTask;
    }

    public void setLoadedChunkTask(boolean loadedChunkTask) {
        this.loadedChunkTask = loadedChunkTask;
    }

    /**
     * @return The progress of the task.
     */
    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    /**
     * @return The time elapsed since the task started.
     */
    public int getTimeElapsed() {
        return timeElapsed;
    }

    public void setTimeElapsed(int timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    /**
     * @return The current position of the task, only applicable if the task is a loaded chunk task.
     */
    public ChunkPosition getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(ChunkPosition currentPosition) {
        this.currentPosition = currentPosition;
    }

    /**
     * @return A formatted result of the task.
     */
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public enum Action {
        START, STOP,
        ADD, REMOVE,
        UPDATE, RESULT;
    }
}
