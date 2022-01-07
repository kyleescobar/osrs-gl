package rs.world;

import rs.world.OverlayDefinition;
import rs.world.OverlayShape;
import rs.world.UnderlayDefinition;

public class MapDefinition {
    public static final int X = 64;
    public static final int Y = 64;
    public static final int Z = 4;

    public int regionX;
    public int regionY;
    public Tile[][][] tiles = new Tile[Z][X][Y];

    public static class Tile {
        public Integer height;
        public byte settings;
        public OverlayDefinition overlay;
        public OverlayShape overlayShape;
        public byte overlayRotation;
        public UnderlayDefinition underlay;
    }
}
