package up.raytracer.scene;

import up.raytracer.camera.Camera;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//holds everything that makes up a renderable scene
public class Scene {

    private final Camera camera;
    private final List<Object3D> objects;
    private final Color backgroundColor;

    public Scene(Camera camera, Color backgroundColor) {
        this.camera = camera;
        this.objects = new ArrayList<>();
        this.backgroundColor = backgroundColor;
    }

    public void addObject(Object3D object) {
        objects.add(object);
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
