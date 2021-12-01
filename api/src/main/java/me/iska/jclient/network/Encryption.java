package me.iska.jclient.network;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class Encryption {

    public static Cipher getCipherFromSecrets(EncryptionType encryptionType, int mode, byte[] sharedKey, byte[] iv)
            throws GeneralSecurityException {
        switch (encryptionType) {
            case NONE: {
                break;
            }
            case AES256: {
                // I could go on all day about how fucking stupid Java's cipher implementation is but I won't :)
                Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");

                if (sharedKey.length > 32) sharedKey = Arrays.copyOf(sharedKey, 32);
                if (iv.length > 16) iv = Arrays.copyOf(iv, 16);

                SecretKeySpec keySpec = new SecretKeySpec(sharedKey, "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(iv);

                cipher.init(mode, keySpec, ivSpec);

                return cipher;
            }
            case BLOWFISH448: {
                break;
            }
            case CAST5: {
                break;
            }
        }
        return null;
    }

    public static class BetterCipherOutputStream extends FilterOutputStream {
        private final Cipher cipher;
        private final OutputStream output;
        private final byte[] inBuffer = new byte[1];
        private byte[] outBuffer;
        private boolean closed = false;

        public BetterCipherOutputStream(OutputStream output, Cipher cipher) {
            super(output);
            this.output = output;
            this.cipher = cipher;
        }

        public void write(int integer) throws IOException {
            inBuffer[0] = (byte)integer;
            try {
                outBuffer = cipher.doFinal(inBuffer, 0, 1);
            } catch (IllegalBlockSizeException | BadPaddingException error) {
                throw new IOException(error);
            }
            if (outBuffer != null) {
                output.write(outBuffer);
                outBuffer = null;
            }
        }

        public void write(byte[] bytes) throws IOException {
            write(bytes, 0, bytes.length);
        }

        public void write(byte[] bytes, int start, int end) throws IOException {
            try {
                outBuffer = cipher.update(bytes, start, end);
                if (outBuffer == null) outBuffer = cipher.doFinal(bytes, start, end);
            } catch (IllegalBlockSizeException | BadPaddingException error) {
                throw new IOException(error);
            }
            if (outBuffer != null) {
                output.write(outBuffer);
                outBuffer = null;
            }

        }

        public void flush() throws IOException {
            if (outBuffer != null) {
                output.write(outBuffer);
                outBuffer = null;
            }

            output.flush();
        }

        public void close() throws IOException {
            if (!closed) {
                closed = true;

                try {
                    outBuffer = this.cipher.doFinal();
                } catch (BadPaddingException | IllegalBlockSizeException error) {
                    outBuffer = null;
                }

                try {
                    flush();
                } catch (IOException ignored) {
                }

                out.close();
            }
        }
    }
}
