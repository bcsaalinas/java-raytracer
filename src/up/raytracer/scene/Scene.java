package up.raytracer.scene;

import up.raytracer.camera.Camera;
import up.raytracer.light.Light;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//holds everything that makes up a renderable scene
public class Scene {

    private final Camera camera;
    private final List<Object3D> objects;
    private final List<Light> lights; // lights used to shade the objects
    private final Color backgroundColor;

    public Scene(Camera camera, Color backgroundColor) {
        this.camera = camera;
        this.objects = new ArrayList<>();
        this.lights = new ArrayList<>();
        this.backgroundColor = backgroundColor;
    }

    public void addObject(Object3D object) {
        objects.add(object);
    }

    public void addLight(Light light) {
        lights.add(light);
    }

    public List<Light> getLights() {
        return Collections.unmodifiableList(lights);
    }

    public Camera getCamera() {
        return camera;
    }

    //read only view so nobody accidentally modifies the list from outside
    public List<Object3D> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }
}
