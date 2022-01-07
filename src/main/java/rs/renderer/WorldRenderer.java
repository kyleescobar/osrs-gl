package rs.renderer;

import rs.util.Color;
import org.joml.Vector3d;
import rs.util.Util;
import rs.cache.CacheSystem;
import rs.model.ModelDefinition;
import rs.model.TransformDefinition;
import rs.world.*;

import java.util.List;

public class WorldRenderer {
    public static final double SCALE = 1 / 128.;
    public static final int CHUNK_SIZE = Renderer.CHUNK_SIZE;
    public final BufferBuilder opaqueBuffer = new BufferBuilder(2500);
    public final BufferBuilder translucentBuffer = new BufferBuilder(100);
    private final World world;

    public WorldRenderer(World world) {
        this.world = world;
    }

    public void chunk(int chunkX, int chunkY) {
        int x1 = chunkX * CHUNK_SIZE;
        int y1 = chunkY * CHUNK_SIZE;
        int x2 = x1 + CHUNK_SIZE;
        int y2 = y1 + CHUNK_SIZE;

        triangle(new Vector3d(x2, y2, -10), new Vector3d(x1, y2, -10), new Vector3d(x1, y1, -10), new Color(0, 0, 0), 0);
        triangle(new Vector3d(x1, y1, -10), new Vector3d(x2, y1, -10), new Vector3d(x2, y2, -10), new Color(0, 0, 0), 0);

        for (int plane = 0; plane < 4; plane++) {
            for (int x = x1; x < x2; x++) {
                for (int y = y1; y < y2; y++) {
                    tile(plane, x, y);
                }
            }
        }

        for (Location location : world.getLocations(x1 / 64, y1 / 64)) {
            int plane = location.position.z;
            int x = location.position.x;
            int y = location.position.y;

            if (x >= x1 && x < x2 && y >= y1 && y < y2) {
                object(location.object, location.type, plane, x, y, location.rotation);
            }
        }
    }

    public void dynamicLocations(int chunkX, int chunkY) {
        int x1 = chunkX * CHUNK_SIZE;
        int y1 = chunkY * CHUNK_SIZE;
        int x2 = x1 + CHUNK_SIZE;
        int y2 = y1 + CHUNK_SIZE;

        if (world.getRegion(x1 / 64, y1 / 64) != null) {
            for (Location location : world.getDynamicLocations(chunkX, chunkY)) {
                int plane = location.position.z;
                int x = location.position.x;
                int y = location.position.y;

                if (x >= x1 && x < x2 && y >= y1 && y < y2) {
                    object(location.object, location.type, plane, x, y, location.rotation);
                }
            }
        }
    }

    private void tile(int plane, int x, int y) {
        UnderlayDefinition underlay = world.getUnderlay(plane, x, y);
        OverlayDefinition overlay = world.getOverlay(plane, x, y);

        if (underlay != null && overlay == null) {
            groundSquare(plane, x, y, null);
        }

        if (overlay != null) {
            Color color = Color.fromRgb(overlay.texture == null ? overlay.color : overlay.texture.averageColor);

            if (overlay.color == 0xff00ff) {
                color = null;
            }

            OverlayShape shape = world.getOverlayShape(plane, x, y);
            int rotation = world.getOverlayRotation(plane, x, y);

            if (shape == OverlayShape.FULL) {
                if (color != null) {
                    groundSquare(plane, x, y, color);
                }
                return;
            }

            for (OverlayShape.Triangle triangle : shape.triangles) {
                Vector3d a = triangle.positionA().rotateZ(-Math.PI / 2 * rotation).add(x + 0.5, y + 0.5, 0);
                Vector3d b = triangle.positionB().rotateZ(-Math.PI / 2 * rotation).add(x + 0.5, y + 0.5, 0);
                Vector3d c = triangle.positionC().rotateZ(-Math.PI / 2 * rotation).add(x + 0.5, y + 0.5, 0);

                if (triangle.overlay && overlay != null && color != null) {
                    groundVertex(plane, a.x, a.y, color);
                    groundVertex(plane, b.x, b.y, color);
                    groundVertex(plane, c.x, c.y, color);
                }

                if (!triangle.overlay && underlay != null) {
                    groundVertex(plane, a.x, a.y, null);
                    groundVertex(plane, b.x, b.y, null);
                    groundVertex(plane, c.x, c.y, null);
                }
            }
        }
    }

    private void groundSquare(int plane, int x, int y, Color color) {
        double h00 = world.height(plane, x, y);
        double h01 = world.height(plane, x, y + 1);
        double h10 = world.height(plane, x + 1, y);
        double h11 = world.height(plane, x + 1, y + 1);

        if (Math.abs(h00 - h11) > Math.abs(h10 - h01)) {
            groundVertex(plane, x + 1, y + 1, color);
            groundVertex(plane, x, y + 1, color);
            groundVertex(plane, x + 1, y, color);

            groundVertex(plane, x, y, color);
            groundVertex(plane, x + 1, y, color);
            groundVertex(plane, x, y + 1, color);
        } else {
            groundVertex(plane, x + 1, y + 1, color);
            groundVertex(plane, x, y + 1, color);
            groundVertex(plane, x, y, color);

            groundVertex(plane, x + 1, y + 1, color);
            groundVertex(plane, x, y, color);
            groundVertex(plane, x + 1, y, color);
        }
    }

    public void groundVertex(int plane, double x, double y, Color color) {
        vertex(world.position(plane, x, y), world.normal(plane, x, y), color == null ? world.color(plane, x, y) : color, plane * 20);
    }

    private void object(ObjectDefinition object, LocationType type, int plane, int x, int y, int rotation) {
        if (object.models == null && object.typeModels == null) {
            return;
        }

        List<ModelDefinition> models = null;

        if (object.typeModels != null) {
            ModelDefinition model = object.typeModels.get(type.baseType);

            if (model != null) {
                models = List.of(model);
            }
        }

        if (models == null && object.models != null) {
            models = object.models;
        }

        if (models == null || models.isEmpty()) {
            return;
        }

        // flip
        boolean flip = object.mirror;

        // rotate

        if (rotation > 3) {
            throw new UnsupportedOperationException("nyi");
        }

        rotation %= 4;
        double angle = -Math.PI / 2 * rotation;

        if (type == LocationType.OBJECT_DIAGONAL || type == LocationType.WALL_DECORATION_DIAGONAL || type == LocationType.WALL_DECORATION_OPPOSITE_DIAGONAL || type == LocationType.WALL_DECORATION_DOUBLE) {
            angle += -Math.PI / 4;
        }

        // scale
        Vector3d scale = new Vector3d(object.scaleX / 128., object.scaleY / 128., object.scaleZ / 128.);

        // translate
        int sizeX = rotation == 0 || rotation == 2 ? object.sizeX : object.sizeY;
        int sizeY = rotation == 0 || rotation == 2 ? object.sizeY : object.sizeX;

        Vector3d pos = world.position(plane, x + sizeX / 2., y + sizeY / 2.);
        double centerZ = pos.z;
        pos.z = 0;

        double wallWidth = 1;

        if (type.baseType == LocationType.WALL_DECORATION) {
            for (Location location : world.getLocations(x / 64, y / 64)) {
                if (location.position.x != x || location.position.y != y && location.position.z == plane) {
                    continue;
                }

                if (location.type != LocationType.WALL && location.type != LocationType.WALL_CORNER && location.type != LocationType.DIAGONAL_WALL) {
                    continue;
                }

                if (location.object.decorationOffset == 16) {
                    continue;
                }

                wallWidth = location.object.decorationOffset / 16.;
                break;
            }
        }

//        if (type.baseType != LocationType.WALL_DECORATION)
//        pos.add(offsetX, offsetY, -object.offsetZ * SCALE);

        pos.add(object.offsetX * SCALE, object.offsetY * SCALE, -object.offsetZ*SCALE);
        Color color = null;

        wallWidth = 0.25;
        Vector3d pos2 = new Vector3d();
        if (type == LocationType.WALL_DECORATION_DIAGONAL) {
            pos2.add(0.5+wallWidth/2, -(0.5+wallWidth/2), 0);
            pos2.add(0.5, 0.5, 0);
            flip=true;
//            color = new Color(1, 0, 0);
        }

        if (type == LocationType.WALL_DECORATION_OPPOSITE) {
            pos2.add(wallWidth, 0, 0);
//            color = new Color(0, 1, 0);
        }

        if (type == LocationType.WALL_DECORATION_OPPOSITE_DIAGONAL) {
            pos2.add(0.5+wallWidth/2, -(0.5+wallWidth/2), 0);
//            color = new Color(1, 1, 0);
        }

        if (type == LocationType.WALL_DECORATION_DOUBLE) {
//            color = new Color(0, 0, 1);
            return;
        }

        double extraPriority = 0;

        if (type.baseType == LocationType.WALL_DECORATION || type.baseType == LocationType.OBJECT || type == LocationType.FLOOR_DECORATION) {
            extraPriority += 5;
        }

//        if (type == LocationType.FLOOR_DECORATION && !object.obstructsGround && object.interactType == 0 && object.wall == -1) {
//            red = 1;
//        }

        for (ModelDefinition model : models) {
            if (type == LocationType.WALL_CORNER) {
                model(object, color, plane, model, !flip, angle, scale, pos, centerZ, null, object.animation != null, extraPriority, pos2);
                model(object, color, plane, model, flip, angle - Math.PI / 2, scale, pos, centerZ, null, object.animation != null, extraPriority, pos2);
            } else {
                model(object, color, plane, model, flip, angle, scale, pos, centerZ, null, object.animation != null, extraPriority, pos2);
            }
        }
    }

    private void model(ObjectDefinition object, Color highlight, int plane, ModelDefinition model, boolean flip, double angle, Vector3d scale, Vector3d pos, double centerZ, TransformDefinition transform, boolean dynamic, double extraPriority, Vector3d pos2) {
        for (ModelDefinition.Face face : model.faces) {
            Color color = Color.fromRgb(object.colorSubstitutions.getOrDefault(face.color, face.color));

            if (face.transparency >= 254) {
                continue;
            }

            if (highlight != null) {
                color = highlight;
            }

            if (face.texture != -1) {
                int texture = object.textureSubstitutions.getOrDefault(face.texture, face.texture);
                color = Color.fromRgb(CacheSystem.getTextureDefinition(texture).averageColor);
            }

            Vector3d a = new Vector3d(face.a.x * SCALE, face.a.z * SCALE, -face.a.y * SCALE);
            Vector3d b = new Vector3d(face.b.x * SCALE, face.b.z * SCALE, -face.b.y * SCALE);
            Vector3d c = new Vector3d(face.c.x * SCALE, face.c.z * SCALE, -face.c.y * SCALE);

            if (flip) { // reverse vertex order for culling to work
                Vector3d t = a;
                a = c;
                c = t;
            }

//            double colorMultiplier = 1 + (object.ambient) / 128.;
//            color = new Color(color.r * colorMultiplier, color.g * colorMultiplier, color.b * colorMultiplier);

            triangle(
                    adjustZ(plane, a.mul(1, flip ? -1 : 1, 1).mul(scale).add(pos2).rotateZ(angle).add(pos), centerZ, object),
                    adjustZ(plane, b.mul(1, flip ? -1 : 1, 1).mul(scale).add(pos2).rotateZ(angle).add(pos), centerZ, object),
                    adjustZ(plane, c.mul(1, flip ? -1 : 1, 1).mul(scale).add(pos2).rotateZ(angle).add(pos), centerZ, object),
                    color,
                    model.priority + face.priority + extraPriority + plane * 20,
                    0xff - face.transparency
            );
        }
    }

    private Vector3d adjustZ(int plane, Vector3d v, double centerZ, ObjectDefinition object) {
        if (object.contouredGround == -1) {
            v.z += centerZ;
        }

        if (object.contouredGround == 0) {
            v.z += world.height(plane, v.x, v.y);
        }

        if (object.contouredGround > 0) { // TODO: ???????
            throw new UnsupportedOperationException("nyi");
        }

        return v;
    }

    public void triangle(Vector3d a, Vector3d b, Vector3d c, Color color, double priority) {
        Vector3d normal = Util.normal(a, b, c);
        vertex(a, normal, color, 0);
        vertex(b, normal, color, 0);
        vertex(c, normal, color, 0);
    }

    public void triangle(Vector3d a, Vector3d b, Vector3d c, Color color, double priority, int alpha) {
        Vector3d normal = Util.normal(a, b, c);
        BufferBuilder buffer = alpha == 0xff ? opaqueBuffer : translucentBuffer;

        buffer.vertex(a, normal, color, priority, alpha / 255.);
        buffer.vertex(b, normal, color, priority, alpha / 255.);
        buffer.vertex(c, normal, color, priority, alpha / 255.);
    }

    private void vertex(Vector3d position, Vector3d normal, Color color, int priority) {
        opaqueBuffer.vertex(position, normal, color, priority, 1.0);
    }
}
