package up.raytracer.texture;

import java.awt.Color;

public class CheckerTexture implements Texture {

    private final Color a;
    private final Color b;
    private final int checks;

    public CheckerTexture(Color a, Color b, int checks) {
        this.a = a;
        this.b = b;
        this.checks = Math.max(1, checks);
    }

    @Override
    public Color sample(double u, double v) {
        // wrapping lets the checker repeat outside the 0..1 uv range
        int x = (int) Math.floor(wrap(u) * checks);
        int y = (int) Math.floor(wrap(v) * checks);
        return ((x + y) % 2 == 0) ? a : b;
    }

    private double wrap(double value) {
        double wrapped = value - Math.floor(value);
        return wrapped < 0 ? wrapped + 1.0 : wrapped;
    }
}
