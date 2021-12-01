package ez.pogdog.yescom.util.task;

import ez.pogdog.yescom.util.task.parameter.ParamDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contains information about a registered task, a task that is registered but not active.
 */
public class RegisteredTask {

    private final List<ParamDescription> paramDescriptions = new ArrayList<>();

    private final String name;
    private final String description;

    public RegisteredTask(String name, String description, List<ParamDescription> paramDescriptions) {
        this.name = name;
        this.description = description;
        this.paramDescriptions.addAll(paramDescriptions);
    }

    @Override
    public String toString() {
        return String.format("RegisteredTask(name=%s, description=%s)", name, description);
    }

    public List<ParamDescription> getParamDescriptions() {
        return new ArrayList<>(paramDescriptions);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
