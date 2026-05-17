package up.raytracer.scene;

import java.awt.*;

public class Material {
    private final Color color;
    private final float shininess;
    private final double reflectivity;
    private final float specularCoefficient;

    public Material(Color color, float shininess, double reflectivity, float specularCoefficient) {
        this.color = color;
        this.shininess = shininess;
        this.reflectivity = reflectivity;
        this.specularCoefficient = specularCoefficient;
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
}
