package up.raytracer.light;

import up.raytracer.core.Vector3D;

import java.awt.Color;



//directional light acts like sunlight, with parallel rays everywhere in the scene
public class DirectionalLight extends Light {

    private final Vector3D direction; // the direction the light rays travel

    public DirectionalLight(Vector3D direction, Color color, double intensity) {
        super(color, intensity);
        this.direction = direction.normalize();
    }

    // for a directional light the point does not matter, the rays are parallel
    @Override
    public Vector3D getDirectionAt(Vector3D point) {
        return direction;
    }

   @Override
   public double getAttenuatedIntensity(Vector3D point){
    return getIntensity(); // no attenuation for directional light
   }

}
