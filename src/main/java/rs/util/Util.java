package rs.util;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;

public class Util {
    public static final int GOLDEN_RATIO = 0x9E3779B9;
    public static final int ROUNDS = 32;
    public static CRC32 crc = new CRC32();

    public static Vector3d normal(Vector3d center, Vector3d a, Vector3d b) { // ccw = up (inwards)
        Vector3d da = a.sub(center, new Vector3d());
        Vector3d db = b.sub(center, new Vector3d());
        return da.cross(db).normalize();
    }

    public static Vector4d[][] boxBlur(Vector4d[][] blended, int radius, int size) {
        // Horizontal blur
        Vector4d[][] newBlended = new Vector4d[size + 2 * radius][size + 2 * radius];

        for (int dy = -radius; dy < size + radius; dy++) {
            Vector4d color = new Vector4d(0);

            for (int dx = -2 * radius; dx < size + radius; dx++) {
                if (dx >= 0) color.sub(blended[radius + dx - radius][radius + dy]);
                if (dx >= -radius) newBlended[radius + dx][radius + dy] = new Vector4d(color);
                if (dx < size) color.add(blended[radius + dx + radius][radius + dy]);
            }
        }

        blended = newBlended;

        // Vertical blur
        newBlended = new Vector4d[size + 2 * radius][size + 2 * radius];

        for (int dx = -radius; dx < size + radius; dx++) {
            Vector4d color = new Vector4d(0);

            for (int dy = -2 * radius; dy < size + radius; dy++) {
                if (dy >= 0) color.sub(blended[radius + dx][radius + dy - radius]);
                if (dy >= -radius) newBlended[radius + dx][radius + dy] = new Vector4d(color);
                if (dy < size) color.add(blended[radius + dx][radius + dy + radius]);
            }
        }

        blended = newBlended;
        return blended;
    }

    public static void unxtea(byte[] data, int start, int end, int[] key) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int numQuads = (end - start) / 8;

        for (int i = 0; i < numQuads; i++) {
            int sum = GOLDEN_RATIO * ROUNDS;
            int v0 = buffer.getInt(start + i * 8);
            int v1 = buffer.getInt(start + i * 8 + 4);

            for (int j = 0; j < ROUNDS; j++) {
                v1 -= (v0 << 4 ^ v0 >>> 5) + v0 ^ sum + key[sum >>> 11 & 3];
                sum -= GOLDEN_RATIO;
                v0 -= (v1 << 4 ^ v1 >>> 5) + v1 ^ sum + key[sum & 3];
            }

            buffer.putInt(start + i * 8, v0);
            buffer.putInt(start + i * 8 + 4, v1);
        }
    }

    public static byte charToByteCp1252(char c) {
        byte result;

        if (c > 0 && c < 128 || c >= 160 && c <= 255) {
            result = (byte) c;
            return result;
        }

        result = switch (c) {
            case 8364 -> (byte) -128;
            case 8218 -> (byte) -126;
            case 402 -> (byte) -125;
            case 8222 -> (byte) -124;
            case 8230 -> (byte) -123;
            case 8224 -> (byte) -122;
            case 8225 -> (byte) -121;
            case 710 -> (byte) -120;
            case 8240 -> (byte) -119;
            case 352 -> (byte) -118;
            case 8249 -> (byte) -117;
            case 338 -> (byte) -116;
            case 381 -> (byte) -114;
            case 8216 -> (byte) -111;
            case 8217 -> (byte) -110;
            case 8220 -> (byte) -109;
            case 8221 -> (byte) -108;
            case 8226 -> (byte) -107;
            case 8211 -> (byte) -106;
            case 8212 -> (byte) -105;
            case 732 -> (byte) -104;
            case 8482 -> (byte) -103;
            case 353 -> (byte) -102;
            case 8250 -> (byte) -101;
            case 339 -> (byte) -100;
            case 382 -> (byte) -98;
            case 376 -> (byte) -97;
            default -> (byte) 0x3f;
        };

        return result;
    }

    public static int hash(String s) {
        int length = s.length();
        int hash = 0;

        for (int i = 0; i < length; ++i) {
            hash = (hash << 5) - hash + charToByteCp1252(s.charAt(i));
        }

        return hash;
    }

    public static int hash(byte[] data) {
        return crc(data);
    }

    public static int crc(byte[] data) {
        crc.reset();
        crc.update(data, 0, data.length);
        return (int) crc.getValue();
    }

    public static byte[] decompress(byte[] bytes) {
        try {
            NetworkBuffer buffer = new NetworkBuffer(bytes);
            int compressionType = buffer.readUnsignedByte();
            int size = buffer.readInt();

            switch (compressionType) {
                case 0 -> {
                    return buffer.read(size);
                }

                case 1 -> {
                    int uncompressedSize = buffer.readInt();
                    byte[] data = new byte[4 + buffer.array.length - buffer.offset];
                    data[0] = 'B';
                    data[1] = 'Z';
                    data[2] = 'h';
                    data[3] = '1';
                    System.arraycopy(buffer.array, buffer.offset, data, 4, buffer.array.length - buffer.offset);
                    return new BZip2CompressorInputStream(new ByteArrayInputStream(data)).readNBytes(uncompressedSize);
                }

                case 2 -> {
                    int uncompressedSize = buffer.readInt();
                    return new GZIPInputStream(buffer.stream()).readNBytes(uncompressedSize);
                }

                default -> throw new AssertionError("unknown compression type " + compressionType);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
