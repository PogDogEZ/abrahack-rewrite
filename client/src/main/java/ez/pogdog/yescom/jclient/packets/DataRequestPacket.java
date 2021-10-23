package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Packet.Info(name="data_request", id=YCRegistry.ID_OFFSET + 4, side=Packet.Side.BOTH)
public class DataRequestPacket extends Packet {

    private final EnumType<RequestType> REQUEST_TYPE = new EnumType<>(RequestType.class);
    private final EnumType<DataType> DATA_TYPE = new EnumType<>(DataType.class);

    private final List<BigInteger> dataIDs = new ArrayList<>();

    private RequestType requestType;
    private DataType dataType;

    private int chunkSize;
    private int expectedParts;

    public DataRequestPacket(RequestType requestType, DataType dataType, List<BigInteger> dataIDs, int chunkSize, int expectedParts) {
        this.requestType = requestType;
        this.dataType = dataType;

        this.dataIDs.addAll(dataIDs);

        this.chunkSize = chunkSize;
        this.expectedParts = expectedParts;
    }

    public DataRequestPacket(DataType dataType, List<BigInteger> dataIDs) {
        this(RequestType.DOWNLOAD, dataType, dataIDs, 65536, 0);
    }

    public DataRequestPacket(int chunkSize, int expectedParts) {
        this(RequestType.UPLOAD, DataType.CHUNK_STATE, new ArrayList<>(), chunkSize, expectedParts);
    }

    public DataRequestPacket(RequestType requestType) {
        this(requestType, DataType.CHUNK_STATE, new ArrayList<>(), 65536, 0);
    }

    public DataRequestPacket() {
        this(RequestType.DOWNLOAD, DataType.CHUNK_STATE, new ArrayList<>(), 65536, 0);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        requestType = REQUEST_TYPE.read(inputStream);

        switch (requestType) {
            case DOWNLOAD: {
                dataType = DATA_TYPE.read(inputStream);
                dataIDs.clear();

                int IDsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index = 0; index < IDsToRead; ++index) dataIDs.add(Registry.VARINT.read(inputStream));
                break;
            }
            case UPLOAD: {
                chunkSize = Registry.INT.read(inputStream);
                expectedParts = Registry.UNSIGNED_SHORT.read(inputStream);
                break;
            }
            case CANCEL: {
                break;
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        REQUEST_TYPE.write(requestType, outputStream);

        switch (requestType) {
            case DOWNLOAD: {
                DATA_TYPE.write(dataType, outputStream);
                Registry.UNSIGNED_SHORT.write(dataIDs.size(), outputStream);
                for (BigInteger dataID : dataIDs) Registry.VARINT.write(dataID, outputStream);
                break;
            }
            case UPLOAD: {
                Registry.INT.write(chunkSize, outputStream);
                Registry.UNSIGNED_SHORT.write(expectedParts, outputStream);
                break;
            }
            case CANCEL: {
                break;
            }
        }
    }

    public List<BigInteger> getDataIDs() {
        return new ArrayList<>(dataIDs);
    }

    public void addDataID(BigInteger dataID) {
        dataIDs.add(dataID);
    }

    public void setDataIDs(List<BigInteger> dataIDs) {
        this.dataIDs.clear();
        this.dataIDs.addAll(dataIDs);
    }

    public void addDataIDs(List<BigInteger> dataIDs) {
        this.dataIDs.addAll(dataIDs);
    }

    public void removeDataID(BigInteger dataID) {
        dataIDs.remove(dataID);
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public enum RequestType {
        DOWNLOAD, UPLOAD, CANCEL;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getExpectedParts() {
        return expectedParts;
    }

    public void setExpectedParts(int expectedParts) {
        this.expectedParts = expectedParts;
    }

    public enum DataType {
        CHUNK_STATE, RENDER_DISTANCE, TRACKED_PLAYER, ONLINE_PLAYER;
    }
}
