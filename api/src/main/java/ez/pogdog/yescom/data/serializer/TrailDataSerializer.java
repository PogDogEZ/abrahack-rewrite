package ez.pogdog.yescom.data.serializer;

import ez.pogdog.yescom.data.ISerializable;
import ez.pogdog.yescom.data.ISerializer;
import ez.pogdog.yescom.data.serializable.TrailData;
import ez.pogdog.yescom.util.DataUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public class TrailDataSerializer implements ISerializer {

    private final static byte[] FILE_HEADER = { 9, 118, 70, 14, 99 };

    private final HashSet<TrailData> dataCache = new HashSet<>();

    private final Logger logger;

    private File currentFile;

    public TrailDataSerializer(Logger logger) {
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
            throw new IOException("Current file does not exist.");

        InputStream inputStream = Files.newInputStream(currentFile.toPath());
        checkHeader(inputStream);

        long startTime = System.currentTimeMillis();
        HashSet<TrailData> temp = new HashSet<>();

        byte[] elementCountData = new byte[4];
        inputStream.read(elementCountData);
        int elementCount = ByteBuffer.wrap(elementCountData).getInt();

        int bite, dataSize;
        while ((bite = inputStream.read()) != -1) {
            if (DataUtil.hasFlag(bite)) { // size flag
                dataSize = ByteBuffer.wrap(new byte[]{(byte) bite, (byte) inputStream.read(),
                        (byte) inputStream.read(), (byte) inputStream.read()}).getInt() * -1;
            }
            else {
                dataSize = ByteBuffer.wrap(new byte[]{(byte) bite, (byte) inputStream.read()}).getShort();
            }

            byte[] data = new byte[8 + dataSize];
            inputStream.read(data);
            temp.add(deserialize(ByteBuffer.wrap(data), dataSize));
        }

        dataCache.clear();
        dataCache.addAll(temp);

        logger.info(String.format("Finished reading %s trail data records in %sms", elementCount, (System.currentTimeMillis() - startTime)));
    }

    @Override
    public void write() throws IOException {
        OutputStream outputStream = Files.newOutputStream(currentFile.toPath());
        writeHeader(outputStream);

        long startTime = System.currentTimeMillis();

        ByteBuffer infoBytes = ByteBuffer.allocate(4);
        infoBytes.putInt(dataCache.size());
        outputStream.write(infoBytes.array());

        for (TrailData data : dataCache) {
            if (data.isEmpty())
                continue;

            outputStream.write(serialize(data));
        }
        outputStream.close();

        logger.info(String.format("Finished writing %s trail data records in %sms", dataCache.size(), (System.currentTimeMillis() - startTime)));
    }

    /**
     * First 2-4 bytes are the trail size
     * Next 8 bytes are the trail ID
     * Next (trail size) * (x) bytes is the render distance id differences
     * @param data The trail data
     * @return A byte array containing the serialized TrailData
     */
    private byte[] serialize(TrailData data) {
        long[] shortenedIds = DataUtil.toOffsets(data.getRenderDistanceIds());

        int idsSize = 0;
        for (long num : shortenedIds)
            idsSize += DataUtil.bytesNeeded(num);

        boolean sizeFlag = idsSize > Short.MAX_VALUE;
        ByteBuffer buffer = ByteBuffer.allocate((sizeFlag ? 4 : 2) + 8 + idsSize);

        if (sizeFlag)
            buffer.putInt(idsSize * -1); // setting the sign bit to tell the reader this is an int
        else
            buffer.putShort((short) idsSize);

        buffer.putLong(data.getTrailID());

        for (long id : shortenedIds)
            buffer.put(DataUtil.shrinkNum(id));

        return buffer.array();
    }

    /**
     * Deserializes data,
     * @param data The byte buffer with the data
     * @param idSize The number of bytes the id data takes up
     * @return A reconstructed TrailData object
     */
    private TrailData deserialize(ByteBuffer data, int idSize) {
        TrailData trailData = new TrailData(data.getLong(0));

        boolean errorFlag = false; // this will tell us if anything goes wrong, so we can deal with it later

        byte[] elements = Arrays.copyOfRange(data.array(), 8, idSize + 8);

        long renderDistanceID = 0;
        for (int i = 0; elements.length > i;) {
            int prev = i;
            boolean flag = false;
            for (; !flag; i++) // Search for byte with flag (end of current number buffer)
                flag = elements[i] > 0 || (i == 0 && elements[i] == 0);

            renderDistanceID += DataUtil.unShrinkNum(Arrays.copyOfRange(elements, prev, i));

            if(!trailData.addRenderDistance(renderDistanceID)) {
                logger.warning("Read invalid RenderDistance ID!");
                errorFlag = true;
            }
        }

        if (errorFlag) {
            logger.info("Error reading TrailData for tracker ID " + trailData.getTrailID() + "!");
            //TODO maybe put the data in an invalid data directory, so we can try recovering it later
        }
        return trailData;
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
        return new ArrayList<>(dataCache);
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
        if (!(serializable instanceof TrailData))
            return;

        dataCache.add((TrailData) serializable);
    }

    @Override
    public void addAll(List<ISerializable> serializables) {
        for (ISerializable serializable : serializables)
            add(serializable);
    }
}
