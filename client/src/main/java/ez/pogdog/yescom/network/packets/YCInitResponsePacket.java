package ez.pogdog.yescom.network.packets;

import ez.pogdog.yescom.network.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="yc_init_response", id=YCRegistry.ID_OFFSET + 1, side=Packet.Side.SERVER)
public class YCInitResponsePacket extends Packet {

    private boolean extendedInit;
    private byte[] signature;

    private boolean rejected;
    private int handlerID;
    private String message;

    public YCInitResponsePacket(boolean extendedInit, byte[] signature, boolean rejected, int handlerID, String message) {
        this.extendedInit = extendedInit;
        this.signature = signature.clone();
        this.rejected = rejected;
        this.handlerID = handlerID;
        this.message = message;
    }

    public YCInitResponsePacket(boolean rejected, int handlerID, String message) {
        this(false, new byte[0], rejected, handlerID, message);
    }

    public YCInitResponsePacket(byte[] signature) {
        this(true, signature, false, 0, "");
    }

    public YCInitResponsePacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        extendedInit = Registry.BOOLEAN.read(inputStream);

        if (extendedInit) {
            signature = Registry.BYTES.read(inputStream);
        } else {
            rejected = Registry.BOOLEAN.read(inputStream);
            message = Registry.STRING.read(inputStream);

            if (!rejected) handlerID = Registry.UNSIGNED_SHORT.read(inputStream);
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BOOLEAN.write(extendedInit, outputStream);

        if (extendedInit) {
            Registry.BYTES.write(signature, outputStream);
        } else {
            Registry.BOOLEAN.write(rejected, outputStream);
            Registry.STRING.write(message, outputStream);

            if (!rejected) Registry.UNSIGNED_SHORT.write(handlerID, outputStream);
        }
    }

    public boolean isExtendedInit() {
        return extendedInit;
    }

    public void setExtendedInit(boolean extendedInit) {
        this.extendedInit = extendedInit;
    }

    public byte[] getSignature() {
        return signature.clone();
    }

    public void setSignature(byte[] signature) {
        this.signature = signature.clone();
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public int getHandlerID() {
        return handlerID;
    }

    public void setHandlerID(int handlerID) {
        this.handlerID = handlerID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
