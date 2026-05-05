package up.raytracer.scene;

import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;

import java.awt.Color;

//base class for everything that can be placed and rendered in the scene
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

    //each shape knows how to intersect itself with a ray
    //returns null if the ray misses
    public abstract Intersection calculateIntersection(Ray ray);

    // returns the surface direction used by the lighting code
    public abstract Vector3D getNormal(Vector3D point);
}
