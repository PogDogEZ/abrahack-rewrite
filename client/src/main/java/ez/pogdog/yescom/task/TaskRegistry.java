package ez.pogdog.yescom.task;

import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;
import ez.pogdog.yescom.util.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Holds all registered tasks.
 */
public class TaskRegistry {

    public static final List<RegisteredTask> registeredTasks = new ArrayList<>();

    static {
        registeredTasks.add(new RegisteredTask(
                BasicScanTask.class,
                "basic_scan",
                "A basic scanning task that scans a rectangle looking for loaded chunks.",
                new ParamDescription("startPos", "The starting position for the scan.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.CHUNK_POSITION),
                new ParamDescription("endPos", "The end position that the scan will finish at.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.CHUNK_POSITION),
                new ParamDescription("chunkSkip",
                        "The number of chunks to linearly skip while scanning " +
                                "(recommended to use the render distance of the server * 2).",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.INTEGER),
                new ParamDescription("dimension", "The dimension to scan in.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.DIMENSION),
                new ParamDescription("priority", "The query priority.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.PRIORITY)
        ));

        registeredTasks.add(new RegisteredTask(
                SpiralScanTask.class,
                "spiral_scan",
                "A scan task that spirals out from a given coordinate.",
                new ParamDescription("startPos", "The starting position for the scan.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.CHUNK_POSITION),
                new ParamDescription("chunkSkip",
                        "The number of chunks to linearly skip while scanning " +
                                "(recommended the render distance of the server * 2).",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.INTEGER),
                new ParamDescription("dimension", "The dimension to scan in.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.DIMENSION),
                new ParamDescription("priority", "The query priority.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.PRIORITY)
        ));

        registeredTasks.add(new RegisteredTask(
                HighwayScanTask.class,
                "highway_scan",
                "A scan that only checks the highways, for a given distance.",
                new ParamDescription("maxDistance", "The maximum distance to scan to.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.INTEGER),
                new ParamDescription("minDistance", "The minumum distance to scan from.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.INTEGER),
                new ParamDescription("chunkSkip",
                        "The number of chunks to linearly skip while scanning " +
                                "(recommended the render distance of the server * 2).",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.INTEGER),
                new ParamDescription("dimension", "The dimension to scan in.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.DIMENSION),
                new ParamDescription("priority", "The query priority.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.PRIORITY)
        ));

        registeredTasks.add(new RegisteredTask(
                StaticScanTask.class,
                "static_scan",
                "A scan task that only checks coordinates from a given array.",
                new ParamDescription("positions", "Static positions to scan.",
                        ParamDescription.InputType.ARRAY, ParamDescription.DataType.CHUNK_POSITION),
                new ParamDescription("dimension", "The dimension to scan in.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.DIMENSION),
                new ParamDescription("priority", "The query priority.",
                        ParamDescription.InputType.SINGULAR, ParamDescription.DataType.PRIORITY)
        ));
    }

    public static RegisteredTask getTask(String name) {
        return registeredTasks.stream().filter(task -> task.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static RegisteredTask getTask(Class<? extends ITask> clazz) {
        return registeredTasks.stream().filter(task -> task.getTaskClazz().equals(clazz)).findFirst().orElse(null);
    }

    /**
     * Contains information about a registered task (for serialization purposes).
     */
    public static class RegisteredTask {

        private final Class<? extends ITask> taskClazz;

        private final String name;
        private final String description;
        private final List<ParamDescription> paramDescriptions;

        public RegisteredTask(Class<? extends ITask> taskClazz, String name, String description,
                              ParamDescription... paramDescriptions) {
            this.taskClazz = taskClazz;
            this.name = name;
            this.description = description;
            this.paramDescriptions = Arrays.asList(paramDescriptions);
        }

        public Class<? extends ITask> getTaskClazz() {
            return taskClazz;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public List<ParamDescription> getParamDescriptions() {
            return paramDescriptions;
        }
    }

    /**
     * Contains information about parameters needed to construct a task.
     */
    public static class ParamDescription {

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

        public enum DataType {
            POSITION(Position.class), ANGLE(Angle.class),
            CHUNK_POSITION(ChunkPosition.class),
            DIMENSION(Dimension.class),
            PRIORITY(IQuery.Priority.class),
            STRING(String.class), INTEGER(Integer.class), FLOAT(Float.class), BOOLEAN(Boolean.class);

            private final Class<?> clazz;

            DataType(Class<?> clazz) {
                this.clazz = clazz;
            }

            public Class<?> getClazz() {
                return clazz;
            }
        }
    }

    /**
     * This class is only for network compatibility.
     */
    public static class Parameter {

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
}
