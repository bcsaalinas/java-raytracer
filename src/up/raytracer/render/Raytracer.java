package up.raytracer.render;

import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;
import up.raytracer.light.Light;
import up.raytracer.scene.Object3D;
import up.raytracer.scene.Scene;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

// shoots rays through every pixel and paints the result into an image
public class Raytracer {

    private final Scene scene;

    public Raytracer(Scene scene) {
        this.scene = scene;
    }

    public BufferedImage render() {
        int width  = scene.getCamera().getWidth();
        int height = scene.getCamera().getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Ray ray = scene.getCamera().getRayForPixel(x, y);
                image.setRGB(x, y, trace(ray).getRGB());
            }
        }
        return image;
    }

    // returns the color of the closest object the ray hits within [near, far],
    // or the background color if nothing is hit
    private Color trace(Ray ray) {
        double near = scene.getCamera().getNear();
        double far  = scene.getCamera().getFar();
        Intersection closest = null;

        for (Object3D obj : scene.getObjects()) {
            Intersection hit = obj.calculateIntersection(ray);

            // skip hits outside the clipping range
            if (hit == null || hit.getDistance() < near || hit.getDistance() > far) continue;

            if (closest == null || hit.getDistance() < closest.getDistance()) {
                closest = hit;
            }
        }

        if (closest == null) return scene.getBackgroundColor();

        return shade(ray, closest);
    }

    // flat shading with Lambert diffuse lighting
    // each light adds color based on the object color, light color, intensity, and angle
    // the final channels are clamped to the 0..1 range used by Color floats
    private Color shade(Ray ray, Intersection hit) {
        Color    objectColor = hit.getObject().getColor();
        Vector3D N           = hit.getObject().getNormal(hit.getPosition());

        // triangle normals can point inward or outward depending on vertex order
        // if the normal points with the ray, flip it so the visible side can receive light
        if (N.dot(ray.getDirection()) > 0) N = N.negate();

        // keep color totals as 0..1 values while adding all light contributions
        double r = 0.0, g = 0.0, b = 0.0;

        for (Light light : scene.getLights()) {
            //L points from the surface toward the light, so negate the light travel direction
            Vector3D L     = light.getDirectionAt(hit.getPosition()).negate();
            double   NdotL = N.dot(L);

            //surfaces facing away from the light do not get diffuse light
            if (NdotL <= 0) continue;

            Color  lc = light.getColor();
            double li = light.getIntensity();

            //per channel: light color times object color times intensity times angle
            r += (lc.getRed()   / 255.0) * (objectColor.getRed()   / 255.0) * li * NdotL;
            g += (lc.getGreen() / 255.0) * (objectColor.getGreen() / 255.0) * li * NdotL;
            b += (lc.getBlue()  / 255.0) * (objectColor.getBlue()  / 255.0) * li * NdotL;
        }

        return new Color(clamp01(r), clamp01(g), clamp01(b));
    }

    // keeps a color channel inside the legal 0..1 range
    private static float clamp01(double v) {
        if (v < 0) return 0f;
        if (v > 1) return 1f;
        return (float) v;
    }

    public void saveImage(BufferedImage image, String path) throws IOException {
        ImageIO.write(image, "png", new File(path));
        System.out.println("saved: " + path);
    }
}
