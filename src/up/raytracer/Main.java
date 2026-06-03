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
import up.raytracer.texture.CheckerTexture;

import java.awt.Color;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Camera camera = new Camera(new Vector3D(0.0, 0.35, -4.8), 1280, 720, 43, 0.1, 100.0, 0.035, 6.4, 5);
        Scene scene = new Scene(camera, new Color(16, 18, 24));
        scene.setFog(new Color(42, 48, 58), 0.020);

        // wide soft light makes roughness differences easy to compare
        scene.addLight(new AreaLight(
                new Color(255, 238, 218),
                85.0,
                new Vector3D(-1.6, 3.1, 1.4),
                new Vector3D(4.8, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 2.4),
                25
        ));

        Material floorMaterial = Material.cookTorrance(new Color(28, 30, 34), 0.24, 0.0, 0.55);
        Material bunnyMaterial = Material.cookTorrance(new Color(210, 178, 128), 0.38, 0.0, 0.16);
        Material matteRed = Material.cookTorrance(new Color(210, 48, 42), 0.82, 0.0, 0.02);
        Material satinBlue = Material.cookTorrance(new Color(58, 110, 220), 0.38, 0.0, 0.12);
        Material polishedGold = Material.cookTorrance(new Color(255, 185, 80), 0.16, 1.0, 0.42);
        Material roughMetal = Material.cookTorrance(new Color(130, 145, 160), 0.58, 1.0, 0.20);
        Material glassMaterial = Material.cookTorrance(new Color(225, 250, 255), 0.05, 0.0, 0.04, 0.88, 1.50);
        Material checkerMaterial = Material.cookTorrance(new Color(255, 255, 255), 0.48, 0.0, 0.05)
                .withTexture(new CheckerTexture(new Color(235, 235, 220), new Color(36, 48, 72), 8));

        // reflective floor shows how each material handles indirect color
        for (Triangle t : OBJReader.load(
                "assets/ground_plane.obj",
                floorMaterial,
                new Vector3D(0.0, -1.82, 6.2),
                1.20
        )) {
            scene.addObject(t);
        }

        // uv panel behind the objects to test texture coordinates
        for (Triangle t : OBJReader.load(
                "assets/uv_panel.obj",
                checkerMaterial,
                new Vector3D(0.0, -0.35, 7.45),
                new Vector3D(0, 0, 0),
                new Vector3D(2.25, 1.10, 1.0)
        )) {
            scene.addObject(t);
        }

        // central non-sphere model for pbr shading on real geometry
        for (Triangle t : OBJReader.load(
                "assets/bunny.obj",
                bunnyMaterial,
                new Vector3D(0.0, -1.82, 6.35),
                new Vector3D(0, -18, 0),
                8.0
        )) {
            scene.addObject(t);
        }

        scene.addObject(new Sphere(
                new Vector3D(-2.05, -1.42, 4.95),
                matteRed,
                0.38
        ));
        scene.addObject(new Sphere(
                new Vector3D(-1.05, -1.42, 4.75),
                satinBlue,
                0.38
        ));
        scene.addObject(new Sphere(
                new Vector3D(1.05, -1.42, 4.75),
                polishedGold,
                0.38
        ));
        scene.addObject(new Sphere(
                new Vector3D(2.05, -1.42, 4.95),
                roughMetal,
                0.38
        ));
        scene.addObject(new Sphere(
                new Vector3D(0.0, -1.35, 4.45),
                glassMaterial,
                0.46
        ));

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(3), "output.png");
    }
}
