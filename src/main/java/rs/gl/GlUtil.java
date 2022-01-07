package rs.gl;

import static org.lwjgl.opengl.GL11C.glGetError;

public class GlUtil {
    public static void checkError() {
        int err = glGetError();

        if (err != 0) {
            throw new RuntimeException("0x" + Integer.toHexString(err));
        }
    }
}
