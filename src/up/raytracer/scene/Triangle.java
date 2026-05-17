package up.raytracer.scene;

import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;

public class Triangle extends Object3D {

    private static final double EPSILON = 1e-8;

    private final Vector3D v0, v1, v2;
    private final Vector3D n0, n1, n2;

    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Material material) {
        this(v0, v1, v2, faceNormal(v0, v1, v2), material);
    }

    private Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Vector3D faceNormal, Material material) {
        this(v0, v1, v2, faceNormal, faceNormal, faceNormal, material);
    }

    public Triangle(
            Vector3D v0,
            Vector3D v1,
            Vector3D v2,
            Vector3D n0,
            Vector3D n1,
            Vector3D n2,
            Material material
    ) {
        super(v0, material);
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.n0 = n0;
        this.n1 = n1;
        this.n2 = n2;
    }

    private static Vector3D faceNormal(Vector3D v0, Vector3D v1, Vector3D v2) {
        Vector3D edgeA = v1.subtract(v0);
        Vector3D edgeB = v0.subtract(v2);
        return edgeA.cross(edgeB).normalize();
    }

    // muller-trumbore ray-triangle intersection
    @Override
    public Intersection calculateIntersection(Ray ray) {
        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);

        Vector3D p   = ray.getDirection().cross(edge1);
        double   det = edge2.dot(p);

        if (Math.abs(det) < EPSILON) return null;

        double   invDet = 1.0 / det;
        Vector3D tvec   = ray.getOrigin().subtract(v0);

        double u = invDet * tvec.dot(p);
        if (u < 0 || u > 1) return null;

        Vector3D q = tvec.cross(edge2);
        double   v = invDet * ray.getDirection().dot(q);
        if (v < 0 || (u + v) > (1.0 + EPSILON)) return null;

        double t = invDet * q.dot(edge1);
        if (t < EPSILON) return null;

        return new Intersection(ray.at(t), t, this, u, v);
    }

    // interpolate per-vertex normals using barycentric coordinates
    @Override
    public Vector3D getNormal(Intersection hit) {
        double u = hit.getU();
        double v = hit.getV();
        double w = 1.0 - u - v;
        return n0.scale(w).add(n1.scale(v)).add(n2.scale(u)).normalize();
    }
}
