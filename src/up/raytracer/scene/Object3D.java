package up.raytracer.scene;

import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;

import java.awt.Color;

public abstract class Object3D {

    private final Vector3D position;
    private final Color color;

    public Object3D(Vector3D position, Color color) {
        this.position = position;
        this.color = color;
    }

    public Vector3D getPosition() {
        return position;
    }

    public Color getColor() {
        return color;
    }

    // returns null if the ray misses
    public abstract Intersection calculateIntersection(Ray ray);

    // the hit carries barycentric data for phong interpolation on triangles
    public abstract Vector3D getNormal(Intersection hit);
}
