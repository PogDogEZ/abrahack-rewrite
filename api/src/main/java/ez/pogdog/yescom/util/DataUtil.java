package ez.pogdog.yescom.util;

import me.iska.jclient.network.packet.types.basic.ShortType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    /**
     * Returns whether a bit is set at an index.
     * @param data The data we are checking
     * @param index The index we are checking
     * @return True if the bit is set, otherwise returns false
     */
    public static boolean hasFlag(int data, int index) {
        return (data & (1 << index)) != 0;
    }

    public static boolean hasFlag(int data) {
        return hasFlag(data, 7);
    }

    public static boolean hasFlag (byte bite) {
        return (bite & (1 << 7)) != 0;
    }

    /**
     * Converts an array of integers to an array of the integers' offsets.
     * The first element retains its original value.
     * Useful for reducing the storage size of large integer value sequences.
     *
     * Example:
     * [9310414, 9310416, 9310418, 9310423] -> [9310414, 2, 2, 5]
     *
     * @param arr The integer array we want to convert
     * @return The array of integer offsets
     */
    public static long[] toOffsets(long[] arr) {
        long[] offsets = new long[arr.length];

        long prev = 0;
        for (int i = 0; arr.length > i; i++) {
            offsets[i] = arr[i] - prev;
            prev = arr[i];
        }

        return offsets;
    }

    /**
     * Shrinks a value to the minimum number of bytes needed to store it, and then read itwithout knowing how many bytes it has.
     * !NOTE! does not support negative numbers
     * @param num The number we want to shrink
     * @return A byte array with the bytes needed to reconstruct the number
     */
    public static byte[] shrinkNum(long num) {
        if (num == 0)
            return new byte[] { 0 };

        byte[] bytes = new byte[8];
        int bytesCount = 0;

        for (int i = 0; num > 0; num >>= 7, i++, bytesCount++)
            bytes[i] = (byte) (((byte) (num & 0x7f) * -1));
        bytes[bytesCount - 1] *= -1;

        return Arrays.copyOf(bytes, bytesCount);
    }

    /**
     * Reconstructs a shrunk number.
     * @param bytes The byte array
     * @return The original number
     */
    public static long unShrinkNum(byte[] bytes) {
        long num = 0;

        for (int i = bytes.length -1; i >= 0; i--) {
            num <<= 7;

            if ((int) bytes[i] >= 0)
                num |= (bytes[i]);
            else
                num |= (bytes[i]) * -1;
        }

        return num;
    }

    /**
     * Returns the number of bytes needed to store shrunk num
     * @param num The number
     * @return How many bytes are needed to store num once it's shrunk
     */
    public static int bytesNeeded(long num) {
        if (num == 0)
            return 1;

        int result = 0;
        for (; num > 0; num >>= 7)
            result++;

        return result;
    }
}
