package up.raytracer.core;

public class Transform {

    private final Vector3D position;
    private final Vector3D rotation;
    private final Vector3D scale;

    public Transform(Vector3D position, Vector3D rotation, Vector3D scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    public static Transform identity() {
        return new Transform(
                new Vector3D(0, 0, 0),
                new Vector3D(0, 0, 0),
                new Vector3D(1, 1, 1)
        );
    }

    public static Transform fromDegrees(Vector3D position, Vector3D rotation, double scale) {
        return fromDegrees(position, rotation, new Vector3D(scale, scale, scale));
    }

    public static Transform fromDegrees(Vector3D position, Vector3D rotation, Vector3D scale) {
        return new Transform(
                position,
                new Vector3D(
                        Math.toRadians(rotation.x),
                        Math.toRadians(rotation.y),
                        Math.toRadians(rotation.z)
                ),
                scale
        );
    }

    public Vector3D applyToPoint(Vector3D point) {
        Vector3D scaled = new Vector3D(point.x * scale.x, point.y * scale.y, point.z * scale.z);
        return rotate(scaled).add(position);
    }

    public Vector3D applyToNormal(Vector3D normal) {
        return rotate(normal).normalize();
    }

    private Vector3D rotate(Vector3D value) {
        Vector3D rotated = rotateX(value);
        rotated = rotateY(rotated);
        return rotateZ(rotated);
    }

    private Vector3D rotateX(Vector3D value) {
        double cos = Math.cos(rotation.x);
        double sin = Math.sin(rotation.x);
        return new Vector3D(
                value.x,
                value.y * cos - value.z * sin,
                value.y * sin + value.z * cos
        );
    }

    private Vector3D rotateY(Vector3D value) {
        double cos = Math.cos(rotation.y);
        double sin = Math.sin(rotation.y);
        return new Vector3D(
                value.x * cos + value.z * sin,
                value.y,
                -value.x * sin + value.z * cos
        );
    }

    private Vector3D rotateZ(Vector3D value) {
        double cos = Math.cos(rotation.z);
        double sin = Math.sin(rotation.z);
        return new Vector3D(
                value.x * cos - value.y * sin,
                value.x * sin + value.y * cos,
                value.z
        );
    }
}
