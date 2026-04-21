package up.raytracer.core;

//a ray is a starting point plus a direction it travels
public class Ray {

    private final Vector3D origin;
    private final Vector3D direction; // always normalized

    public Ray(Vector3D origin, Vector3D direction) {
        this.origin = origin;
        this.direction = direction.normalize();
    }

    public Vector3D getOrigin() {
        return origin;
    }

    public Vector3D getDirection() {
        return direction;
    }

    //gives the point along the ray at distance t: origin + t * direction
    public Vector3D at(double t) {
        return origin.add(direction.scale(t));
    }
}
