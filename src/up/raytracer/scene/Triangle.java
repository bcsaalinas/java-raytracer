package up.raytracer.scene;

import up.raytracer.core.AABB;
import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector2D;
import up.raytracer.core.Vector3D;

public class Triangle extends Object3D {

    private static final double EPSILON = 1e-8;

    private final Vector3D v0, v1, v2;
    private final Vector3D n0, n1, n2;
    private final Vector2D uv0, uv1, uv2;
    private final Vector3D tangent;
    private final Vector3D bitangent;

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
        this(v0, v1, v2, n0, n1, n2, null, null, null, material);
    }

    public Triangle(
            Vector3D v0,
            Vector3D v1,
            Vector3D v2,
            Vector3D n0,
            Vector3D n1,
            Vector3D n2,
            Vector2D uv0,
            Vector2D uv1,
            Vector2D uv2,
            Material material
    ) {
        super(v0, material);
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.n0 = n0;
        this.n1 = n1;
        this.n2 = n2;
        this.uv0 = uv0;
        this.uv1 = uv1;
        this.uv2 = uv2;
        Vector3D[] basis = calculateTangentBasis();
        this.tangent = basis[0];
        this.bitangent = basis[1];
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

        return new Intersection(ray.at(t), t, this, u, v, getTextureCoordinates(u, v));
    }

    // interpolate per-vertex normals using barycentric coordinates
    @Override
    public Vector3D getNormal(Intersection hit) {
        double u = hit.getU();
        double v = hit.getV();
        double w = 1.0 - u - v;
        return n0.scale(w).add(n1.scale(v)).add(n2.scale(u)).normalize();
    }

    private Vector2D getTextureCoordinates(double u, double v) {
        if (uv0 == null || uv1 == null || uv2 == null) return null;

        double w = 1.0 - u - v;
        return uv0.scale(w).add(uv1.scale(v)).add(uv2.scale(u));
    }

    @Override
    public Vector3D getTangent(Intersection hit) {
        return tangent;
    }

    @Override
    public Vector3D getBitangent(Intersection hit) {
        return bitangent;
    }

    private Vector3D[] calculateTangentBasis() {
        if (uv0 == null || uv1 == null || uv2 == null) {
            return fallbackTangentBasis();
        }

        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);
        double du1 = uv1.x - uv0.x;
        double dv1 = uv1.y - uv0.y;
        double du2 = uv2.x - uv0.x;
        double dv2 = uv2.y - uv0.y;
        double determinant = du1 * dv2 - du2 * dv1;

        if (Math.abs(determinant) < EPSILON) {
            return fallbackTangentBasis();
        }

        double inv = 1.0 / determinant;
        Vector3D tangent = edge1.scale(dv2).subtract(edge2.scale(dv1)).scale(inv).normalize();
        Vector3D bitangent = edge2.scale(du1).subtract(edge1.scale(du2)).scale(inv).normalize();
        return new Vector3D[] { tangent, bitangent };
    }

    private Vector3D[] fallbackTangentBasis() {
        Vector3D normal = faceNormal(v0, v1, v2);
        Vector3D helper = Math.abs(normal.y) < 0.9 ? new Vector3D(0, 1, 0) : new Vector3D(1, 0, 0);
        Vector3D tangent = helper.cross(normal).normalize();
        Vector3D bitangent = normal.cross(tangent).normalize();
        return new Vector3D[] { tangent, bitangent };
    }

    @Override
    public AABB getBounds() {
        double minX = Math.min(v0.x, Math.min(v1.x, v2.x));
        double minY = Math.min(v0.y, Math.min(v1.y, v2.y));
        double minZ = Math.min(v0.z, Math.min(v1.z, v2.z));
        double maxX = Math.max(v0.x, Math.max(v1.x, v2.x));
        double maxY = Math.max(v0.y, Math.max(v1.y, v2.y));
        double maxZ = Math.max(v0.z, Math.max(v1.z, v2.z));

        Vector3D min = new Vector3D(minX - EPSILON, minY - EPSILON, minZ - EPSILON);
        Vector3D max = new Vector3D(maxX + EPSILON, maxY + EPSILON, maxZ + EPSILON);
        return new AABB(min, max);
    }
}
