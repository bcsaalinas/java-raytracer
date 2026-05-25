package up.raytracer.scene;

import java.awt.*;

public class Material {
    private final Color color;
    private final float shininess;
    private final double reflectivity;
    private final float specularCoefficient;
    private final double transparency;
    private final double refractiveIndex;

    public Material(Color color, float shininess, double reflectivity, float specularCoefficient) {
        this(color, shininess, reflectivity, specularCoefficient, 0.0, 1.0);
    }

    public Material(
            Color color,
            float shininess,
            double reflectivity,
            float specularCoefficient,
            double transparency,
            double refractiveIndex
    ) {
        this.color = color;
        this.shininess = shininess;
        this.reflectivity = reflectivity;
        this.specularCoefficient = specularCoefficient;
        this.transparency = transparency;
        this.refractiveIndex = refractiveIndex;
    }

    public Color getColor() {
        return color;
    }

    public float getShininess() {
        return shininess;
    }

    public double getReflectivity() {
        return reflectivity;
    }

    public float getSpecularCoefficient() {
        return specularCoefficient;
    }

    public double getTransparency() {
        return transparency;
    }

    public double getRefractiveIndex() {
        return refractiveIndex;
    }
}
