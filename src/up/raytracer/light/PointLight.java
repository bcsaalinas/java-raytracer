package up.raytracer.light;

import up.raytracer.core.Vector3D;

import java.awt.*;

//point light has a set position in the space
public class PointLight extends Light{

    private final Vector3D position;

    public PointLight(Color color, double intensity, Vector3D position) {
        super(color, intensity);
        this.position = position;
    }


    //direction where the light travels at certain point
    //in the ray tracer class I negate the direction, thats why i inverted the substraction
    @Override
    public Vector3D getDirectionAt(Vector3D point) {
        return point.subtract(position).normalize();
    }

    public double getDistanceAt(Vector3D point) {
        return position.subtract(point).magnitude();
    }

   @Override
    public double getAttenuatedIntensity(Vector3D point){
        double distance = Math.max(getDistanceAt(point), 1e-3); //safe clamp to prevent division by 0
        return getIntensity() / (distance * distance);
    }


}
