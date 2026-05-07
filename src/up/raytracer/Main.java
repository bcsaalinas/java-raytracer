package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.io.OBJReader;
import up.raytracer.light.DirectionalLight;
import up.raytracer.render.Raytracer;
import up.raytracer.scene.Scene;
import up.raytracer.scene.Triangle;

import java.awt.Color;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Camera camera = new Camera(new Vector3D(0, 0, -1), 1080, 1080, 60, 0.1, 1000.0);
        Scene  scene  = new Scene(camera, Color.BLACK);

        // single key light for the v05 render
        scene.addLight(new DirectionalLight(new Vector3D(-0.3, -1.0, 0.4), Color.WHITE, 1.0));

        // bunny.obj has no vn lines, so normals are derived for phong interpolation
        for (Triangle t : OBJReader.load(
                "assets/bunny.obj",
                new Color(210, 210, 210),
                new Vector3D(-0.17, -1.6, 2.5),
                new Vector3D(-10, 10, -10))) {
            scene.addObject(t);
        }

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(), "output.png");
    }
}
