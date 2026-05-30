package up.raytracer.scene;

import up.raytracer.core.AABB;
import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;

public abstract class Object3D {

    private final Vector3D position;
    private final Material material;

    public Object3D(Vector3D position, Material material) {
        this.position = position;
        this.material = material;
    }

    public Vector3D getPosition() {
        return position;
    }

    public Material getMaterial() {
        return material;
    }

    // returns null if the ray misses
    public abstract Intersection calculateIntersection(Ray ray);

    // the hit carries barycentric data for phong interpolation on triangles
    public abstract Vector3D getNormal(Intersection hit);

    // used by the bvh to skip objects the ray cannot reach
    public abstract AABB getBounds();
}
