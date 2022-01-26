package ez.pogdog.yescom.task;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.IHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Map<Integer, ITask> tasks = new ConcurrentHashMap<>();
    private final Map<ITask, Integer> taskResults = new HashMap<>();

    @Override
    public void tick() {
        tasks.forEach((taskID, task) -> {
            task.tick();

            if (yesCom.ycHandler != null) {
                yesCom.ycHandler.onTaskUpdate(task);
                List<Object> results = task.getResults();
                for (int index = taskResults.computeIfAbsent(task, value -> 0); index < results.size(); ++index)
                    yesCom.ycHandler.onTaskResult(task, results.get(index));
                taskResults.put(task, results.size()); // Update the last known results
            }

            if (task.isFinished()) removeTask(task);
        });
    }

    @Override
    public void exit() {
        tasks.forEach((taskID, task) -> task.finish());
    }

    /**
     * Gets a task by its ID.
     * @param taskID The task ID.
     * @return The task.
     */
    public ITask getTask(int taskID) {
        return tasks.get(taskID);
    }

    /**
     * @return The list of all running tasks.
     */
    public List<ITask> getTasks() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * Adds a task.
     * @param task The task to add.
     */
    public void addTask(ITask task) {
        if (task == null || tasks.containsValue(task)) return;

        int taskID = task.getID();
        while (tasks.containsKey(taskID)) ++taskID; // Find the next available task ID
        task.setID(taskID);
        tasks.put(taskID, task);

        if (yesCom.ycHandler != null) yesCom.ycHandler.onTaskAdded(task);
    }

    /**
     * Removes a task.
     * @param task The task to remove.
     */
    public void removeTask(ITask task) {
        if (task == null || !tasks.containsValue(task)) return;

        if (tasks.get(task.getID()) == task) {
            tasks.remove(task.getID());
        } else {
            for (Map.Entry<Integer, ITask> entry : tasks.entrySet()) {
                if (entry.getValue() == task) {
                    tasks.remove(entry.getKey());
                    break;
                }
            }
        }

        if (yesCom.ycHandler != null) yesCom.ycHandler.onTaskRemoved(task);
    }
}
