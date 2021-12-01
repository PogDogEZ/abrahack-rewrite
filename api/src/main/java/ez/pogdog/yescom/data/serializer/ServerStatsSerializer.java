package ez.pogdog.yescom.data.serializer;

import ez.pogdog.yescom.data.ISerializable;
import ez.pogdog.yescom.data.ISerializer;
import ez.pogdog.yescom.data.serializable.ServerStats;
import ez.pogdog.yescom.logging.Logger;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;

public class ServerStatsSerializer implements ISerializer {

    private final static byte[] FILE_HEADER = { 42, 112, 23, 22, 93 };
    private final static float MIN_FLOAT_DIV = 100;

    private final SortedMap<Long, ServerStats> dataCache = new TreeMap<>();

    private final Logger logger;

    private File currentFile;

    public ServerStatsSerializer(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void setFile(File file) {
        if(!file.equals(currentFile)) {
            currentFile = file;
        }
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void read() throws IOException {
        if (!currentFile.exists())
            throw new IOException("Current file does not exist or is not a directory.");

        InputStream inputStream = Files.newInputStream(currentFile.toPath());
        checkHeader(inputStream);

        long startTime = System.currentTimeMillis();
        SortedMap<Long, ServerStats> temp = new TreeMap<>();

        byte[] timeData = new byte[8];
        inputStream.read(timeData);
        long time = ByteBuffer.wrap(timeData).getLong();

        byte[] elementData = new byte[4];
        inputStream.read(elementData);
        int elementCount = ByteBuffer.wrap(elementData).getInt();

        int bite;
        while ((bite = inputStream.read()) != -1) {
            long timeOffset;
            if ((bite & (1 << 7)) != 0) { // time flag
                timeOffset = ByteBuffer.wrap(new byte[] { (byte) bite, (byte) inputStream.read(),
                        (byte) inputStream.read(), (byte) inputStream.read()}).getInt() * -1;
            }
            else {
                timeOffset = ByteBuffer.wrap(new byte[] { (byte) bite, (byte) inputStream.read()}).getShort();
            }
            timeOffset *= 1000;
            time += timeOffset;

            byte[] data = new byte[6];
            inputStream.read(data);
            temp.put(time, deserialize(ByteBuffer.wrap(data), time));
        }

        dataCache.clear();
        dataCache.putAll(temp);

        logger.info(String.format("Finished reading %s server statistics in %sms", elementCount, (System.currentTimeMillis() - startTime)));
    }

    @Override
    public void write() throws IOException {
        OutputStream outputStream = Files.newOutputStream(currentFile.toPath());
        writeHeader(outputStream);

        long startTime = System.currentTimeMillis();

        ByteBuffer infoBytes = ByteBuffer.allocate(12);
        infoBytes.putLong(dataCache.firstKey());
        infoBytes.putInt(dataCache.size());
        outputStream.write(infoBytes.array());

        long prevTime = 0;
        boolean first = true;

        for (Map.Entry<Long, ServerStats> entry : dataCache.entrySet()) {
            outputStream.write(serialize(entry.getValue(), prevTime, first));

            if (first) first = false;
            prevTime = entry.getKey();
        }
        outputStream.close();

        logger.info(String.format("Finished writing %s server statistics in %sms", dataCache.size(), (System.currentTimeMillis() - startTime)));
    }

    private byte[] serialize(ServerStats data, long prevTime, boolean first) {
        boolean timeFlag = (data.getTime() - prevTime) / 1000 > 32767 && !first;
        ByteBuffer buffer = ByteBuffer.allocate(6 + (timeFlag ? 4 : 2));

        if (first)
            buffer.putShort((short)0); // No time difference
        else {
            if (timeFlag) {
                int difference = (int)((data.getTime() - prevTime) / 1000);
                difference *= -1; // Set the sign bit to 1, used as a flag when we're reading
                buffer.putInt(difference);
            }
            else {
                short difference = (short)((data.getTime() - prevTime) / 1000);
                buffer.putShort(difference);
            }
        }

        buffer.putShort(getMinFloat(data.getTPS()));
        buffer.putShort(getMinFloat(data.getPING()));
        buffer.putShort((short)data.getTSLP());

        return buffer.array();
    }

    private ServerStats deserialize(ByteBuffer data, long time) {
        return new ServerStats(
                time,
                getNormalFloat(data.getShort(0)),
                getNormalFloat(data.getShort(2)),
                data.getShort(4));
    }

    private short getMinFloat(float input) {
        return (short) (input * MIN_FLOAT_DIV);
    }

    private float getNormalFloat(short minFloat) {
        return minFloat / MIN_FLOAT_DIV;
    }

    private void checkHeader(InputStream inputStream) throws IOException {
        byte[] headerBytes = new byte[5];
        if (inputStream.read(headerBytes) != 5)
            throw new IOException("Unexpected EOF while reading header.");
        if (!Arrays.equals(headerBytes, FILE_HEADER))
            throw new IOException("Invalid header check.");
    }

    private void writeHeader(OutputStream outputStream) throws IOException {
        outputStream.write(FILE_HEADER);
    }

    @Override
    public List<ISerializable> get() {
        return new ArrayList<>(dataCache.values());
    }

    @Override
    public ISerializable get(BigInteger id) {
        return null;
    }

    @Override
    public boolean has(BigInteger id) {
        return false;
    }

    @Override
    public void add(ISerializable serializable) {
        if (!(serializable instanceof ServerStats))
            return;

        ServerStats serverStats = (ServerStats) serializable;
        dataCache.put(serverStats.getTime(), serverStats);
    }

    @Override
    public void addAll(List<ISerializable> serializables) {
        for (ISerializable serializable : serializables)
            add(serializable);
    }
}
