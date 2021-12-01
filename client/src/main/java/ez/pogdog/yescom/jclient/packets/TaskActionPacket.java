package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.task.TaskRegistry;
import ez.pogdog.yescom.util.ChunkPosition;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sent by either party to perform an action on a task.
 */
@Packet.Info(name="task_action", id=YCRegistry.ID_OFFSET + 5, side=Packet.Side.BOTH)
public class TaskActionPacket extends Packet {

    private Action action;
    private ITask task;
    private int taskID;
    private boolean loadedChunkTask;

    private float progress;
    private int timeElapsed;
    private ChunkPosition currentPosition;
    private String result;

    public TaskActionPacket(Action action, ITask task, int taskID, boolean loadedChunkTask, float progress, int timeElapsed,
                            ChunkPosition currentPosition, String result) {
        this.action = action;
        this.task = task;
        this.taskID = taskID;
        this.loadedChunkTask = loadedChunkTask;
        this.progress = progress;
        this.timeElapsed = timeElapsed;
        this.currentPosition = currentPosition;
        this.result = result;
    }

    public TaskActionPacket(ITask task, int taskID) {
        this(Action.ADD, task, taskID, false, task.getProgress(), task.getTimeElapsed(), new ChunkPosition(0, 0), "");
    }

    public TaskActionPacket(ITask task) {
        this(task, task.getID());
    }

    public TaskActionPacket(int taskID) {
        this(Action.REMOVE, null, taskID, false, 0.0f, 0, new ChunkPosition(0, 0), "");
    }

    public TaskActionPacket(int taskID, boolean loadedChunkTask, float progress, int timeElapsed, ChunkPosition currentPosition) {
        this(Action.UPDATE, null, taskID, loadedChunkTask, progress, timeElapsed, currentPosition, "");
    }

    public TaskActionPacket(int taskID, float progress, int timeElapsed, ChunkPosition currentPosition) {
        this(taskID, true, progress, timeElapsed, currentPosition);
    }

    public TaskActionPacket(ITask task, float progress, int timeElapsed, ChunkPosition currentPosition) {
        this(task.getID(), progress, timeElapsed, currentPosition);
    }

    public TaskActionPacket(int taskID, float progress, int timeElapsed) {
        this(taskID, false, progress, timeElapsed, new ChunkPosition(0, 0));
    }

    public TaskActionPacket(ITask task, float progress, int timeElapsed) {
        this(task.getID(), progress, timeElapsed);
    }

    public TaskActionPacket(int taskID, String result) {
        this(Action.RESULT, null, taskID, false, 0.0f, 0, new ChunkPosition(0, 0), result);
    }

    public TaskActionPacket(ITask task, String result) {
        this(task.getID(), result);
    }

    public TaskActionPacket() {
        this(Action.ADD, null, 0, false, 0.0f, 0, new ChunkPosition(0, 0), "");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = new EnumType<>(Action.class).read(inputStream);

        switch (action) {
            case START: {
                TaskRegistry.RegisteredTask registeredTask = TaskRegistry.getTask(Registry.STRING.read(inputStream));
                List<TaskRegistry.Parameter> parameters = new ArrayList<>();

                int paramsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index = 0; index < paramsToRead; ++index) parameters.add(YCRegistry.PARAMETER.read(inputStream));

                if (registeredTask != null) {
                    try {
                        Constructor<? extends ITask> constructor = registeredTask.getTaskClazz().getConstructor(
                                registeredTask.getParamDescriptions().stream()
                                        .map(paramDescription -> {
                                            switch (paramDescription.getInputType()) {
                                                default:
                                                case SINGULAR: {
                                                    return paramDescription.getDataType().getClazz();
                                                }
                                                case ARRAY: {
                                                    return List.class;
                                                }
                                            }
                                        })
                                        .toArray(Class[]::new));
                        task = constructor.newInstance(parameters.stream().map(parameter -> {
                            switch (parameter.getParamDescription().getInputType()) {
                                default:
                                case SINGULAR: {
                                    return parameter.getValue();
                                }
                                case ARRAY: {
                                    return parameter.getValues();
                                }
                            }
                        }).toArray());

                    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException error) {
                        error.printStackTrace();
                    }
                }

                break;
            }
            case ADD: {
                taskID = Registry.UNSIGNED_SHORT.read(inputStream);
                TaskRegistry.RegisteredTask registeredTask = TaskRegistry.getTask(Registry.STRING.read(inputStream));
                List<TaskRegistry.Parameter> parameters = new ArrayList<>();

                int paramsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index = 0; index < paramsToRead; ++index) parameters.add(YCRegistry.PARAMETER.read(inputStream));

                if (registeredTask != null) {
                    try {
                        Constructor<? extends ITask> constructor = registeredTask.getTaskClazz().getConstructor(
                                (Class<?>[])registeredTask.getParamDescriptions().stream()
                                        .map(paramDescription -> paramDescription.getDataType().getClazz())
                                        .toArray());
                        task = constructor.newInstance(parameters.stream().map(parameter -> {
                            switch (parameter.getParamDescription().getInputType()) {
                                default:
                                case SINGULAR: {
                                    return parameter.getValue();
                                }
                                case ARRAY: {
                                    return parameter.getValues();
                                }
                            }
                        }).toArray());
                        task.setID(taskID);

                    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ignored) {
                    }
                }
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
        new EnumType<>(Action.class).write(action, outputStream);

        switch (action) {
            case START: {
                Registry.STRING.write(TaskRegistry.getTask(task.getClass()).getName(), outputStream);

                Registry.UNSIGNED_SHORT.write(task.getParameters().size(), outputStream);
                for (TaskRegistry.Parameter parameter : task.getParameters())
                    YCRegistry.PARAMETER.write(parameter, outputStream);
                break;
            }
            case ADD: {
                Registry.UNSIGNED_SHORT.write(task.getID(), outputStream);
                Registry.STRING.write(TaskRegistry.getTask(task.getClass()).getName(), outputStream);

                Registry.UNSIGNED_SHORT.write(task.getParameters().size(), outputStream);
                for (TaskRegistry.Parameter parameter : task.getParameters())
                    YCRegistry.PARAMETER.write(parameter, outputStream);
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

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public ITask getTask() {
        return task;
    }

    public void setTask(ITask task) {
        this.task = task;
    }

    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public boolean isLoadedChunkTask() {
        return loadedChunkTask;
    }

    public void setLoadedChunkTask(boolean loadedChunkTask) {
        this.loadedChunkTask = loadedChunkTask;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public int getTimeElapsed() {
        return timeElapsed;
    }

    public void setTimeElapsed(int timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    public ChunkPosition getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(ChunkPosition currentPosition) {
        this.currentPosition = currentPosition;
    }

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
