package up.raytracer.light;

import up.raytracer.core.Vector3D;

import java.awt.Color;

public class SpotLight extends PointLight {

    private final Vector3D direction;
    private final double innerCos;
    private final double outerCos;

    public SpotLight(
            Color color,
            double intensity,
            Vector3D position,
            Vector3D direction,
            double innerAngle,
            double outerAngle
    ) {
        super(color, intensity, position);
        this.direction = direction.normalize();
        this.innerCos = Math.cos(Math.toRadians(innerAngle));
        this.outerCos = Math.cos(Math.toRadians(outerAngle));
    }

    @Override
    public double getAttenuatedIntensity(Vector3D point) {
        return super.getAttenuatedIntensity(point) * getSpotFactor(point);
    }

    private double getSpotFactor(Vector3D point) {
        double coneCos = getDirectionAt(point).dot(direction);

        if (coneCos >= innerCos) return 1.0;
        if (coneCos <= outerCos) return 0.0;

        return (coneCos - outerCos) / (innerCos - outerCos);
    }
}
