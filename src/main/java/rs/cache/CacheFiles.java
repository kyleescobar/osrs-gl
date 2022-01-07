package rs.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CacheFiles {
    public static byte[] read(int archive, int group) {
        try {
            Path path = Path.of("cache/" + archive + "/" + group);

            if (!Files.exists(path)) {
                return null;
            }

            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(int archive, int group, byte[] data) {
        try {
            Path path = Path.of("cache/" + archive + "/" + group);
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
