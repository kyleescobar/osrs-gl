package rs.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import rs.world.World;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO:
//  priority by distance,
//  cancel if too far,
//  render far away chunks low detail objects only
public class ChunkRenderScheduler {
    private final List<WorldRenderer> renderersToClose = new ArrayList<>();
    private final ExecutorService buildExecutor;
    private final Cache<Integer, WorldRenderer> chunks = CacheBuilder
            .newBuilder()
            .expireAfterAccess(Duration.ofSeconds(30))
            .weigher((Weigher<Integer, WorldRenderer>) (key, value) -> value.opaqueBuffer.memoryUsage() + value.translucentBuffer.memoryUsage())
            .maximumWeight(12000 * 1024L * 1024L)
            .removalListener(n -> renderersToClose.add(n.getValue()))
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .build();
    private final Set<Integer> scheduled = ConcurrentHashMap.newKeySet();
    private final World world;

    public ChunkRenderScheduler(World world) {
        this.world = world;
        buildExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public WorldRenderer get(int x, int y) {
        closedUncachedRenderers();

        int key = (x << 16) + y;
        WorldRenderer chunk = chunks.getIfPresent(key);

        if (chunk != null) {
            return chunk;
        }

        if (scheduled.add(key)) {
            buildExecutor.submit(() -> render(x, y));
        }

        return null;
    }

    private void closedUncachedRenderers() {
        for (WorldRenderer renderer : new ArrayList<>(renderersToClose)) {
            if (renderer != null) {
                renderer.opaqueBuffer.close();
                renderer.translucentBuffer.close();
            }
        }

        renderersToClose.clear();
    }

    private void render(int x, int y) {
        try {
            if (world.getRegion(x * Renderer.CHUNK_SIZE / 64, y * Renderer.CHUNK_SIZE / 64) == null) {
                return;
            }

            WorldRenderer renderer = new WorldRenderer(world);
            renderer.chunk(x, y);

            int key = (x << 16) + y;
            chunks.put(key, renderer);
            scheduled.remove(key);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(0);
        }
    }
}
