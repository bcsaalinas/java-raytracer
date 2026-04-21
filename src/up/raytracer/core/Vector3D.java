package up.raytracer.core;

//a point or direction in 3d space
public class Vector3D {

    public double x, y, z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D add(Vector3D v) {
        return new Vector3D(x + v.x, y + v.y, z + v.z);
    }

    public Vector3D subtract(Vector3D v) {
        return new Vector3D(x - v.x, y - v.y, z - v.z);
    }

    public Vector3D scale(double s) {
        return new Vector3D(x * s, y * s, z * s);
    }

    //dot product
    public double dot(Vector3D v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3D normalize() {
        double mag = magnitude();
        return new Vector3D(x / mag, y / mag, z / mag);
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f, %.3f)", x, y, z);
    }
}
