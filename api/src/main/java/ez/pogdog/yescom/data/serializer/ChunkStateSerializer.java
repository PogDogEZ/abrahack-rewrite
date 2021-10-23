package ez.pogdog.yescom.data.serializer;

import ez.pogdog.yescom.data.ISerializable;
import ez.pogdog.yescom.data.ISerializer;
import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.logging.Logger;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.DataUtil;
import ez.pogdog.yescom.util.Dimension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ChunkStateSerializer implements ISerializer {

    private final SortedMap<BigInteger, ChunkState> dataCache = new TreeMap<>();
    private final List<BigInteger> dirty = new ArrayList<>();

    private final Logger logger;

    private File currentFile;
    private IndexFile indexFile;

    public ChunkStateSerializer(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void setFile(File file) {
        if (!file.equals(currentFile)) {
            dataCache.clear();
            currentFile = file;
            indexFile = null;
        }
    }

    @Override
    public void read() throws IOException {
        if (!currentFile.exists() || !currentFile.isDirectory()) throw new IOException("Current file does not exist or is not a directory.");

        indexFile = new IndexFile(Paths.get(currentFile.getPath(), "cstate-index.ycom").toFile());
        indexFile.read();

        dataCache.clear();
    }

    @Override
    public void write() throws IOException {
        if ((!currentFile.exists() || !currentFile.isDirectory()) && !currentFile.mkdir())
            throw new IOException("Couldn't create new directory due to unknown.");

        indexFile = new IndexFile(Paths.get(currentFile.getPath(), "cstate-index.ycom").toFile());
        try {
            indexFile.read();
        } catch (IOException ignored) { // We haven't written an index file yet
        }

        int modifiedStates = 0;
        int newStates = 0;

        for (BigInteger chunkStateID : dirty) {
            if (indexFile.getMaxChunkStateID().compareTo(chunkStateID) < 0) {
                ++modifiedStates;
            } else {
                ++newStates;
            }
        }

        logger.debug(String.format("Writing %d modified states, %d new states.", modifiedStates, newStates));

        if (newStates > 0) {
            logger.debug("Readjust index params due to new states...");

            indexFile.setTotalStates(indexFile.getTotalStates() + newStates);
            indexFile.setMaxChunkStateID(dataCache.lastKey()); // We have new states so adjust the max ID
        }

        logger.debug("Recalculating ID offsets...");
        indexFile.recalculateIDOffsets();

        /*
        BigInteger IDOffset = dataCache.firstKey();
        long timestampOffest = dataCache.get(IDOffset).getFoundAt();

        for (Map.Entry<BigInteger, ChunkState> entry : dataCache.entrySet())
            writeChunkState(outputStream, entry.getValue(), IDOffset, timestampOffest);

        outputStream.close();
         */
    }

    @Override
    public List<ISerializable> get() {
        return new ArrayList<>(dataCache.values());
    }

    @Override
    public ISerializable get(BigInteger id) {
        if (!dataCache.containsKey(id)) {
            try {
                readSpecific(id);
            } catch (IOException ignored) {
            }
        }
        return dataCache.get(id);
    }

    @Override
    public boolean has(BigInteger id) {
        return dataCache.containsKey(id);
    }

    @Override
    public void add(ISerializable serializable) {
        if (serializable instanceof ChunkState) {
            dataCache.put(serializable.getID(), (ChunkState)serializable);
            if (!dirty.contains(serializable.getID())) dirty.add(serializable.getID());
        }
    }

    @Override
    public void addAll(List<ISerializable> serializables) {
        for (ISerializable serializable : serializables) {
            if (serializable instanceof ChunkState) {
                dataCache.put(serializable.getID(), (ChunkState)serializable);
                if (!dirty.contains(serializable.getID())) dirty.add(serializable.getID());
            }
        }
    }

    private void readSpecific(BigInteger chunkStateID) throws IOException {
        if (indexFile == null) throw new IOException("No index file.");

        Map<BigInteger, ChunkState> chunkStates = new HashMap<>();
        Optional<BigInteger> minID = indexFile.getIDOffsets().keySet().stream()
                .filter(id -> id.compareTo(chunkStateID) >= 0)
                .min(Comparator.comparing(BigInteger::abs));

        if (minID.isPresent()) {
            InputStream inputStream = Files.newInputStream(indexFile.getIDOffsets().get(minID.get()).toPath());
            checkHeader(inputStream);

            byte[] infoBytes = new byte[12];
            if (inputStream.read(infoBytes) != infoBytes.length) throw new IOException("Unexpected EOF while reading file.");
            ByteBuffer infoByteBuffer = ByteBuffer.wrap(infoBytes);

            int statesToRead = infoByteBuffer.getInt();
            long timestampOffset = infoByteBuffer.getLong();
            BigInteger IDOffset = DataUtil.readBigInteger(inputStream);

            for (int index = 0; index < statesToRead; ++index) {
                ChunkState chunkState = readChunkState(inputStream, IDOffset, timestampOffset);
                chunkStates.put(chunkState.getID(), chunkState);
            }

            inputStream.close();
        }

        dataCache.putAll(chunkStates);
    }

    private void checkHeader(InputStream inputStream) throws IOException {
        byte[] headerBytes = new byte[5];
        if (inputStream.read(headerBytes) != 5) throw new IOException("Unexpected EOF while reading header.");
        if (!Arrays.equals(headerBytes, new byte[] { 121, 99, 111, 109, 0 })) throw new IOException("Invalid header check.");
    }

    private void writeHeader(OutputStream outputStream) throws IOException {
        outputStream.write(new byte[] { 121, 99, 111, 109, 0 });
    }

    private ChunkState readChunkState(InputStream inputStream, BigInteger IDOffset, long timestampOffset) throws IOException {
        BigInteger chunkStateID = IDOffset.add(DataUtil.readBigInteger(inputStream));

        byte[] data = new byte[18];
        if (inputStream.read(data) != data.length) throw new IOException("Unexpected EOF while reading chunk state.");
        ByteBuffer dataByteBuffer = ByteBuffer.wrap(data);

        ChunkState.State state = ChunkState.State.values()[dataByteBuffer.get()];
        ChunkPosition chunkPosition = new ChunkPosition(dataByteBuffer.getInt(), dataByteBuffer.getInt());
        Dimension dimension = Dimension.fromMC(dataByteBuffer.get());
        long foundAt = timestampOffset + (long)dataByteBuffer.getInt();

        return new ChunkState(chunkStateID, state, chunkPosition, dimension, foundAt);
    }

    private void writeChunkState(OutputStream outputStream, ChunkState chunkState, BigInteger IDOffset, long timestampOffset) throws IOException {
        DataUtil.writeBigInteger(outputStream, chunkState.getID().subtract(IDOffset));

        ByteBuffer dataByteBuffer = ByteBuffer.allocate(18);

        dataByteBuffer.put((byte)chunkState.getState().ordinal());
        dataByteBuffer.putInt(chunkState.getChunkPosition().getX());
        dataByteBuffer.putInt(chunkState.getChunkPosition().getZ());
        dataByteBuffer.put((byte)chunkState.getDimension().getMCDim());
        dataByteBuffer.putInt((int)(chunkState.getFoundAt() - timestampOffset));

        outputStream.write(dataByteBuffer.array());
    }

    private class IndexFile {

        private final Map<BigInteger, File> IDOffsets = new HashMap<>();

        private final File file;

        private int maxStatesPerFile;
        private int totalStates;

        private BigInteger maxChunkStateID;
        private BigInteger minChunkStateID;

        public IndexFile(File file) {
            this.file = file;

            maxStatesPerFile = 65536;
            totalStates = 0;

            maxChunkStateID = BigInteger.ZERO;
            minChunkStateID = BigInteger.ZERO;
        }

        public void read() throws IOException {
            if (!file.exists() || file.isDirectory()) throw new IOException("Index file does not exist or is a directory.");

            InputStream inputStream = Files.newInputStream(file.toPath());
            checkHeader(inputStream);

            byte[] infoBytes = new byte[12];
            if (inputStream.read(infoBytes) != infoBytes.length) throw new IOException("Unexpected EOF while reading info.");
            ByteBuffer infoByteBuffer = ByteBuffer.wrap(infoBytes);

            maxStatesPerFile = infoByteBuffer.getInt();
            totalStates = infoByteBuffer.getInt();

            int filesToRead = infoByteBuffer.getInt();

            maxChunkStateID = DataUtil.readBigInteger(inputStream);
            minChunkStateID = DataUtil.readBigInteger(inputStream);

            IDOffsets.clear();
            for (int index = 0; index < filesToRead; ++index)
                IDOffsets.put(DataUtil.readBigInteger(inputStream), Paths.get(DataUtil.readString(inputStream)).toFile());

            inputStream.close();
        }

        public void write() throws IOException {
            if (!file.exists() && !file.createNewFile()) throw new IOException("Couldn't write index file due to unknown.");

            OutputStream outputStream = Files.newOutputStream(file.toPath());
            writeHeader(outputStream);

            ByteBuffer infoByteBuffer = ByteBuffer.allocate(12);

            infoByteBuffer.putInt(maxStatesPerFile);
            infoByteBuffer.putInt(totalStates);
            infoByteBuffer.putInt((int)Math.ceil(totalStates / (double)maxStatesPerFile));

            outputStream.write(infoByteBuffer.array());

            DataUtil.writeBigInteger(outputStream, maxChunkStateID);
            DataUtil.writeBigInteger(outputStream, minChunkStateID);

            for (Map.Entry<BigInteger, File> entry : IDOffsets.entrySet()) {
                DataUtil.writeBigInteger(outputStream, entry.getKey());
                DataUtil.writeString(outputStream, entry.getValue().getPath());
            }

            outputStream.close();
        }

        public void recalculateIDOffsets() {
        }

        public Map<BigInteger, File> getIDOffsets() {
            return new HashMap<>(IDOffsets);
        }

        public int getMaxStatesPerFile() {
            return maxStatesPerFile;
        }

        public void setMaxStatesPerFile(int maxStatesPerFile) {
            this.maxStatesPerFile = maxStatesPerFile;
        }

        public int getTotalStates() {
            return totalStates;
        }

        public void setTotalStates(int totalStates) {
            this.totalStates = totalStates;
        }

        public BigInteger getMaxChunkStateID() {
            return maxChunkStateID;
        }

        public void setMaxChunkStateID(BigInteger maxChunkStateID) {
            this.maxChunkStateID = maxChunkStateID;
        }

        public BigInteger getMinChunkStateID() {
            return minChunkStateID;
        }

        public void setMinChunkStateID(BigInteger minChunkStateID) {
            this.minChunkStateID = minChunkStateID;
        }
    }
}
