package zerocopy.ioutils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Protocol {
    
    // Command Codes
    public static final int REQ_FILE_LIST = 1;
    public static final int REQ_MAIN = 2;
    public static final int REQ_DATA_SINGLE = 3;
    public static final int REQ_DATA_PARTIAL = 4;
    public static final int REQ_COMPLETE = 5;

    // --- WRITE HELPERS ---

    public static void writeInt(SocketChannel channel, int value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(value);
        buffer.flip();
        channel.write(buffer);
    }

    public static void writeLong(SocketChannel channel, long value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        buffer.flip();
        channel.write(buffer);
    }

    public static void writeString(SocketChannel channel, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeInt(channel, bytes.length); // 1. ส่งขนาดของ String
        channel.write(ByteBuffer.wrap(bytes)); // 2. ส่ง String
    }

    // --- READ HELPERS ---

    public static int readInt(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        readFully(channel, buffer);
        buffer.flip();
        return buffer.getInt();
    }

    public static long readLong(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        readFully(channel, buffer);
        buffer.flip();
        return buffer.getLong();
    }

    public static String readString(SocketChannel channel) throws IOException {
        int length = readInt(channel); // 1. อ่านขนาดของ String
        ByteBuffer buffer = ByteBuffer.allocate(length);
        readFully(channel, buffer); // 2. อ่าน String
        buffer.flip();
        return new String(buffer.array(), StandardCharsets.UTF_8);
    }

    // Helper ภายใน: อ่านจนกว่า Buffer จะเต็ม
    private static void readFully(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) == -1) {
                throw new IOException("Unexpected end of stream");
            }
        }
    }
}
