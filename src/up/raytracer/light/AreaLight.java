package up.raytracer.light;

import up.raytracer.core.Vector3D;

import java.awt.Color;

public class AreaLight extends Light {

    private final Vector3D center;
    private final Vector3D u;
    private final Vector3D v;
    private final int samples;

    public AreaLight(Color color, double intensity, Vector3D center, Vector3D u, Vector3D v, int samples) {
        super(color, intensity);
        this.center = center;
        this.u = u;
        this.v = v;
        this.samples = Math.max(1, samples);
    }

    @Override
    public Vector3D getDirectionAt(Vector3D point) {
        return getDirectionAt(point, 0);
    }

    public Vector3D getDirectionAt(Vector3D point, int sample) {
        return point.subtract(getSamplePosition(sample)).normalize();
    }

    public double getDistanceAt(Vector3D point, int sample) {
        return getSamplePosition(sample).subtract(point).magnitude();
    }

    @Override
    public double getAttenuatedIntensity(Vector3D point) {
        return getAttenuatedIntensity(point, 0);
    }

    public double getAttenuatedIntensity(Vector3D point, int sample) {
        double distance = Math.max(getDistanceAt(point, sample), 1e-3);
        return getIntensity() / (distance * distance);
    }

    public int getSamples() {
        return samples;
    }

    private Vector3D getSamplePosition(int sample) {
        int grid = (int) Math.ceil(Math.sqrt(samples));
        int row = sample / grid;
        int col = sample % grid;

        // samples are spread across the rectangle so shadows can fade softly
        double su = ((col + 0.5) / grid) - 0.5;
        double sv = ((row + 0.5) / grid) - 0.5;

        return center.add(u.scale(su)).add(v.scale(sv));
    }
}
