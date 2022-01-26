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

    private int timeElapsed;

    private boolean hasProgress;
    private float progress;

    private boolean hasCurrentPosition;
    private ChunkPosition currentPosition;
    private String result;

    public TaskActionPacket(Action action, long actionID, String taskName, int taskID, List<Parameter> taskParameters,
                            int timeElapsed, boolean hasProgress, float progress, boolean hasCurrentPosition,
                            ChunkPosition currentPosition, String result) {
        this.action = action;
        this.actionID = actionID;
        this.taskName = taskName;
        this.taskID = taskID;
        this.timeElapsed = timeElapsed;
        this.hasProgress = hasProgress;
        this.progress = progress;
        this.hasCurrentPosition = hasCurrentPosition;
        this.currentPosition = currentPosition;
        this.result = result;
        this.taskParameters.addAll(taskParameters);
    }

    public TaskActionPacket(Action action, long actionID, ActiveTask task) {
        this(action, actionID, task.getRegisteredTask().getName(), task.getID(), task.getParameters(), task.getTimeElapsed(),
                task.getHasProgress(), task.getProgress(), task.getHasCurrentPosition(), task.getCurrentPosition(), "");
    }

    public TaskActionPacket(Action action, ActiveTask task) {
        this(action, -1, task.getRegisteredTask().getName(), task.getID(), task.getParameters(), task.getTimeElapsed(),
                task.getHasProgress(), task.getProgress(), task.getHasCurrentPosition(), task.getCurrentPosition(), "");
    }

    public TaskActionPacket(long actionID, String taskName, List<Parameter> taskParameters) {
        this(Action.START, actionID, taskName, -1, taskParameters, 0, false, 0,
                false, null, "");
    }

    public TaskActionPacket(long actionID, int taskID) {
        this(Action.STOP, actionID, "", taskID, new ArrayList<Parameter>(), 0, false, 0,
                false, null, "");
    }

    public TaskActionPacket(ActiveTask task, int timeElapsed, boolean hasProgress, float progress, boolean hasCurrentPosition,
                            ChunkPosition currentPosition) {
        this(Action.UPDATE, task.getID(), task.getRegisteredTask().getName(), task.getID(), task.getParameters(), timeElapsed,
                hasProgress, progress, hasCurrentPosition, currentPosition, "");
    }

    public TaskActionPacket(ActiveTask task, String result) {
        this(Action.RESULT, task.getID(), task.getRegisteredTask().getName(), task.getID(), task.getParameters(), task.getTimeElapsed(),
                task.getHasProgress(), task.getProgress(), task.getHasCurrentPosition(), task.getCurrentPosition(), result);
    }

    public TaskActionPacket() {
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
                timeElapsed = Registry.INTEGER.read(inputStream);

                hasProgress = Registry.BOOLEAN.read(inputStream);
                if (hasProgress) progress = Registry.FLOAT.read(inputStream);

                hasCurrentPosition = Registry.BOOLEAN.read(inputStream);
                if (hasCurrentPosition) currentPosition = YCRegistry.CHUNK_POSITION.read(inputStream);
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
                Registry.INTEGER.write(timeElapsed, outputStream);

                Registry.BOOLEAN.write(hasProgress, outputStream);
                if (hasProgress) Registry.FLOAT.write(progress, outputStream);

                Registry.BOOLEAN.write(hasCurrentPosition, outputStream);
                if (hasCurrentPosition) YCRegistry.CHUNK_POSITION.write(currentPosition, outputStream);
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
     * @return The time elapsed since the task started.
     */
    public int getTimeElapsed() {
        return timeElapsed;
    }

    public void setTimeElapsed(int timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    /**
     * @return Whether or not the task in question has progress.
     */
    public boolean getHasProgress() {
        return hasProgress;
    }

    public void setHasProgress(boolean hasProgress) {
        this.hasProgress = hasProgress;
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
     * @return Whether or not the task in question has a current position.
     */
    public boolean getHasCurrentPosition() {
        return hasCurrentPosition;
    }

    public void setHasCurrentPosition(boolean hasCurrentPosition) {
        this.hasCurrentPosition = hasCurrentPosition;
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
