package ez.pogdog.yescom.util.task.parameter;

import ez.pogdog.yescom.util.DataType;

import java.util.Objects;

/**
 * Contains information about parameters needed to construct a task.
 */
public class ParamDescription {

    private final String name;
    private final String description;
    private final InputType inputType;
    private final DataType dataType;

    public ParamDescription(String name, String description, InputType inputType, DataType dataType) {
        this.name = name;
        this.description = description;
        this.inputType = inputType;
        this.dataType = dataType;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ParamDescription that = (ParamDescription)other;
        return name.equals(that.name) && description.equals(that.description) && inputType == that.inputType && dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, inputType, dataType);
    }

    @Override
    public String toString() {
        return String.format("ParamDescription(name=%s, itype=%s, dtype=%s)", name, inputType, dataType);
    }

    /**
     * The name of the parameter.
     */
    public String getName() {
        return name;
    }

    /**
     * The description of the parameter.
     */
    public String getDescription() {
        return description;
    }

    /**
     * The input type, whether it requires a single input or an array of inputs.
     */
    public InputType getInputType() {
        return inputType;
    }

    /**
     * The data type, i.e. chunk positions, integer, floats.
     */
    public DataType getDataType() {
        return dataType;
    }

    public enum InputType {
        SINGULAR, ARRAY;
    }
}