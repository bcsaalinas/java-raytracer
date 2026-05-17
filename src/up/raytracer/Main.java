package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.io.OBJReader;
import up.raytracer.light.PointLight;
import up.raytracer.render.Raytracer;
import up.raytracer.scene.Material;
import up.raytracer.scene.Scene;
import up.raytracer.scene.Triangle;

import java.awt.Color;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Camera camera = new Camera(new Vector3D(0.0, 0.05, -0.7), 1080, 1080, 60, 0.1, 100.0);
        Scene  scene  = new Scene(camera, Color.BLACK);

        scene.addLight(new PointLight(Color.white, 10.0, new Vector3D(0.0, 2, 4)));

        Material teapotMaterial = new Material(new Color(0, 0, 255), 32f, 0.0, 0.5f);
        for (Triangle t : OBJReader.load(
                "assets/utah_teapot2.obj",
                teapotMaterial,
                new Vector3D(-0.8, -2.05, 7.1),
                1.0
        )) {
            scene.addObject(t);
        }

        Material groundMaterial = new Material(new Color(220, 220, 220), 16f, 0.0, 0.3f);
        for (Triangle t : OBJReader.load(
                "assets/ground_plane.obj",
                groundMaterial,
                new Vector3D(1.9, -2.45, 8.4),
                1.0
        )) {
            scene.addObject(t);
        }

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(), "output.png");
    }
}
