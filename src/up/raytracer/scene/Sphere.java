package up.raytracer.scene;

import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;

public class Sphere extends Object3D {

    private final double radius;

    public Sphere(Vector3D position, Material material, double radius) {
        super(position, material);
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public Intersection calculateIntersection(Ray ray) {
        Vector3D L   = getPosition().subtract(ray.getOrigin());
        double   tca = L.dot(ray.getDirection());

        if (tca < 0) return null;

        double d2 = L.dot(L) - tca * tca;

        if (d2 > radius * radius) return null;

        double thc = Math.sqrt(radius * radius - d2);
        double t0  = tca - thc;
        double t1  = tca + thc;

        double t = (t0 >= 0) ? t0 : t1;
        if (t < 0) return null;

        return new Intersection(ray.at(t), t, this);
    }

    @Override
    public Vector3D getNormal(Intersection hit) {
        return hit.getPosition().subtract(getPosition()).normalize();
    }
}
