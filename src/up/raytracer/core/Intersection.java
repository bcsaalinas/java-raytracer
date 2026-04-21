package up.raytracer.core;

import up.raytracer.scene.Object3D;

//stores the result when a ray hits an object
public class Intersection {

    private final Vector3D position;
    private final double distance;
    private final Object3D object;

    public Intersection(Vector3D position, double distance, Object3D object) {
        this.position = position;
        this.distance = distance;
        this.object = object;
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
}
