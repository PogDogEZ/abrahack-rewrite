package ez.pogdog.yescom.data;

import java.io.File;
import java.io.IOException;

public interface ISerializer {

    void setFile(File file);

    /**
     * Dump the data this serializer contains.
     * @throws IOException Thrown when the data cannot be written.
     */
    void dump() throws IOException;

    /**
     * Read the data from the file this serializer is assigned to.
     * @throws IOException Thrown when the data cannot be written.
     */
    void read() throws IOException;

    void add(ISerializable serializable);
    void remove(ISerializable serializable);
}
