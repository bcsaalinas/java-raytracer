package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.io.OBJReader;
import up.raytracer.light.PointLight;
import up.raytracer.render.Raytracer;
import up.raytracer.scene.Material;
import up.raytracer.scene.Scene;
import up.raytracer.scene.Sphere;
import up.raytracer.scene.Triangle;

import java.awt.Color;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Camera camera = new Camera(new Vector3D(0.0, 0.2, -3.1), 1280, 960, 42, 0.1, 100.0);
        Scene scene = new Scene(camera, new Color(32, 36, 44));

        // key + fill + rim setup to make diffuse and specular terms easier to read
        scene.addLight(new PointLight(new Color(255, 244, 228), 54.0, new Vector3D(2.8, 4.4, -0.5)));
        scene.addLight(new PointLight(new Color(190, 210, 255), 18.0, new Vector3D(-3.2, 2.0, -1.0)));
        scene.addLight(new PointLight(new Color(170, 190, 255), 10.5, new Vector3D(0.4, 2.4, 8.0)));

        // same hue, different shininess
        scene.addObject(new Sphere(
                new Vector3D(-1.55, -1.65, 4.7),
                new Material(new Color(188, 44, 44), 10f, 0.2, 0.15f),
                0.50
        ));
        scene.addObject(new Sphere(
                new Vector3D(-0.35, -1.70, 4.95),
                new Material(new Color(188, 44, 44), 55f, 0.5, 0.55f),
                0.44
        ));
        scene.addObject(new Sphere(
                new Vector3D(0.74, -1.74, 5.24),
                new Material(new Color(188, 44, 44), 190f, 0.5, 0.95f),
                0.38
        ));

        Material teapotMaterial = new Material(new Color(66, 74, 188), 90f, 0.7, 0.62f);
        for (Triangle t : OBJReader.load(
                "assets/utah_teapot2.obj",
                teapotMaterial,
                new Vector3D(0.95, -1.10, 6.25),
                0.74
        )) {
            scene.addObject(t);
        }

        Material groundMaterial = new Material(new Color(82, 88, 96), 24f, 0.0, 0.30f);
        for (Triangle t : OBJReader.load(
                "assets/ground_plane.obj",
                groundMaterial,
                new Vector3D(1.9, -2.45, 8.4),
                1.45
        )) {
            scene.addObject(t);
        }

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(3), "output.png");
    }
}
