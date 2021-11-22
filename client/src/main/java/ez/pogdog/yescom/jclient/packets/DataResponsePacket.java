package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Packet.Info(name="data_response", id=YCRegistry.ID_OFFSET + 5, side=Packet.Side.BOTH)
public class DataResponsePacket extends Packet {

    private final List<Object> data = new ArrayList<>();
    private final List<BigInteger> invalidDataIDs = new ArrayList<>();

    private boolean valid;

    public DataResponsePacket(boolean valid, List<Object> data, List<BigInteger> invalidDataIDs) {
        this.valid = valid;
        this.data.addAll(data);
        this.invalidDataIDs.addAll(invalidDataIDs);
    }

    public DataResponsePacket(List<Object> data, List<BigInteger> invalidDataIDs) {
        this(false, data, invalidDataIDs);
    }

    public DataResponsePacket() {
        this(false, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        valid = Registry.BOOLEAN.read(inputStream);

        if (valid) {
            data.clear();

            int dataToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int index = 0; index < dataToRead; ++index); // data.add()
        } else {
            invalidDataIDs.clear();

            int dataIDsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int index = 0; index < dataIDsToRead; ++index) invalidDataIDs.add(Registry.VARINT.read(inputStream));
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BOOLEAN.write(valid, outputStream);

        if (valid) {
            Registry.UNSIGNED_SHORT.write(data.size(), outputStream);
            for (Object object : data); // Registry.OBJECT.write(object, outputStream);
        } else {
            Registry.UNSIGNED_SHORT.write(invalidDataIDs.size(), outputStream);
            for (BigInteger invalidDataID : invalidDataIDs) Registry.VARINT.write(invalidDataID, outputStream);
        }
    }

    public List<Object> getData() {
        return new ArrayList<>(data);
    }

    public void addData(Object object) {
        data.add(object);
    }

    public void setData(List<Object> data) {
        this.data.clear();
        this.data.addAll(data);
    }

    public void addData(List<Object> data) {
        this.data.addAll(data);
    }

    public void removeData(Object object) {
        data.remove(object);
    }

    public List<BigInteger> getInvalidDataIDs() {
        return new ArrayList<>(invalidDataIDs);
    }

    public void addInvalidDataID(BigInteger invalidDataID) {
        invalidDataIDs.add(invalidDataID);
    }

    public void setInvalidDataIDs(List<BigInteger> invalidDataIDs) {
        this.invalidDataIDs.clear();
        this.invalidDataIDs.addAll(invalidDataIDs);
    }

    public void addInvalidDataIDs(List<BigInteger> invalidDataIDs) {
        this.invalidDataIDs.addAll(invalidDataIDs);
    }

    public void removeInvalidDataID(BigInteger invalidDataID) {
        invalidDataIDs.remove(invalidDataID);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
