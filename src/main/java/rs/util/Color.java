package rs.util;

import org.joml.Vector3d;

public class Color {
    public final double r;
    public final double g;
    public final double b;

    public Color(double r, double g, double b) {
        this.r = Math.max(0, Math.min(r, 1));
        this.g = Math.max(0, Math.min(g, 1));
        this.b = Math.max(0, Math.min(b, 1));
    }

    public static Color fromRgb(int i) {
        return new Color(
                (i >> 16 & 0xff) / 255.,
                (i >> 8 & 0xff) / 255.,
                (i & 0xff) / 255.
        );
    }

    public static int hslToRgb(int hsl) {
        float hue = (hsl >> 10 & 63) / 63f;
        float saturation = (hsl >> 7 & 7) / 7f;
        float lightness = (hsl & 127) / 127f;

        float brightness = lightness < 0.5 ? lightness * (1 + saturation) : lightness * (1 - saturation) + saturation;
        float adjustedSaturation = 2 * (1 - lightness / brightness);
        return java.awt.Color.HSBtoRGB(hue, adjustedSaturation, brightness);
    }

    public Vector3d toVector() {
        return new Vector3d(r, g, b);
    }

    public static Color fromVector(Vector3d v) {
        return new Color(v.x, v.y, v.z);
    }
}
