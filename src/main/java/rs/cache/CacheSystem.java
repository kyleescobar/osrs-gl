package rs.cache;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.definitions.loaders.TransformLoader;
import net.runelite.cache.definitions.loaders.ObjectLoader;
import net.runelite.cache.definitions.loaders.OverlayLoader;
import net.runelite.cache.definitions.loaders.RegionLoader;
import net.runelite.cache.definitions.loaders.SequenceLoader;
import net.runelite.cache.definitions.loaders.SoundEffectLoader;
import net.runelite.cache.definitions.loaders.TextureLoader;
import net.runelite.cache.definitions.loaders.UnderlayLoader;
import rs.model.AnimationDefinition;
import rs.model.ModelDefinition;
import rs.model.TransformDefinition;
import rs.model.TextureDefinition;
import rs.sound.SoundEffectDefinition;
import rs.util.FastIntMap;
import rs.world.ObjectDefinition;
import rs.world.OverlayDefinition;
import rs.world.Region;
import rs.world.UnderlayDefinition;

public class CacheSystem {
    public static final Cache CACHE = new Cache();
    private static final Int2ObjectMap<ObjectDefinition> objectDefinitions = new FastIntMap<>();
    private static final Int2ObjectMap<UnderlayDefinition> underlayDefinitions = new FastIntMap<>();
    private static final Int2ObjectMap<OverlayDefinition> overlayDefinitions = new FastIntMap<>();
    private static final Int2ObjectMap<TextureDefinition> textureDefinitions = new FastIntMap<>();
    private static final Int2ObjectMap<ModelDefinition> modelDefinitions = new FastIntMap<>();
    private static final Int2ObjectMap<AnimationDefinition> sequenceDefinitions = new FastIntMap<>();
    private static final Int2ObjectMap<SoundEffectDefinition> soundEffectDefinitions = new FastIntMap<>();
    private static final Int2ObjectMap<TransformDefinition> frameDefinitions = new FastIntMap<>();
    private static final Int2ObjectMap<Region> regions = new FastIntMap<>();
    private static final Int2ObjectMap<TransformLoader.SkeletonDefinition> framemapDefinitions = new FastIntMap<>();

    public synchronized static Region getRegion(int x, int y) {
        int id = x << 8 | y;

        return regions.computeIfAbsent(id, k -> {
            Region region = new Region(x, y);

            byte[] terrain = null;
            byte[] locations = null;

            try {
                terrain = CACHE.archive(5).group("m" + x + "_" + y).file(0);
                locations = CACHE.archive(5).group("l" + x + "_" + y).file(0);
            } catch (Exception e) {
                System.err.println("Couldn't load region (" + x + ", " + y + ")");
            }

            if (terrain == null) {
                return null;
            }

            region.loadTerrain(RegionLoader.readTerrain(terrain));

            if (locations != null) {
                region.loadLocations(RegionLoader.loadLocations(locations));
            }

            return region;
        });
    }

    public static ObjectDefinition getObjectDefinition(int id) {
        return objectDefinitions.computeIfAbsent(id, k -> ObjectLoader.load(id, CACHE.get(2, 6, id)));
    }

    public static UnderlayDefinition getUnderlayDefinition(int id) {
        return underlayDefinitions.computeIfAbsent(id, k -> UnderlayLoader.load(id, CACHE.get(2, 1, id)));
    }

    public static ModelDefinition getModelDefinition(int id) {
        return modelDefinitions.computeIfAbsent(id, k -> ModelLoader.load(id, CACHE.get(7, id, 0)));
    }

    public static OverlayDefinition getOverlayDefinition(int id) {
        return overlayDefinitions.computeIfAbsent(id, k -> OverlayLoader.load(id, CACHE.get(2, 4, id)));
    }

    public static TextureDefinition getTextureDefinition(int id) {
        return textureDefinitions.computeIfAbsent(id, k -> TextureLoader.load(id, CACHE.get(9, 0, id)));
    }

    public static AnimationDefinition getSequenceDefinition(int id) {
        return sequenceDefinitions.computeIfAbsent(id, k -> SequenceLoader.load(id, CACHE.get(2, 12, id)));
    }

    public static SoundEffectDefinition getSoundEffectDefinition(int id) {
        return soundEffectDefinitions.computeIfAbsent(id, k -> SoundEffectLoader.readSoundEffect(CACHE.get(4, id, 0)));
    }

    public static TransformDefinition getFrameDefiniton(int id) {
        return frameDefinitions.computeIfAbsent(id, k -> TransformLoader.loadTransform(id, CACHE.get(0, id >> 16, id & 0xffff)));
    }

    public static TransformLoader.SkeletonDefinition getFramemapDefinition(int id) {
        return framemapDefinitions.computeIfAbsent(id, k -> TransformLoader.loadSkeleton(id, CACHE.get(1, id, 0)));
    }
}
