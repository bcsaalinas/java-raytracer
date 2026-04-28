package up.raytracer.scene;

import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;

import java.awt.Color;

// a triangle defined by three vertices, using the Moller-Trumbore intersection algorithm
public class Triangle extends Object3D {

    // used to avoid division by zero and floating point noise
    private static final double EPSILON = 1e-8;

    private final Vector3D v0, v1, v2;

    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Color color) {
        super(v0, color);
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
    }

    // Moller-Trumbore: solves where the ray meets the plane of the triangle,
    // then checks if that point is actually inside the triangle using barycentric coordinates (u, v)
    @Override
    public Intersection calculateIntersection(Ray ray) {
        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);

        Vector3D p   = ray.getDirection().cross(edge1);
        double   det = edge2.dot(p);

        // ray is parallel to the triangle, no intersection
        if (Math.abs(det) < EPSILON) return null;

        double   invDet = 1.0 / det;
        Vector3D tvec   = ray.getOrigin().subtract(v0);

        double u = invDet * tvec.dot(p);
        if (u < 0 || u > 1) return null;

        Vector3D q = tvec.cross(edge2);
        double   v = invDet * ray.getDirection().dot(q);
        if (v < 0 || (u + v) > (1.0 + EPSILON)) return null;

        double t = invDet * q.dot(edge1);
        if (t < EPSILON) return null; // intersection is behind the ray origin

        return new Intersection(ray.at(t), t, this);
    }
}
