package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.data.serializable.ChunkState;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Packet.Info(name="chunk_states", id=YCRegistry.ID_OFFSET + 13, side=Packet.Side.CLIENT)
public class ChunkStatesPacket extends Packet {

    private final List<ChunkState> chunkStates = new ArrayList<>();

    public ChunkStatesPacket(List<ChunkState> chunkStates) {
        this.chunkStates.addAll(chunkStates);
    }

    public ChunkStatesPacket() {
        this(new ArrayList<>());
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        chunkStates.clear();

        int chunkStatesToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < chunkStatesToRead; ++index) chunkStates.add(YCRegistry.CHUNK_STATE.read(inputStream));
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(chunkStates.size(), outputStream);
        for (ChunkState chunkState : chunkStates) YCRegistry.CHUNK_STATE.write(chunkState, outputStream);
    }

    public List<ChunkState> getChunkStates() {
        return new ArrayList<>(chunkStates);
    }

    public void addChunkState(ChunkState chunkState) {
        chunkStates.add(chunkState);
    }

    public void setChunkStates(List<ChunkState> chunkStates) {
        this.chunkStates.clear();
        this.chunkStates.addAll(chunkStates);
    }

    public void addChunkStates(List<ChunkState> chunkStates) {
        this.chunkStates.addAll(chunkStates);
    }

    public void removeChunkState(ChunkState chunkState) {
        chunkStates.remove(chunkState);
    }
}
