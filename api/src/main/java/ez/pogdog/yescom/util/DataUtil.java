package ez.pogdog.yescom.util;

import me.iska.jclient.network.packet.types.basic.ShortType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DataUtil {

    public static BigInteger readBigInteger(InputStream inputStream) throws IOException {
        byte[] readLength = new byte[1];
        if (inputStream.read(readLength) != 1) throw new IOException("Unexpected EOF while reading big integer.");
        byte[] bytes = new byte[readLength[0]];
        if (inputStream.read(bytes) != readLength[0]) throw new IOException("Unexpected EOF while reading big integer.");

        BigInteger result = BigInteger.ZERO;
        for (int index = 0; index < bytes.length; ++index)
            result = result.add(BigInteger.valueOf(Byte.toUnsignedInt(bytes[index])).multiply(BigInteger.valueOf(256L).pow(index)));

        return result;
    }

    public static void writeBigInteger(OutputStream outputStream, BigInteger bigInteger) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();

        do {
            bytesOut.write(new byte[] { (byte)(bigInteger.mod(BigInteger.valueOf(256)).intValue()) });
            bigInteger = bigInteger.divide(BigInteger.valueOf(256));
        } while (bigInteger.compareTo(BigInteger.valueOf(256)) > 0);

        bytesOut.write(new byte[] { (byte)(bigInteger.mod(BigInteger.valueOf(256)).intValue()) });

        outputStream.write(new byte[] { (byte)bytesOut.size() });
        outputStream.write(bytesOut.toByteArray());
    }

    public static String readString(InputStream inputStream) throws IOException {
        byte[] lengthBytes = new byte[2];
        if (inputStream.read(lengthBytes) != 2) throw new IOException("Unexpected EOF while reading string.");
        int length = ByteBuffer.wrap(lengthBytes).getShort();
        byte[] raw = new byte[length];
        if (inputStream.read(raw) != length) throw new IOException("Unexpected EOF while reading string.");
        return new String(raw, StandardCharsets.UTF_8);
    }

    public static void writeString(OutputStream outputStream, String string) throws IOException {
        byte[] raw = string.getBytes(StandardCharsets.UTF_8);
        new ShortType.Unsigned().write(raw.length, outputStream);
        outputStream.write(raw);
    }
}
