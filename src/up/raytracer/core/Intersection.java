package up.raytracer.core;

import up.raytracer.scene.Object3D;

public class Intersection {

    private final Vector3D position;
    private final double distance;
    private final Object3D object;

    // barycentric coordinates, used by triangles for phong interpolation
    private final double u, v;

    public Intersection(Vector3D position, double distance, Object3D object) {
        this(position, distance, object, 0.0, 0.0);
    }

    public Intersection(Vector3D position, double distance, Object3D object, double u, double v) {
        this.position = position;
        this.distance = distance;
        this.object = object;
        this.u = u;
        this.v = v;
    }

    public Vector3D getPosition() {
        return position;
    }

    public double getDistance() {
        return distance;
    }

    public Object3D getObject() {
        return object;
    }

    public double getU() {
        return u;
    }

    public double getV() {
        return v;
    }
}
