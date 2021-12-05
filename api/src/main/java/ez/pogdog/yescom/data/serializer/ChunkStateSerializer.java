package ez.pogdog.yescom.data.serializer;

import ez.pogdog.yescom.data.ISerializable;
import ez.pogdog.yescom.data.ISerializer;
import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public class ChunkStateSerializer implements ISerializer {

    private final static byte[] FILE_HEADER = { 70, 30, 56, 34, 45 };
    private final static int MAX_DISTANCE = 1_875_000;

    private final SortedMap<BigInteger, ChunkState> dataCache = new TreeMap<>();

    private final Logger logger;

    private File currentFile;

    public ChunkStateSerializer(Logger logger) {
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
        SortedMap<BigInteger, ChunkState> temp = new TreeMap<>();

        byte[] elementData = new byte[4];
        inputStream.read(elementData);
        int elementCount = ByteBuffer.wrap(elementData).getInt();

        int bite;
        while ((bite = inputStream.read()) != -1) {
            ByteBuffer buffer = ByteBuffer.allocate(22);
            buffer.put((byte) bite);

            for (int i = 1; 22 > i; i++)
                buffer.put((byte) inputStream.read());

            ChunkState chunkState = deSerialize(buffer);
            temp.put(chunkState.getID(), chunkState);
        }


        dataCache.clear();
        dataCache.putAll(temp);

        logger.info(String.format("Finished reading %s ChunkStates in %sms", elementCount, (System.currentTimeMillis() - startTime)));
    }

    @Override
    public void write() throws IOException {
        OutputStream outputStream = Files.newOutputStream(currentFile.toPath());
        writeHeader(outputStream);

        long startTime = System.currentTimeMillis();

        ByteBuffer infoBytes = ByteBuffer.allocate(4);
        infoBytes.putInt(dataCache.size());
        outputStream.write(infoBytes.array());

        for (ChunkState chunkState : dataCache.values())
            outputStream.write(serialize(chunkState));
        outputStream.close();

        logger.info(String.format("Finished writing %s ChunkStates in %sms", dataCache.size(), (System.currentTimeMillis() - startTime)));
    }

    /**
     * First 8 bytes is the ChunkStateID
     * Next 8 bytes is the foundAt
     * Next 44 bits contain posX and posZ
     * next 2 bits contain dimension
     * Next bit is the state
     */
    private byte[] serialize(ChunkState chunkState) {
        ByteBuffer buffer = ByteBuffer.allocate(22);

        buffer.putLong(chunkState.getID().longValue());
        buffer.putLong(chunkState.getFoundAt());

        long data = compressPos(chunkState.getChunkPosition());
        data <<= 2;
        data |= compressDimension(chunkState.getDimension());
        data <<= 1;
        data |= (chunkState.getState() == ChunkState.State.LOADED ? 1 : 0); // set bit at index 0 if state is loaded
        for (int i = 0; 6 > i; i++, data >>= 8)
            buffer.put((byte) (data & 0xFF));

        return buffer.array();
    }

    private ChunkState deSerialize(ByteBuffer data) {
        long chunkStateID = data.getLong(0);
        long foundAt = data.getLong(8);

        long misc = 0;
        for (int i = 5; i >= 0; i--) {
            misc <<= 8;
            misc |= data.get(16 + i) & 0xFF;
        }

        ChunkState.State state = ((misc & 0x1) != 0 ? ChunkState.State.LOADED : ChunkState.State.UNLOADED);
        misc >>= 1;
        Dimension dimension = uncompressDimension((int)(misc & 0x3));
        misc >>= 2;
        ChunkPosition chunkPosition = uncompressPos(misc);

        return new ChunkState(BigInteger.valueOf(chunkStateID), state, chunkPosition, dimension, foundAt);
    }

    /**
     * We know that the max position value is 1,875,000 (30,000,000 / 16).
     * We also know that 2^22 is 4,194,304, which can contain negative and position of max position.
     * @param chunkPosition The chunk position
     * @return A long with the fist 44 bits containing posX and posZ
     */
    private long compressPos(ChunkPosition chunkPosition) {
        long data = chunkPosition.getX() + MAX_DISTANCE; // prevent negative numbers
        data <<= 22;
        data |= chunkPosition.getZ() + MAX_DISTANCE;

        return data;
    }

    private ChunkPosition uncompressPos(long data) {
        int posZ = (int)(data & 0x3FFFFF) - MAX_DISTANCE;
        data >>= 22;
        int posX = (int)(data & 0x3FFFFF) - MAX_DISTANCE;

        return new ChunkPosition(posX, posZ);
    }

    private int compressDimension(Dimension dimension) {
        return dimension.getMCDim() + 1;
    }

    private Dimension uncompressDimension(int data) {
        if (data == 0)
            return Dimension.NETHER;
        else if (data == 1)
            return Dimension.OVERWORLD;
        else
            return Dimension.END;
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
        return dataCache.get(id);
    }

    @Override
    public boolean has(BigInteger id) {
        return dataCache.containsKey(id);
    }

    @Override
    public void add(ISerializable serializable) {
        if (!(serializable instanceof ChunkState))
            return;

        ChunkState chunkState = (ChunkState) serializable;
        dataCache.put(chunkState.getID(), chunkState);
    }

    @Override
    public void addAll(List<ISerializable> serializables) {
        for (ISerializable serializable : serializables)
            add(serializable);
    }
}
