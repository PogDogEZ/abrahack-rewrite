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

    private final List<BigInteger> invalidDataIDs = new ArrayList<>();

    private boolean valid;
    private int chunkSize;
    private int expectedParts;

    public DataResponsePacket(boolean valid, int chunkSize, int expectedParts, List<BigInteger> invalidDataIDs) {
        this.valid = valid;
        this.chunkSize = chunkSize;
        this.expectedParts = expectedParts;

        this.invalidDataIDs.addAll(invalidDataIDs);
    }

    public DataResponsePacket(int chunkSize, int expectedParts) {
        this(true, chunkSize, expectedParts, new ArrayList<>());
    }

    public DataResponsePacket(List<BigInteger> invalidDataIDs) {
        this(false, 65536, 0, invalidDataIDs);
    }

    public DataResponsePacket() {
        this(false, 65536, 0, new ArrayList<>());
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        valid = Registry.BOOLEAN.read(inputStream);

        if (valid) {
            chunkSize = Registry.INT.read(inputStream);
            expectedParts = Registry.UNSIGNED_SHORT.read(inputStream);
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
            Registry.INT.write(chunkSize, outputStream);
            Registry.UNSIGNED_SHORT.write(chunkSize, outputStream);
        } else {
            Registry.UNSIGNED_SHORT.write(invalidDataIDs.size(), outputStream);
            for (BigInteger invalidDataID : invalidDataIDs) Registry.VARINT.write(invalidDataID, outputStream);
        }
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
}
