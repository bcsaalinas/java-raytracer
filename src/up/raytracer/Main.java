package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.io.OBJReader;
import up.raytracer.light.DirectionalLight;
import up.raytracer.render.Raytracer;
import up.raytracer.scene.Scene;
import up.raytracer.scene.Sphere;
import up.raytracer.scene.Triangle;

import java.awt.Color;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        // near = 0.1 avoids self-intersections, far = 1000 sets the visible range
        Camera camera = new Camera(new Vector3D(0, 0, -1), 800, 800, 60, 0.1, 1000.0);
        Scene  scene  = new Scene(camera, Color.BLACK);

        // directional light works like sunlight, all rays move in the same direction
        // this direction lights the objects from above and slightly from the front
        scene.addLight(new DirectionalLight(new Vector3D(-0.3, -1.0, 0.6), Color.WHITE, 1.0));

        // spheres flank the cube on either side, slightly in front
        scene.addObject(new Sphere(new Vector3D(-1.9, -0.3, 3.5), 0.6, Color.RED));
        scene.addObject(new Sphere(new Vector3D( 1.9, -0.3, 3.5), 0.6, Color.BLUE));

        // triangle sits behind the cube as a backdrop
        scene.addObject(new Triangle(
            new Vector3D( 0.0,  2.5, 9),
            new Vector3D(-3.0, -2.0, 9),
            new Vector3D( 3.0, -2.0, 9),
            Color.GREEN
        ));

        // the offset places the cube so the camera can see multiple faces
        // each face has its own normal, so flat shading gives each face a different brightness
        for (Triangle t : OBJReader.load("assets/cube.obj", Color.ORANGE, new Vector3D(-1.5, -1.5, 0))) {
            scene.addObject(t);
        }

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(), "output.png");
    }
}
