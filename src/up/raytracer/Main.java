package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.io.OBJReader;
import up.raytracer.light.AreaLight;
import up.raytracer.render.Raytracer;
import up.raytracer.scene.Material;
import up.raytracer.scene.Scene;
import up.raytracer.scene.Triangle;

import java.awt.Color;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Camera camera = new Camera(
                new Vector3D(0.00, 2.76, 3.74),
                new Vector3D(-0.45, -1.76, -2.64),
                new Vector3D(0.0, 1.0, 0.0),
                4096,
                2160,
                38.0,
                0.1000,
                1000.0000,
                0.0040,
                3.20,
                3
        );
        Scene scene = new Scene(camera, new Color(8, 9, 12));
        scene.setFog(new Color(16, 17, 19), 0.003);

        Material fallback = Material.cookTorrance(
                new Color(160, 160, 160),
                0.45,
                0.0,
                0.05
        );

        for (Triangle t : OBJReader.load(
                "assets/blender_scenes/scene_02/render02 (1).obj",
                fallback,
                new Vector3D(0.0, 0.0, 0.0),
                new Vector3D(0, 0, 0),
                1.0
        )) {
            scene.addObject(t);
        }

        scene.addLight(new AreaLight(
                new Color(255, 232, 205),
                20.0,
                new Vector3D(-3.35, 2.95, 3.20),
                new Vector3D(0.0, 0.0, 4.20),
                new Vector3D(0.0, 1.70, 0.0),
                64
        ));

        scene.addLight(new AreaLight(
                new Color(170, 205, 255),
                5.0,
                new Vector3D(1.65, 2.05, 0.85),
                new Vector3D(1.40, 0.0, 0.0),
                new Vector3D(0.0, 1.00, 0.0),
                16
        ));

        scene.addLight(new AreaLight(
                new Color(255, 190, 120),
                2.0,
                new Vector3D(0.65, 1.18, 0.50),
                new Vector3D(0.34, 0.0, 0.0),
                new Vector3D(0.0, 0.45, 0.0),
                9
        ));

        scene.addLight(new AreaLight(
                new Color(190, 220, 255),
                1.2,
                new Vector3D(-1.55, 0.95, 1.60),
                new Vector3D(0.36, 0.0, 0.0),
                new Vector3D(0.0, 0.36, 0.0),
                9
        ));

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(4), "render02.png");
    }
}
