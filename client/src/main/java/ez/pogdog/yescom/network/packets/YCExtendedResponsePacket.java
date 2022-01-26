package ez.pogdog.yescom.network.packets;

import ez.pogdog.yescom.network.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="yc_extended_response", id=YCRegistry.ID_OFFSET + 2, side=Packet.Side.CLIENT)
public class YCExtendedResponsePacket extends Packet {

    private byte[] identityProofNonce;

    public YCExtendedResponsePacket(byte[] identityProofNonce) {
        this.identityProofNonce = identityProofNonce.clone();
    }

    public YCExtendedResponsePacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        identityProofNonce = Registry.BYTES.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BYTES.write(identityProofNonce, outputStream);
    }

    public byte[] getIdentityProofNonce() {
        return identityProofNonce;
    }

    public void setIdentityProofNonce(byte[] identityProofNonce) {
        this.identityProofNonce = identityProofNonce.clone();
    }
}
