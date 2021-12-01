package ez.pogdog.yescom.util.task.parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains information about the parameters used to start a task.
 */
public class Parameter {

    private final List<Object> values = new ArrayList<>();

    private final ParamDescription paramDescription;

    /*
    public Parameter(ParamDescription paramDescription, Object value) {
        this.paramDescription = paramDescription;
        values.add(value);
    }
     */

    public Parameter(ParamDescription paramDescription, List<?> values) {
        this.paramDescription = paramDescription;
        this.values.addAll(values);
    }

    @Override
    public String toString() {
        return String.format("Parameter(description=%s, values=%s)", paramDescription, values);
    }

    public ParamDescription getParamDescription() {
        return paramDescription;
    }

    public Object getValue() {
        return values.get(0);
    }

    public List<Object> getValues() {
        return new ArrayList<>(values);
    }
}