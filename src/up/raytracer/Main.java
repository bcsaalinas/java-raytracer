package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.io.OBJReader;
import up.raytracer.light.DirectionalLight;
import up.raytracer.light.PointLight;
import up.raytracer.render.Raytracer;
import up.raytracer.scene.Scene;
import up.raytracer.scene.Triangle;

import java.awt.Color;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Camera camera = new Camera(new Vector3D(0.0, -0.15, -1.2), 1080, 1080, 90, 0.1, 100.0);
        Scene  scene  = new Scene(camera, Color.BLACK);

        scene.addLight(new PointLight(Color.white, 9.0, new Vector3D(2, 2, 7)));

        for (Triangle t : OBJReader.load(
                "assets/utah_teapot2.obj", //render a new obj because why not
                new Color(255, 0, 0),
                new Vector3D(-0.8, -2.05, 7.1),
                1.0
                )){
            scene.addObject(t);
        }

        for (Triangle t : OBJReader.load(
                "assets/ground_plane.obj",
                new Color(220, 220, 220),
                new Vector3D(1.9, -2.45, 8.4),
                1.0
        )) {
            scene.addObject(t);
        }

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(), "output.png");
    }
}
