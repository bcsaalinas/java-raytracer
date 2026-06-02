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
    private final double apertureRadius;
    private final double focalDistance;
    private final int lensSamples;

    public Camera(Vector3D position, int width, int height, double fov, double near, double far) {
        this(position, width, height, fov, near, far, 0.0, 1.0, 1);
    }

    public Camera(
            Vector3D position,
            int width,
            int height,
            double fov,
            double near,
            double far,
            double apertureRadius,
            double focalDistance,
            int lensSamples
    ) {
        this.position = position;
        this.width    = width;
        this.height   = height;
        this.fov      = fov;
        this.near     = near;
        this.far      = far;
        this.apertureRadius = Math.max(0.0, apertureRadius);
        this.focalDistance = Math.max(near, focalDistance);
        this.lensSamples = Math.max(1, lensSamples);
    }

    // maps a pixel to a ray in world space using perspective projection
    // pixels go left-to-right and top-to-bottom, world x goes right, y goes up
    public Ray getRayForPixel(int px, int py) {
        return getRayForPixel(px, py, 0);
    }

    public Ray getRayForPixel(int px, int py, int sample) {
        double aspectRatio = (double) width / height;
        double scale       = Math.tan(Math.toRadians(fov / 2.0));

        double x = (2.0 * (px + 0.5) / width  - 1.0) * aspectRatio * scale;
        double y = (1.0 - 2.0 * (py + 0.5) / height) * scale;

        Vector3D direction = new Vector3D(x, y, 1.0).normalize();
        if (!hasDepthOfField()) return new Ray(position, direction);

        // all lens rays aim at the same point on the focal plane
        Vector3D focusPoint = position.add(direction.scale(focalDistance));
        Vector3D lensOffset = getLensOffset(sample);
        Vector3D lensPosition = position.add(lensOffset);
        return new Ray(lensPosition, focusPoint.subtract(lensPosition));
    }

    private Vector3D getLensOffset(int sample) {
        // golden angle samples fill the lens disk without random noise
        double angle = sample * 2.399963229728653;
        double radius = apertureRadius * Math.sqrt((sample + 0.5) / lensSamples);

        return new Vector3D(
                Math.cos(angle) * radius,
                Math.sin(angle) * radius,
                0.0
        );
    }

    public boolean hasDepthOfField() {
        return apertureRadius > 0.0 && lensSamples > 1;
    }

    public Vector3D getPosition() { return position; }
    public int getWidth()         { return width; }
    public int getHeight()        { return height; }
    public double getNear()       { return near; }
    public double getFar()        { return far; }
    public int getLensSamples()   { return lensSamples; }
}
