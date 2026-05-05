package up.raytracer.scene;

import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;

import java.awt.Color;

// a sphere defined by a center position, a radius, and a color
public class Sphere extends Object3D {

    private final double radius;

    public Sphere(Vector3D position, double radius, Color color) {
        super(position, color);
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    // we project the ray onto the line toward the sphere center,
    // then use pythagoras to find how far the ray enters and exits
    @Override
    public Intersection calculateIntersection(Ray ray) {
        Vector3D L   = getPosition().subtract(ray.getOrigin());
        double   tca = L.dot(ray.getDirection());

        //sphere is behind the ray
        if (tca < 0) return null;

        double d2 = L.dot(L) - tca * tca;

        //ray passes outside the sphere
        if (d2 > radius * radius) return null;

        double thc = Math.sqrt(radius * radius - d2);
        double t0  = tca - thc; //entry point
        double t1  = tca + thc; //exit point

        // pick the closest point that is in front of the ray
        double t = (t0 >= 0) ? t0 : t1;
        if (t < 0) return null;

        return new Intersection(ray.at(t), t, this);
    }

    // sphere normals point from the center to the surface point
    @Override
    public Vector3D getNormal(Vector3D point) {
        return point.subtract(getPosition()).normalize();
    }
}
