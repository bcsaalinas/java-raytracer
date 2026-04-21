package up.raytracer.render;

import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.scene.Object3D;
import up.raytracer.scene.Scene;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

//shoots rays through every pixel and paints the result into an image
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

    //returns the color of whatever the ray hits first, or the background color
    private Color trace(Ray ray) {
        Intersection closest = null;

        for (Object3D obj : scene.getObjects()) {
            Intersection hit = obj.calculateIntersection(ray);
            if (hit != null && (closest == null || hit.getDistance() < closest.getDistance())) {
                closest = hit;
            }
        }

        return closest != null ? closest.getObject().getColor() : scene.getBackgroundColor();
    }

    public void saveImage(BufferedImage image, String path) throws IOException {
        ImageIO.write(image, "png", new File(path));
        System.out.println("saved: " + path);
    }
}
