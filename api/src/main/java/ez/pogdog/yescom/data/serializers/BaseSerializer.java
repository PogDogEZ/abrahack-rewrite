package ez.pogdog.yescom.data.serializers;

import ez.pogdog.yescom.data.ISerializable;
import ez.pogdog.yescom.data.ISerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseSerializer implements ISerializer {

    /**
     * Serializer instances mapped to their magic numbers.
     */
    public static final Map<Byte, BaseSerializer> serializers = new HashMap<>();

    static { // TODO: Serializers, duh
    }

    protected File file;

    private final byte magicNumber;

    protected BaseSerializer(byte magicNumber) {
        this.magicNumber = magicNumber;

        // This probably wouldn't be called
        if (!serializers.containsKey(magicNumber)) serializers.put(magicNumber, this);
    }

    @Override
    public void setFile(File file) {
        this.file = file;
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void dump() throws IOException {
        if (!file.exists() || file.isDirectory()) file.createNewFile();

        OutputStream outputStream = new FileOutputStream(file);

        outputStream.write(new byte[] { 89, 67, 79, 77, magicNumber });

        serializers.get(magicNumber).writeData(outputStream);

        outputStream.close();
    }

    @Override
    public void read() throws IOException {
        if (!file.exists() || file.isDirectory()) throw new IOException("File does not exist or is directory!");

        InputStream inputStream = new FileInputStream(file);

        byte[] headerBytes = new byte[5];
        if (inputStream.read(headerBytes) != 5 || !Arrays.equals(Arrays.copyOfRange(headerBytes, 0, 4), new byte[] { 89, 67, 79, 77 }))
            throw new IOException("Invalid header check!");

        serializers.get(headerBytes[4]).readData(inputStream);

        inputStream.close();
    }

    protected abstract void writeData(OutputStream outputStream) throws IOException;
    protected abstract void readData(InputStream inputStream) throws IOException;

    public File getFile() {
        return file;
    }
}
