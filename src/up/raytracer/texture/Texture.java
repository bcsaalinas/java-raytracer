package up.raytracer.texture;

import java.awt.Color;

public interface Texture {
    Color sample(double u, double v);
}
