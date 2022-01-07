package rs.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import rs.util.Color;
import org.joml.Vector3d;
import org.joml.Vector4d;
import rs.util.Util;
import rs.cache.CacheSystem;
import rs.renderer.WorldRenderer;

import java.util.ArrayList;
import java.util.List;

public class World {
    public static final int BLEND_RADIUS = 5;
    private final int[][][][] heights = new int[65536][][][]; // todo: flatten into two layers for less pointer dereferences
    private final UnderlayDefinition[][][][] underlays = new UnderlayDefinition[65536][][][];
    private final OverlayDefinition[][][][] overlays = new OverlayDefinition[65536][][][];
    private final OverlayShape[][][][] overlayShapes = new OverlayShape[65536][][][];
    private final byte[][][][] overlayRotations = new byte[65536][][][];
    private final Color[][][][] blendedColors = new Color[65536][][][];
    private final Int2ObjectMap<List<Location>> dynamicLocations = new Int2ObjectOpenHashMap<>();

    public double height(int z, int x, int y) {
        int regionX = x / 64;
        int regionY = y / 64;

        int[][][] heights = getHeights(regionX, regionY);

        if (heights.length == 0) {
            return -getNeighborRegionHeight(z, x, y) * WorldRenderer.SCALE;
        }

        int xir = x % 64;
        int yir = y % 64;

        return -heights[z][xir][yir] * WorldRenderer.SCALE;
    }

    private int getNeighborRegionHeight(int z, int x, int y) {
        int height = -1;
        if (height == -1) height = heightDirect(z, x, y);
        if (height == -1) height = heightDirect(z, x - 1, y);
        if (height == -1) height = heightDirect(z, x + 1, y);
        if (height == -1) height = heightDirect(z, x, y - 1);
        if (height == -1) height = heightDirect(z, x, y + 1);
        if (height == -1) height = heightDirect(z, x - 1, y - 1);
        if (height == -1) height = heightDirect(z, x + 1, y + 1);
        if (height == -1) height = heightDirect(z, x - 1, y + 1);
        if (height == -1) height = heightDirect(z, x + 1, y - 1);
        if (height == -1) height = heightDirect(z, x - 2, y);
        if (height == -1) height = heightDirect(z, x + 2, y);
        if (height == -1) height = heightDirect(z, x, y - 2);
        if (height == -1) height = heightDirect(z, x, y + 2);
        if (height == -1) height = heightDirect(z, x - 2, y - 2);
        if (height == -1) height = heightDirect(z, x + 2, y + 2);
        if (height == -1) height = heightDirect(z, x - 2, y + 2);
        if (height == -1) height = heightDirect(z, x + 2, y - 2);
        return height;
    }

    public int heightDirect(int z, int x, int y) {
        int[][][] heights = getHeights(x / 64, y / 64);
        return heights.length == 0 ? -1 : heights[z][x % 64][y % 64];
    }

    private int[][][] getHeights(int regionX, int regionY) {
        int[][][] heights = this.heights[(regionX << 8) + regionY];

        if (heights == null) {
            loadRegion(regionX, regionY);
            heights = this.heights[(regionX << 8) + regionY];
        }
        return heights;
    }

    private void loadRegion(int regionX, int regionY) {
        Region region = getRegion(regionX, regionY);

        if (region == null) {
            region = new Region(regionX, regionY);
            heights[(regionX << 8) + regionY] = new int[0][][];
            underlays[(regionX << 8) + regionY] = region.underlays;
            overlays[(regionX << 8) + regionY] = region.overlays;
            overlayShapes[(regionX << 8) + regionY] = region.overlayShapes;
            overlayRotations[(regionX << 8) + regionY] = region.overlayRotations;
        } else {
            heights[(regionX << 8) + regionY] = region.heights;
            underlays[(regionX << 8) + regionY] = region.underlays;
            overlays[(regionX << 8) + regionY] = region.overlays;
            overlayShapes[(regionX << 8) + regionY] = region.overlayShapes;
            overlayRotations[(regionX << 8) + regionY] = region.overlayRotations;
        }
    }

    public Color color(int plane, int x, int y) {
        Color a = getTileColor(plane, x, y);
        Color b = getTileColor(plane, x - 1, y);
        Color c = getTileColor(plane, x, y - 1);
        Color d = getTileColor(plane, x - 1, y - 1);

        return new Color(
                (a.r + b.r + c.r + d.r) / 4,
                (a.g + b.g + c.g + d.g) / 4,
                (a.b + b.b + c.b + d.b) / 4
        );
    }

    public Color getTileColor(int plane, int x, int y) {
        int regionX = x / 64;
        int regionY = y / 64;

        Color[][][] blendedColors = this.blendedColors[(regionX << 8) + regionY];

        if (blendedColors == null) {
            calculateRegionColors(regionX * 64, regionY * 64);
            blendedColors = this.blendedColors[(regionX << 8) + regionY];
        }

        return blendedColors[plane][x % 64][y % 64];
    }

    private void calculateRegionColors(int x, int y) {
        Color[][][] colors = new Color[4][64][64];

        for (int plane = 0; plane < 4; plane++) {
            Vector4d[][] blended = new Vector4d[64 + 2 * BLEND_RADIUS][64 + 2 * BLEND_RADIUS];

            for (int dx = -BLEND_RADIUS; dx < 64 + BLEND_RADIUS; dx++) {
                for (int dy = -BLEND_RADIUS; dy < 64 + BLEND_RADIUS; dy++) {
                    Color c = getUnblendedColor(plane, x + dx, y + dy);
                    blended[BLEND_RADIUS + dx][BLEND_RADIUS + dy] = c == null ? new Vector4d(0, 0, 0, 0) : new Vector4d(c.r, c.g, c.b, 1);
                }
            }

            blended = Util.boxBlur(blended, BLEND_RADIUS, 64);

            for (int dx = 0; dx < 64; dx++) {
                for (int dy = 0; dy < 64; dy++) {
                    Vector4d c = blended[dx + BLEND_RADIUS][dy + BLEND_RADIUS];
                    colors[plane][dx][dy] = new Color(c.x / c.w, c.y / c.w, c.z / c.w);
                }
            }
        }

        blendedColors[(x / 64 << 8) + y / 64] = colors;
    }

    public Color getUnblendedColor(int plane, int x, int y) {
        UnderlayDefinition underlay = getUnderlay(plane, x, y);

        if (underlay == null) {
            return null;
        }

        return Color.fromRgb(underlay.color);
    }

    public UnderlayDefinition getUnderlay(int plane, int x, int y) {
        int regionX = x / 64;
        int regionY = y / 64;

        UnderlayDefinition[][][] underlays = this.underlays[(regionX << 8) + regionY];

        if (underlays == null) {
            loadRegion(regionX, regionY);
            underlays = this.underlays[(regionX << 8) + regionY];
        }

        return underlays[plane][x % 64][y % 64];
    }

    public OverlayDefinition getOverlay(int plane, int x, int y) {
        int regionX = x / 64;
        int regionY = y / 64;

        OverlayDefinition[][][] overlays = this.overlays[(regionX << 8) + regionY];

        if (overlays == null) {
            loadRegion(regionX, regionY);
            overlays = this.overlays[(regionX << 8) + regionY];
        }

        return overlays[plane][x % 64][y % 64];
    }

    public Vector3d position(int plane, int x, int y) {
        return new Vector3d(x, y, height(plane, x, y));
    }

    public Vector3d position(int plane, double x, double y) {
        return new Vector3d(x, y, height(plane, x, y));
    }

    public double height(int plane, double x, double y) {
        double h00 = height(plane, (int) x, (int) y);
        double h10 = height(plane, (int) x + 1, (int) y);
        double h01 = height(plane, (int) x, (int) y + 1);
        double h11 = height(plane, (int) x + 1, (int) y + 1);

        return h00 * (1 - x % 1) * (1 - y % 1) +
                h10 * (x % 1) * (1 - y % 1) +
                h01 * (1 - x % 1) * (y % 1) +
                h11 * (x % 1) * (y % 1);
    }

    public Vector3d normal(int plane, int x, int y) {
        Vector3d center = position(plane, x, y);
        Vector3d e = position(plane, x + 0.01, y);
        Vector3d n = position(plane, x, y + 0.01);
        Vector3d w = position(plane, x - 0.01, y);
        Vector3d s = position(plane, x, y - 0.01);

        return new Vector3d()
                .add(Util.normal(center, e, n))
                .add(Util.normal(center, n, w))
                .add(Util.normal(center, w, s))
                .add(Util.normal(center, s, e))
                .normalize();
    }

    public Vector3d normal(int plane, double x, double y) {
        Vector3d center = position(plane, x, y);
        Vector3d e = position(plane, x + 0.01, y);
        Vector3d n = position(plane, x, y + 0.01);
        Vector3d w = position(plane, x - 0.01, y);
        Vector3d s = position(plane, x, y - 0.01);

        return new Vector3d()
                .add(Util.normal(center, e, n))
                .add(Util.normal(center, n, w))
                .add(Util.normal(center, w, s))
                .add(Util.normal(center, s, e))
                .normalize();
    }

    public Color color(int plane, double x, double y) {
        Vector3d n00 = color(plane, (int) x, (int) y).toVector();
        Vector3d n10 = color(plane, (int) x + 1, (int) y).toVector();
        Vector3d n01 = color(plane, (int) x, (int) y + 1).toVector();
        Vector3d n11 = color(plane, (int) x + 1, (int) y + 1).toVector();

        return Color.fromVector(new Vector3d()
                .add(n00.mul(1 - x % 1).mul(1 - y % 1))
                .add(n10.mul(x % 1).mul(1 - y % 1))
                .add(n01.mul(1 - x % 1).mul(y % 1))
                .add(n11.mul(x % 1).mul(y % 1))
        );
    }

    public Region getRegion(int x, int y) {
        return CacheSystem.getRegion(x, y);
    }

    public List<Location> getLocations(int regionX, int regionY) {
        return getRegion(regionX, regionY).locations;
    }

    public OverlayShape getOverlayShape(int plane, int x, int y) {
        int regionX = x / 64;
        int regionY = y / 64;

        OverlayShape[][][] overlayShapes = this.overlayShapes[(regionX << 8) + regionY];

        if (overlays == null) {
            loadRegion(regionX, regionY);
            overlayShapes = this.overlayShapes[(regionX << 8) + regionY];
        }

        return overlayShapes[plane][x % 64][y % 64];
    }

    public byte getOverlayRotation(int plane, int x, int y) {
        int regionX = x / 64;
        int regionY = y / 64;

        byte[][][] overlayRotations = this.overlayRotations[(regionX << 8) + regionY];

        if (overlays == null) {
            loadRegion(regionX, regionY);
            overlayRotations = this.overlayRotations[(regionX << 8) + regionY];
        }

        return overlayRotations[plane][x % 64][y % 64];
    }

    public List<Location> getDynamicLocations(int chunkX, int chunkY) {
        return dynamicLocations.computeIfAbsent((chunkX << 16) + chunkY, k -> {
            List<Location> result = new ArrayList<>();
            int x1 = chunkX * WorldRenderer.CHUNK_SIZE;
            int y1 = chunkY * WorldRenderer.CHUNK_SIZE;
            int x2 = x1 + WorldRenderer.CHUNK_SIZE;
            int y2 = y1 + WorldRenderer.CHUNK_SIZE;

            for (Location location : getLocations(x1 / 64, y1 / 64)) {
                int x = location.position.x;
                int y = location.position.y;

                if (x >= x1 && x < x2 && y >= y1 && y < y2) {
                    if (location.object.animation != null) {
                        result.add(location);
                    }
                }
            }

            return result;
        });
    }

    public Vector3d squareNormal(int plane, int x, int y) {
        Vector3d p00 = position(plane, x, y);
        Vector3d p10 = position(plane, x + 1, y);
        Vector3d p01 = position(plane, x, y + 1);
        Vector3d p11 = position(plane, x + 1, y + 1);

        Vector3d n1 = Util.normal(p11, p10, p01);
        Vector3d n2 = Util.normal(p00, p01, p10);
        return new Vector3d(0).add(n1).add(n2).normalize();
    }
}
