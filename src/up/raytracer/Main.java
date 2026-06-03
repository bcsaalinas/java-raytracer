package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.io.OBJReader;
import up.raytracer.light.AreaLight;
import up.raytracer.render.Raytracer;
import up.raytracer.scene.Material;
import up.raytracer.scene.Scene;
import up.raytracer.scene.Sphere;
import up.raytracer.scene.Triangle;

import java.awt.Color;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Camera camera = new Camera(new Vector3D(0.0, 0.55, -4.4), 4096, 2160, 39, 0.1, 100.0, 0.075, 6.6, 9);
        Scene scene = new Scene(camera, new Color(18, 22, 30));
        scene.setFog(new Color(48, 58, 76), 0.035);

        // large warm light above the exhibit, like a museum ceiling panel
        scene.addLight(new AreaLight(
                new Color(255, 232, 204),
                70.0,
                new Vector3D(-1.8, 3.4, 3.6),
                new Vector3D(3.4, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 2.0),
                36
        ));

        Material floorMaterial = new Material(new Color(34, 37, 42), 70f, 0.52, 0.55f);
        Material bunnyMaterial = new Material(new Color(210, 190, 156), 110f, 0.18, 0.65f);
        Material blueCrystal = new Material(new Color(150, 210, 255), 260f, 0.16, 0.90f, 0.62, 1.48);
        Material redCrystal = new Material(new Color(255, 90, 86), 220f, 0.20, 0.85f, 0.45, 1.40);
        Material pearlMaterial = new Material(new Color(245, 230, 205), 180f, 0.32, 0.80f);
        Material shadowMaterial = new Material(new Color(44, 48, 72), 60f, 0.10, 0.35f);

        // reflective floor for shadows, glass and silhouettes
        for (Triangle t : OBJReader.load(
                "assets/ground_plane.obj",
                floorMaterial,
                new Vector3D(0.0, -1.95, 6.4),
                1.45
        )) {
            scene.addObject(t);
        }

        // the bunny is the main artifact in the room
        for (Triangle t : OBJReader.load(
                "assets/bunny.obj",
                bunnyMaterial,
                new Vector3D(-0.15, -1.95, 6.05),
                new Vector3D(0, -22, 0),
                9.5
        )) {
            scene.addObject(t);
        }

        for (Triangle t : OBJReader.load(
                "assets/octahedron.obj",
                blueCrystal,
                new Vector3D(-1.65, -0.80, 5.65),
                new Vector3D(18, 30, -12),
                0.32
        )) {
            scene.addObject(t);
        }

        for (Triangle t : OBJReader.load(
                "assets/octahedron.obj",
                redCrystal,
                new Vector3D(1.55, -0.95, 6.10),
                new Vector3D(-10, -28, 24),
                0.25
        )) {
            scene.addObject(t);
        }

        scene.addObject(new Sphere(
                new Vector3D(-0.95, -1.72, 4.85),
                pearlMaterial,
                0.34
        ));
        scene.addObject(new Sphere(
                new Vector3D(1.05, -1.70, 4.92),
                shadowMaterial,
                0.30
        ));
        scene.addObject(new Sphere(
                new Vector3D(0.0, -1.45, 4.35),
                new Material(new Color(230, 250, 255), 240f, 0.03, 0.85f, 0.90, 1.50),
                0.42
        ));

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(3), "output.png");
    }
}
