package rs.renderer;

import rs.util.Color;
import rs.gl.VertexBuffer;
import org.joml.Vector3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BufferBuilder {
    public static final int VERTEX_SIZE = 44;
    private ByteBuffer buffer;
    public int vertexCount = 0;
    private int memoryUsage = -1;
    private VertexBuffer uploaded;

    public BufferBuilder(int triangles) {
        buffer = ByteBuffer.allocateDirect(triangles * 3 * VERTEX_SIZE).order(ByteOrder.nativeOrder());
    }

    public void vertex(Vector3d position, Vector3d normal, Color color, double priority, double alpha) {
        if (buffer.limit() - buffer.position() < VERTEX_SIZE) {
            buffer.limit(buffer.position());
            buffer.position(0);
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.capacity() * 2).order(ByteOrder.nativeOrder());
            newBuffer.put(buffer);
            buffer = newBuffer;
        }

        vertexCount++;
        buffer.putFloat((float) position.x);
        buffer.putFloat((float) position.y);
        buffer.putFloat((float) position.z);

        buffer.putFloat((float) normal.x);
        buffer.putFloat((float) normal.y);
        buffer.putFloat((float) normal.z);

        buffer.putFloat((float) color.r);
        buffer.putFloat((float) color.g);
        buffer.putFloat((float) color.b);
        buffer.putFloat((float) alpha);

        buffer.putFloat((float) priority);

//        buffer.putFloat((float) modelPosition.x);
//        buffer.putFloat((float) modelPosition.y);
//        buffer.putFloat((float) modelPosition.z);
    }

    public VertexBuffer buffer() {
        if (uploaded == null) {
            memoryUsage = buffer.position();
            uploaded = new VertexBuffer();
            buffer.limit(buffer.position());
            uploaded.set(vertexCount, buffer.position(0));
            buffer = null;
        }

        return uploaded;
    }

    public void close() {
//        buffer = null;

        if (uploaded != null) {
            uploaded.close();
        }
    }

    public int memoryUsage() {
        return buffer != null ? buffer.limit() : memoryUsage;
    }
}
