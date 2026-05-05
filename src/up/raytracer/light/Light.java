package up.raytracer.light;

import up.raytracer.core.Vector3D;

import java.awt.Color;

// base class for lights that add color and brightness to the scene
// subclasses decide how light direction is calculated at each point
public abstract class Light {

    private final Color color;
    private final double intensity;

    public Light(Color color, double intensity) {
        this.color = color;
        this.intensity = intensity;
    }

    public Color getColor() {
        return color;
    }

    public double getIntensity() {
        return intensity;
    }

    //returns the direction the light travels at the given surface point
    //Lambert shading negates this when it needs the direction toward the light
    public abstract Vector3D getDirectionAt(Vector3D point);
}
