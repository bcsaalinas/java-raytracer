package up.raytracer.texture;

import java.awt.Color;

public interface Texture {
    // u and v usually come from obj vt coordinates
    Color sample(double u, double v);
}
