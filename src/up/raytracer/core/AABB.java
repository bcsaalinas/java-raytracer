package up.raytracer.core;

public class AABB {

    private final Vector3D min;
    private final Vector3D max;

    public AABB(Vector3D min, Vector3D max) {
        this.min = min;
        this.max = max;
    }

    public Vector3D getMin() {
        return min;
    }

    public Vector3D getMax() {
        return max;
    }

    public Vector3D getCenter() {
        return new Vector3D(
                (min.x + max.x) * 0.5,
                (min.y + max.y) * 0.5,
                (min.z + max.z) * 0.5
        );
    }

    public int getLongestAxis() {
        double x = max.x - min.x;
        double y = max.y - min.y;
        double z = max.z - min.z;

        if (x >= y && x >= z) return 0;
        if (y >= z) return 1;
        return 2;
    }

    public double getSurfaceArea() {
        double x = Math.max(0.0, max.x - min.x);
        double y = Math.max(0.0, max.y - min.y);
        double z = Math.max(0.0, max.z - min.z);
        return 2.0 * (x * y + x * z + y * z);
    }

    public boolean intersects(Ray ray, double near, double far) {
        return hitDistance(ray, near, far) != Double.POSITIVE_INFINITY;
    }

    // slab test, returns infinity when the ray misses the box
    public double hitDistance(Ray ray, double near, double far) {
        double tMin = near;
        double tMax = far;

        double[] origin = { ray.getOrigin().x, ray.getOrigin().y, ray.getOrigin().z };
        double[] direction = { ray.getDirection().x, ray.getDirection().y, ray.getDirection().z };
        double[] boxMin = { min.x, min.y, min.z };
        double[] boxMax = { max.x, max.y, max.z };

        for (int i = 0; i < 3; i++) {
            if (Math.abs(direction[i]) < 1e-12) {
                if (origin[i] < boxMin[i] || origin[i] > boxMax[i]) return Double.POSITIVE_INFINITY;
                continue;
            }

            double invD = 1.0 / direction[i];
            double t0 = (boxMin[i] - origin[i]) * invD;
            double t1 = (boxMax[i] - origin[i]) * invD;

            if (invD < 0) {
                double temp = t0;
                t0 = t1;
                t1 = temp;
            }

            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);
            if (tMax < tMin) return Double.POSITIVE_INFINITY;
        }

        return tMin;
    }

    // combines two boxes into the smallest box that contains both
    public static AABB surrounding(AABB a, AABB b) {
        Vector3D min = new Vector3D(
                Math.min(a.min.x, b.min.x),
                Math.min(a.min.y, b.min.y),
                Math.min(a.min.z, b.min.z)
        );
        Vector3D max = new Vector3D(
                Math.max(a.max.x, b.max.x),
                Math.max(a.max.y, b.max.y),
                Math.max(a.max.z, b.max.z)
        );
        return new AABB(min, max);
    }
}
