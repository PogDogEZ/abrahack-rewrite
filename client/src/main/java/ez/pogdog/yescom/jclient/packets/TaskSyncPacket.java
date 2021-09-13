package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.task.TaskRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Packet.Info(name="task_sync", id=YCRegistry.ID_OFFSET + 3, side=Packet.Side.CLIENT)
public class TaskSyncPacket extends Packet {

    private final List<TaskRegistry.RegisteredTask> registeredTasks = new ArrayList<>();

    public TaskSyncPacket(List<TaskRegistry.RegisteredTask> tasks) {
        registeredTasks.addAll(tasks);
    }

    public TaskSyncPacket() {
        this(TaskRegistry.registeredTasks);
    }

    @Override
    public void read(InputStream inputStream) throws IOException { // Not actually gonna be reading this packet
        int tasksToRead = Registry.INT.read(inputStream);

        for (int index = 0; index < tasksToRead; ++index) {
            Registry.STRING.read(inputStream);
            Registry.STRING.read(inputStream);

            int paramsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int pIndex = 0; pIndex < paramsToRead; ++pIndex) YCRegistry.PARAM_DESCRIPTION.read(inputStream);
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.INT.write(registeredTasks.size(), outputStream);

        for (TaskRegistry.RegisteredTask task : registeredTasks) {
            Registry.STRING.write(task.getName(), outputStream);
            Registry.STRING.write(task.getDescription(), outputStream);

            Registry.UNSIGNED_SHORT.write(task.getParamDescriptions().size(), outputStream);
            for (TaskRegistry.ParamDescription paramDescription : task.getParamDescriptions())
                YCRegistry.PARAM_DESCRIPTION.write(paramDescription, outputStream);
        }
    }

    public void addTask(TaskRegistry.RegisteredTask task) {
        if (!registeredTasks.contains(task)) registeredTasks.add(task);
    }

    public void remove(TaskRegistry.RegisteredTask task) {
        registeredTasks.remove(task);
    }
}