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
        Camera camera = new Camera(new Vector3D(0, 0, -1), 1080, 1080, 60, 0.1, 1000.0);
        Scene  scene  = new Scene(camera, Color.BLACK);

        scene.addLight(new PointLight(Color.white, 1.0, new Vector3D(1,4,5)));

        for (Triangle t : OBJReader.load(
                "assets/utah_teapot2.obj", //render a new obj because why not
                new Color(255, 0, 0),
                new Vector3D(-0.3, -2.0, 5.0),
                1.0
                )){
            scene.addObject(t);
        }

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(), "output.png");
    }
}
