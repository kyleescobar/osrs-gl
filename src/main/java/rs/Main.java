package rs;

import rs.cache.CacheSystem;
import rs.renderer.Renderer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        Files.write(Path.of("xtea.json"), new URL("http://xtea.openosrs.dev/get").openStream().readAllBytes());
        CacheSystem.CACHE.init();
        new Renderer().run();
        System.exit(0);
    }
}
