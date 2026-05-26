package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.io.OBJReader;
import up.raytracer.light.PointLight;
import up.raytracer.light.SpotLight;
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
        scene.addLight(new SpotLight(
                new Color(255, 244, 228),
                220.0,
                new Vector3D(1.4, 3.8, 0.4),
                new Vector3D(-1.95, -5.25, 4.4),
                24.0,
                46.0
        ));
        scene.addLight(new PointLight(new Color(190, 210, 255), 8.0, new Vector3D(-3.2, 2.0, -1.0)));
        scene.addLight(new PointLight(new Color(170, 190, 255), 5.0, new Vector3D(0.4, 2.4, 8.0)));

        Material glassMaterial = new Material(new Color(235, 248, 255), 260f, 0.01, 0.75f, 0.94, 1.50);

        // the red sphere sits behind the glass sphere to test refraction
        scene.addObject(new Sphere(
                new Vector3D(-0.55, -1.45, 5.45),
                new Material(new Color(188, 44, 44), 10f, 0.2, 0.15f),
                0.44
        ));
        scene.addObject(new Sphere(
                new Vector3D(-0.55, -1.45, 4.20),
                glassMaterial,
                0.58
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
                new Vector3D(0, -18, 0),
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
