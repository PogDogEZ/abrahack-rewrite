package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.task.TaskRegistry;
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

@Packet.Info(name="task_action", id=YCRegistry.ID_OFFSET + 4, side=Packet.Side.BOTH)
public class TaskActionPacket extends Packet {

    private Action action;
    private ITask task;
    private TaskRegistry.RegisteredTask registeredTask;
    private int taskID;

    public TaskActionPacket(Action action, ITask task, TaskRegistry.RegisteredTask registeredTask, int taskID) {
        this.action = action;
        this.task = task;
        this.registeredTask = registeredTask;
        this.taskID = taskID;
    }

    public TaskActionPacket(ITask task, int taskID) {
        this(Action.ADD, task, null, taskID);
    }

    public TaskActionPacket(ITask task) {
        this(task, task.getID());
    }

    public TaskActionPacket(int taskID) {
        this(Action.REMOVE, null, null, taskID);
    }

    public TaskActionPacket() {
        this(Action.ADD, null, null, 0);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = new EnumType<>(Action.class).read(inputStream);

        switch (action) {
            case START: {
                TaskRegistry.RegisteredTask task = TaskRegistry.getTask(Registry.STRING.read(inputStream));

                if (task != null) { // Holy fuck uh this is a lot of weird class stuff hopefully it works
                    List<Object> parameters = new ArrayList<>();

                    int paramsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                    for (int index = 0; index < paramsToRead; ++index) {
                        TaskRegistry.ParamDescription paramDescription = task.getParamDescriptions().get(index);
                        try {
                            Type<?> type = Registry.knownTypes.get(paramDescription.getDataType().getClazz()).newInstance();

                            switch (paramDescription.getInputType()) {
                                case SINGULAR: {
                                    parameters.add(type.read(inputStream));
                                    break;
                                }
                                case ARRAY: {
                                    List<Object> data = new ArrayList<>();

                                    int elementsToRead = Registry.INT.read(inputStream);
                                    for (int eIndex = 0; eIndex < elementsToRead; ++eIndex) data.add(type.read(inputStream));

                                    parameters.add(data.toArray());
                                    break;
                                }
                            }
                        } catch (InstantiationException | IllegalAccessException ignored) {
                        }
                    }

                    try {
                        Constructor<? extends ITask> constructor = task.getTaskClazz().getConstructor(
                                (Class<?>[])task.getParamDescriptions().stream()
                                        .map(paramDescription -> paramDescription.getDataType().getClazz())
                                        .toArray());
                        this.task = constructor.newInstance(parameters.toArray());

                    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ignored) {
                    }
                }

                break;
            }
            case ADD: {
                registeredTask = TaskRegistry.getTask(Registry.STRING.read(inputStream));
                taskID = Registry.UNSIGNED_SHORT.read(inputStream);
                break;
            }
            case STOP:
            case REMOVE: {
                taskID = Registry.UNSIGNED_SHORT.read(inputStream);
                break;
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        new EnumType<>(Action.class).write(action, outputStream);

        switch (action) {
            case START: { // I can't be bothered to write this since we won't be using it anyway
                break;
            }
            case ADD: {
                Registry.STRING.write(registeredTask.getName(), outputStream);
                Registry.UNSIGNED_SHORT.write(taskID, outputStream);
                break;
            }
            case STOP:
            case REMOVE: {
                Registry.UNSIGNED_SHORT.write(taskID, outputStream);
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

    public TaskRegistry.RegisteredTask getRegisteredTask() {
        return registeredTask;
    }

    public void setRegisteredTask(TaskRegistry.RegisteredTask registeredTask) {
        this.registeredTask = registeredTask;
    }

    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public enum Action {
        START, STOP,
        ADD, REMOVE;
    }
}
