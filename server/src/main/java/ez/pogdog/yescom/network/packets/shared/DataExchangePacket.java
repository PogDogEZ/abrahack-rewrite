package ez.pogdog.yescom.network.packets.shared;

import ez.pogdog.yescom.data.serializable.ChatMessage;
import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.network.YCRegistry;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Sent by either party to either upload or download YesCom data.
 */
@Packet.Info(name="data_exchange", id=YCRegistry.ID_OFFSET + 3, side=Packet.Side.BOTH)
public class DataExchangePacket extends Packet {

    private final EnumType<RequestType> REQUEST_TYPE = new EnumType<>(RequestType.class);
    private final EnumType<DataType> DATA_TYPE = new EnumType<>(DataType.class);

    private final List<Object> data = new ArrayList<>();
    private final List<BigInteger> invalidDataIDs = new ArrayList<>();
    private final List<BigInteger> dataIDs = new ArrayList<>();

    private RequestType requestType;
    private DataType dataType;

    private int requestID;

    private long startTime;
    private long endTime;
    private int updateInterval;

    private BigInteger maxDataID;
    private BigInteger minDataID;

    public DataExchangePacket(RequestType requestType, DataType dataType, int requestID, List<?> data, List<BigInteger> invalidDataIDs,
                              List<BigInteger> dataIDs, long endTime, long startTime, int updateInterval,
                              BigInteger maxDataID, BigInteger minDataID) {
        this.requestType = requestType;
        this.dataType = dataType;

        this.requestID = requestID;

        this.startTime = startTime;
        this.endTime = endTime;
        this.updateInterval = updateInterval;

        this.maxDataID = maxDataID;
        this.minDataID = minDataID;

        this.data.addAll(data);
        this.invalidDataIDs.addAll(invalidDataIDs);
        this.dataIDs.addAll(dataIDs);
    }

    public DataExchangePacket(DataType dataType, int requestID, List<?> data, List<BigInteger> invalidDataIDs, long startTime,
                              long endTime, int updateInterval) {
        this(RequestType.UPLOAD, dataType, requestID, data, invalidDataIDs, new ArrayList<>(), startTime, endTime, updateInterval,
                BigInteger.ZERO, BigInteger.ZERO);
    }

    public DataExchangePacket(DataType dataType, List<?> data, List<BigInteger> invalidDataIDs) {
        this(RequestType.UPLOAD, dataType, 0, data, invalidDataIDs, new ArrayList<>(), 0L, 0L,
                0, BigInteger.ZERO, BigInteger.ZERO);
    }

    public DataExchangePacket(DataType dataType, int requestID, List<BigInteger> dataIDs, long startTime, long endTime) {
        this(RequestType.DOWNLOAD, dataType, requestID, new ArrayList<>(), new ArrayList<>(), dataIDs, startTime, endTime, 0,
                BigInteger.ZERO, BigInteger.ZERO);
    }

    public DataExchangePacket(DataType dataType, int requestID) {
        this(RequestType.GET_BOUNDS, dataType, requestID, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                0L, 0L, 0, BigInteger.ZERO, BigInteger.ZERO);
    }

    public DataExchangePacket(DataType dataType, long startTime, long endTime, int updateInterval, BigInteger maxDataID,
                              BigInteger minDataID) {
        this(RequestType.SET_BOUNDS, dataType, 0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                startTime, endTime, updateInterval, maxDataID, minDataID);
    }

    public DataExchangePacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        requestType = REQUEST_TYPE.read(inputStream);
        dataType = DATA_TYPE.read(inputStream);

        requestID = Registry.INTEGER.read(inputStream);

        switch (requestType) {
            case DOWNLOAD: {
                switch (dataType) {
                    case TICK_DATA:
                    case PING_DATA:
                    case TSLP_DATA: {
                        startTime = Registry.LONG.read(inputStream);
                        endTime = Registry.LONG.read(inputStream);
                        break;
                    }
                    default: {
                        dataIDs.clear();
                        int IDsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                        for (int index = 0; index < IDsToRead; ++index) dataIDs.add(Registry.VAR_INTEGER.read(inputStream));
                        break;
                    }
                }
                break;
            }
            case UPLOAD: {
                invalidDataIDs.clear();

                int IDsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index = 0; index < IDsToRead; ++index) invalidDataIDs.add(Registry.VAR_INTEGER.read(inputStream));

                data.clear();
                int dataToRead = Registry.UNSIGNED_SHORT.read(inputStream);

                switch (dataType) {
                    case TICK_DATA:
                    case PING_DATA:
                    case TSLP_DATA: {
                        for (int index = 0; index < dataToRead; ++index) data.add(Registry.FLOAT.read(inputStream));

                        startTime = Registry.LONG.read(inputStream);
                        endTime = Registry.LONG.read(inputStream);
                        updateInterval = Registry.UNSIGNED_SHORT.read(inputStream);
                        break;
                    }
                    case ONLINE_PLAYER: {
                        break;
                    }
                    case LOGS: {
                        for (int index = 0; index < dataToRead; ++index) data.add(Registry.STRING.read(inputStream));
                        break;
                    }
                    case CHAT: {
                        for (int index = 0; index < dataToRead; ++index) data.add(YCRegistry.CHAT_MESSAGE.read(inputStream));
                        break;
                    }
                    case CHUNK_STATE: {
                        for (int index = 0; index < dataToRead; ++index) data.add(YCRegistry.CHUNK_STATE.read(inputStream));
                        break;
                    }
                    case RENDER_DISTANCE: {
                        for (int index = 0; index < dataToRead; ++index) data.add(YCRegistry.RENDER_DISTANCE.read(inputStream));
                        break;
                    }
                    case TRACKED_PLAYER: {
                        for (int index = 0; index < dataToRead; ++index) data.add(YCRegistry.TRACKED_PLAYER.read(inputStream));
                        break;
                    }
                    case TRACKING_DATA: {
                        for (int index = 0; index < dataToRead; ++index) data.add(YCRegistry.TRACKING_DATA.read(inputStream));
                        break;
                    }
                }
                break;
            }
            case GET_BOUNDS: {
                break;
            }
            case SET_BOUNDS: {
                switch (dataType) {
                    case TICK_DATA:
                    case PING_DATA:
                    case TSLP_DATA: {
                        startTime = Registry.LONG.read(inputStream);
                        endTime = Registry.LONG.read(inputStream);
                        updateInterval = Registry.UNSIGNED_SHORT.read(inputStream);
                        break;
                    }
                    default: {
                        maxDataID = Registry.VAR_INTEGER.read(inputStream);
                        minDataID = Registry.VAR_INTEGER.read(inputStream);
                        break;
                    }
                }
                break;
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        REQUEST_TYPE.write(requestType, outputStream);
        DATA_TYPE.write(dataType, outputStream);

        Registry.INTEGER.write(requestID, outputStream);

        switch (requestType) {
            case DOWNLOAD: {
                switch (dataType) {
                    case TICK_DATA:
                    case PING_DATA:
                    case TSLP_DATA: {
                        Registry.LONG.write(startTime, outputStream);
                        Registry.LONG.write(endTime, outputStream);
                        break;
                    }
                    default: {
                        Registry.UNSIGNED_SHORT.write(dataIDs.size(), outputStream);
                        for (BigInteger dataID : dataIDs) Registry.VAR_INTEGER.write(dataID, outputStream);
                        break;
                    }
                }
                break;
            }
            case UPLOAD: {
                Registry.UNSIGNED_SHORT.write(invalidDataIDs.size(), outputStream);
                for (BigInteger dataID : invalidDataIDs) Registry.VAR_INTEGER.write(dataID, outputStream);

               Registry.UNSIGNED_SHORT.write(data.size(), outputStream);

               switch (dataType) {
                   case TICK_DATA:
                   case PING_DATA:
                   case TSLP_DATA: {
                       for (Object data : data) Registry.FLOAT.write((Float)data, outputStream);

                       Registry.LONG.write(startTime, outputStream);
                       Registry.LONG.write(endTime, outputStream);
                       Registry.UNSIGNED_SHORT.write(updateInterval, outputStream);
                       break;
                   }
                   case ONLINE_PLAYER: {
                       break;
                   }
                   case LOGS: {
                       for (Object data : data) Registry.STRING.write((String)data, outputStream);
                       break;
                   }
                   case CHAT: {
                       for (Object data : data) YCRegistry.CHAT_MESSAGE.write((ChatMessage)data, outputStream);
                       break;
                   }
                   case CHUNK_STATE: {
                       for (Object data : data) YCRegistry.CHUNK_STATE.write((ChunkState)data, outputStream);
                       break;
                   }
                   case RENDER_DISTANCE: {
                       for (Object data : data) YCRegistry.RENDER_DISTANCE.write((RenderDistance)data, outputStream);
                       break;
                   }
                   case TRACKED_PLAYER: {
                       for (Object data : data) YCRegistry.TRACKED_PLAYER.write((TrackedPlayer)data, outputStream);
                       break;
                   }
                   case TRACKING_DATA: {
                       for (Object data : data) YCRegistry.TRACKING_DATA.write((TrackedPlayer.TrackingData)data, outputStream);
                       break;
                   }
               }
               break;
            }
            case GET_BOUNDS: {
                break;
            }
            case SET_BOUNDS: {
                switch (dataType) {
                    case TICK_DATA:
                    case PING_DATA:
                    case TSLP_DATA: {
                        Registry.LONG.write(startTime, outputStream);
                        Registry.LONG.write(endTime, outputStream);
                        Registry.UNSIGNED_SHORT.write(updateInterval, outputStream);
                        break;
                    }
                    default: {
                        Registry.VAR_INTEGER.write(maxDataID, outputStream);
                        Registry.VAR_INTEGER.write(minDataID, outputStream);
                        break;
                    }
                }
                break;
            }
        }
    }

    /**
     * @return The data that was exchanged.
     */
    public List<Object> getData() {
        return new ArrayList<>(data);
    }

    public void addData(Object data) {
        this.data.add(data);
    }

    public void setData(List<Object> data) {
        this.data.clear();
        this.data.addAll(data);
    }

    public void removeData(Object data) {
        this.data.remove(data);
    }

    /**
     * @return The requested data IDs that were invalid.
     */
    public List<BigInteger> getInvalidDataIDs() {
        return new ArrayList<>(invalidDataIDs);
    }

    public void addInvalidDataID(BigInteger dataID) {
        invalidDataIDs.add(dataID);
    }

    public void setInvalidDataIDs(List<BigInteger> invalidDataIDs) {
        this.invalidDataIDs.clear();
        this.invalidDataIDs.addAll(invalidDataIDs);
    }

    public void addInvalidDataIDs(List<BigInteger> invalidDataIDs) {
        this.invalidDataIDs.addAll(invalidDataIDs);
    }

    public void removeInvalidDataID(BigInteger dataID) {
        invalidDataIDs.remove(dataID);
    }

    /**
     * @return The requested data IDs.
     */
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

    /**
     * @return The type of request being made.
     */
    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    /**
     * @return The type of data being transferred.
     */
    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * @return The unique request ID, -1 if it needs to be assigned by the server.
     */
    public int getRequestID() {
        return requestID;
    }

    public void setRequestID(int requestID) {
        this.requestID = requestID;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public BigInteger getMaxDataID() {
        return maxDataID;
    }

    public void setMaxDataID(BigInteger maxDataID) {
        this.maxDataID = maxDataID;
    }

    public BigInteger getMinDataID() {
        return minDataID;
    }

    public void setMinDataID(BigInteger minDataID) {
        this.minDataID = minDataID;
    }

    public enum RequestType {
        DOWNLOAD, UPLOAD,
        GET_BOUNDS, SET_BOUNDS;
    }

    public enum DataType {
        TICK_DATA, PING_DATA, TSLP_DATA,
        ONLINE_PLAYER,
        LOGS, CHAT,
        CHUNK_STATE, RENDER_DISTANCE, TRACKED_PLAYER, TRACKING_DATA;
    }
}
