package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.render.Raytracer;
import up.raytracer.scene.Scene;
import up.raytracer.scene.Sphere;

import java.awt.Color;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Camera camera = new Camera(new Vector3D(0, 0, -1), 800, 800, 60);
        Scene  scene  = new Scene(camera, Color.WHITE);

        scene.addObject(new Sphere(new Vector3D(-0.5, 0, 3), 1.0, Color.RED));
        scene.addObject(new Sphere(new Vector3D(1.25, 0, 3), 0.5, Color.BLUE));

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(), "output.png");
    }
}
