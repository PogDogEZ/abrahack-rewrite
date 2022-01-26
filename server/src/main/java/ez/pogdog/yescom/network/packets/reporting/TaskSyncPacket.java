package ez.pogdog.yescom.network.packets.reporting;

import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.task.RegisteredTask;
import ez.pogdog.yescom.util.task.parameter.ParamDescription;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Packet.Info(name="task_sync", id=YCRegistry.ID_OFFSET + 14, side=Packet.Side.CLIENT)
public class TaskSyncPacket extends Packet {

    private final List<RegisteredTask> registeredTasks = new ArrayList<>();

    public TaskSyncPacket(List<RegisteredTask> tasks) {
        registeredTasks.addAll(tasks);
    }

    public TaskSyncPacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        int tasksToRead = Registry.UNSIGNED_SHORT.read(inputStream);

        for (int index = 0; index < tasksToRead; ++index) {
            String name = Registry.STRING.read(inputStream);
            String description = Registry.STRING.read(inputStream);

            List<ParamDescription> paramDescriptions = new ArrayList<>();
            int paramsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int pIndex = 0; pIndex < paramsToRead; ++pIndex) paramDescriptions.add(YCRegistry.PARAM_DESCRIPTION.read(inputStream));

            registeredTasks.add(new RegisteredTask(name, description, paramDescriptions));
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(registeredTasks.size(), outputStream); // Pls don't make 65535 tasks thanks

        for (RegisteredTask task : registeredTasks) {
            Registry.STRING.write(task.getName(), outputStream);
            Registry.STRING.write(task.getDescription(), outputStream);

            Registry.UNSIGNED_SHORT.write(task.getParamDescriptions().size(), outputStream);
            for (ParamDescription paramDescription : task.getParamDescriptions())
                YCRegistry.PARAM_DESCRIPTION.write(paramDescription, outputStream);
        }
    }

    public List<RegisteredTask> getTasks() {
        return new ArrayList<>(registeredTasks);
    }

    public void addTask(RegisteredTask task) {
        if (!registeredTasks.contains(task)) registeredTasks.add(task);
    }

    public void setTasks(List<RegisteredTask> tasks) {
        registeredTasks.clear();
        registeredTasks.addAll(tasks);
    }

    public void addTasks(List<RegisteredTask> tasks) {
        registeredTasks.addAll(tasks);
    }

    public void removeTask(RegisteredTask task) {
        registeredTasks.remove(task);
    }
}
