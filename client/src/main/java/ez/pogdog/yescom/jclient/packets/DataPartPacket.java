package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="data_part", id=YCRegistry.ID_OFFSET + 6, side=Packet.Side.BOTH)
public class DataPartPacket extends Packet {

    private int dataPart;
    private byte[] data;

    public DataPartPacket(int dataPart, byte[] data) {
        this.dataPart = dataPart;
        this.data = data.clone();
    }

    public DataPartPacket() {
        this(0, new byte[0]);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        dataPart = Registry.UNSIGNED_SHORT.read(inputStream);
        data = Registry.BYTES.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(dataPart, outputStream);
        Registry.BYTES.write(data, outputStream);
    }

    public int getDataPart() {
        return dataPart;
    }

    public void setDataPart(int dataPart) {
        this.dataPart = dataPart;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
