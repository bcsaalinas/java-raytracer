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
    private final Vector3D forward;
    private final Vector3D right;
    private final Vector3D up;
    private final double apertureRadius;
    private final double focalDistance;
    private final int lensSamples;

    public Camera(Vector3D position, int width, int height, double fov, double near, double far) {
        this(position, width, height, fov, near, far, 0.0, 1.0, 1);
    }

    public Camera(Vector3D position, Vector3D direction, int width, int height, double fov, double near, double far) {
        this(position, direction, width, height, fov, near, far, 0.0, 1.0, 1);
    }

    public Camera(
            Vector3D position,
            Vector3D direction,
            Vector3D cameraUp,
            int width,
            int height,
            double fov,
            double near,
            double far,
            double apertureRadius,
            double focalDistance,
            int lensSamples
    ) {
        this(position, direction, buildRight(direction, cameraUp), buildUp(direction, cameraUp), width, height, fov, near, far, apertureRadius, focalDistance, lensSamples);
    }

    public Camera(
            Vector3D position,
            Vector3D direction,
            int width,
            int height,
            double fov,
            double near,
            double far,
            double apertureRadius,
            double focalDistance,
            int lensSamples
    ) {
        this(position, direction, buildRight(direction), buildUp(direction), width, height, fov, near, far, apertureRadius, focalDistance, lensSamples);
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
        this(position, new Vector3D(0.0, 0.0, 1.0), new Vector3D(1.0, 0.0, 0.0), new Vector3D(0.0, 1.0, 0.0), width, height, fov, near, far, apertureRadius, focalDistance, lensSamples);
    }

    private Camera(
            Vector3D position,
            Vector3D forward,
            Vector3D right,
            Vector3D up,
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
        this.forward = forward.normalize();
        this.right = right.normalize();
        this.up = up.normalize();
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

        Vector3D direction = forward.add(right.scale(x)).add(up.scale(y)).normalize();
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
                right.x * Math.cos(angle) * radius + up.x * Math.sin(angle) * radius,
                right.y * Math.cos(angle) * radius + up.y * Math.sin(angle) * radius,
                right.z * Math.cos(angle) * radius + up.z * Math.sin(angle) * radius
        );
    }

    private static Vector3D buildRight(Vector3D direction) {
        Vector3D worldUp = new Vector3D(0.0, 0.0, 1.0);
        return buildRight(direction, worldUp);
    }

    private static Vector3D buildRight(Vector3D direction, Vector3D cameraUp) {
        Vector3D right = direction.normalize().cross(cameraUp.normalize());
        if (right.magnitude() < 1e-8) {
            right = new Vector3D(1.0, 0.0, 0.0);
        }
        return right;
    }

    private static Vector3D buildUp(Vector3D direction) {
        Vector3D worldUp = new Vector3D(0.0, 0.0, 1.0);
        return buildUp(direction, worldUp);
    }

    private static Vector3D buildUp(Vector3D direction, Vector3D cameraUp) {
        Vector3D forward = direction.normalize();
        return buildRight(direction, cameraUp).cross(forward);
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
