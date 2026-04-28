package up.raytracer.camera;

import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;

// the camera sits at a position and shoots rays through each pixel
public class Camera {

    private final Vector3D position;
    private final int width;
    private final int height;
    private final double fov;
    private final double near; // minimum distance to consider a hit
    private final double far;  // maximum distance to consider a hit

    public Camera(Vector3D position, int width, int height, double fov, double near, double far) {
        this.position = position;
        this.width    = width;
        this.height   = height;
        this.fov      = fov;
        this.near     = near;
        this.far      = far;
    }

    // maps a pixel to a ray in world space using perspective projection
    // pixels go left-to-right and top-to-bottom, world x goes right, y goes up
    public Ray getRayForPixel(int px, int py) {
        double aspectRatio = (double) width / height;
        double scale       = Math.tan(Math.toRadians(fov / 2.0));

        double x = (2.0 * (px + 0.5) / width  - 1.0) * aspectRatio * scale;
        double y = (1.0 - 2.0 * (py + 0.5) / height) * scale;

        return new Ray(position, new Vector3D(x, y, 1.0));
    }

    public Vector3D getPosition() { return position; }
    public int getWidth()         { return width; }
    public int getHeight()        { return height; }
    public double getNear()       { return near; }
    public double getFar()        { return far; }
}
