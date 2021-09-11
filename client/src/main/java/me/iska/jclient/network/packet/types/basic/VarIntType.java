package me.iska.jclient.network.packet.types.basic;

import me.iska.jclient.network.packet.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public class VarIntType extends Type<BigInteger> {

    @Override
    public BigInteger read(InputStream inputStream) throws IOException {
        byte[] readLength = new byte[1];
        inputStream.read(readLength);
        byte[] bytes = new byte[readLength[0]];
        inputStream.read(bytes);

        BigInteger result = BigInteger.ZERO;
        for (int index = 0; index < bytes.length; ++index)
            result = result.add(BigInteger.valueOf(Byte.toUnsignedInt(bytes[index]) * (long)(Math.pow(256, index))));

        return result;
    }

    @Override
    public void write(BigInteger value, OutputStream outputStream) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();

        do {
            bytesOut.write(new byte[] { (byte)(value.mod(BigInteger.valueOf(256)).intValue()) });
            value = value.divide(BigInteger.valueOf(256));
        } while (value.compareTo(BigInteger.valueOf(256)) > 0);

        bytesOut.write(new byte[] { (byte)(value.mod(BigInteger.valueOf(256)).intValue()) });

        outputStream.write(new byte[] { (byte)bytesOut.size() });
        outputStream.write(bytesOut.toByteArray());
    }
}
