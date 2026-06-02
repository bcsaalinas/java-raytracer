package up.raytracer.scene;

import java.awt.*;

public class Material {
    private final Color color;
    private final float shininess;
    private final double reflectivity;
    private final float specularCoefficient;
    private final double transparency;
    private final double refractiveIndex;
    private final double roughness;
    private final double metallic;
    private final boolean cookTorrance;

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
        this(color, shininess, reflectivity, specularCoefficient, transparency, refractiveIndex, 0.5, 0.0, false);
    }

    public Material(
            Color color,
            float shininess,
            double reflectivity,
            float specularCoefficient,
            double transparency,
            double refractiveIndex,
            double roughness,
            double metallic,
            boolean cookTorrance
    ) {
        this.color = color;
        this.shininess = shininess;
        this.reflectivity = reflectivity;
        this.specularCoefficient = specularCoefficient;
        this.transparency = transparency;
        this.refractiveIndex = refractiveIndex;
        this.roughness = clamp(roughness, 0.02, 1.0);
        this.metallic = clamp(metallic, 0.0, 1.0);
        this.cookTorrance = cookTorrance;
    }

    public static Material cookTorrance(Color color, double roughness, double metallic, double reflectivity) {
        return new Material(color, 80f, reflectivity, 0.5f, 0.0, 1.0, roughness, metallic, true);
    }

    // cook-torrance materials use roughness and metallic instead of blinn-phong shininess
    public static Material cookTorrance(
            Color color,
            double roughness,
            double metallic,
            double reflectivity,
            double transparency,
            double refractiveIndex
    ) {
        return new Material(color, 120f, reflectivity, 0.8f, transparency, refractiveIndex, roughness, metallic, true);
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

    public double getRoughness() {
        return roughness;
    }

    public double getMetallic() {
        return metallic;
    }

    public boolean usesCookTorrance() {
        return cookTorrance;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
