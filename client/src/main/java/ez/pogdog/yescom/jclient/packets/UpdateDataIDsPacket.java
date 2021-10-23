package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

@Packet.Info(name="update_data_ids", id=YCRegistry.ID_OFFSET + 3, side=Packet.Side.BOTH)
public class UpdateDataIDsPacket extends Packet {

    private final EnumType<DataType> DATA_TYPE = new EnumType<>(DataType.class);

    private DataType dataType;
    private BigInteger dataIDMax;
    private BigInteger dataIDMin;

    public UpdateDataIDsPacket(DataType dataType, BigInteger dataIDMax, BigInteger dataIDMin) {
        this.dataType = dataType;
        this.dataIDMax = dataIDMax;
        this.dataIDMin = dataIDMin;
    }

    public UpdateDataIDsPacket() {
        this(DataType.CHUNK_STATE, BigInteger.ZERO, BigInteger.ZERO);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        dataType = DATA_TYPE.read(inputStream);
        dataIDMax = Registry.VARINT.read(inputStream);
        dataIDMin = Registry.VARINT.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        DATA_TYPE.write(dataType, outputStream);
        Registry.VARINT.write(dataIDMax, outputStream);
        Registry.VARINT.write(dataIDMin, outputStream);
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public BigInteger getDataIDMax() {
        return dataIDMax;
    }

    public void setDataIDMax(BigInteger dataIDMax) {
        this.dataIDMax = dataIDMax;
    }

    public BigInteger getDataIDMin() {
        return dataIDMin;
    }

    public void setDataIDMin(BigInteger dataIDMin) {
        this.dataIDMin = dataIDMin;
    }

    public enum DataType {
        CHUNK_STATE, RENDER_DISTANCE, TRACKED_PLAYER, ONLINE_PLAYER;
    }
}
