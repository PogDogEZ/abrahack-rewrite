package ez.pogdog.yescom.data;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public interface ISerializer {

    void setFile(File file);
    void read() throws IOException;
    void write() throws IOException;

    /**
     * Gets all the serializable objects this serializer has.
     * @return A list of all serializable objects.
     */
    List<ISerializable> get();

    ISerializable get(BigInteger id);
    boolean has(BigInteger id);

    /**
     * Adds a serializable object to this serializer.
     * @param serializable The serializable object to add.
     */
    void add(ISerializable serializable);

    /**
     * Adds a list of serializable objects to this serializer.
     * @param serializables The serializable objects to add.
     */
    void addAll(List<ISerializable> serializables);
}
