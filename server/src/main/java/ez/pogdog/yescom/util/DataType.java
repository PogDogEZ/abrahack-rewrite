package ez.pogdog.yescom.util;

public enum DataType {
    POSITION(Position.class), ANGLE(Angle.class),
    CHUNK_POSITION(ChunkPosition.class),
    DIMENSION(Dimension.class),
    PRIORITY(Priority.class),
    STRING(String.class), INTEGER(Integer.class), FLOAT(Float.class), BOOLEAN(Boolean.class);

    private final Class<?> clazz;

    DataType(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}