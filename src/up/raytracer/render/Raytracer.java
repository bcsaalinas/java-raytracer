package up.raytracer.render;

import com.sun.source.tree.UsesTree;
import up.raytracer.camera.Camera;
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
import java.util.ArrayList;
import java.util.List;

public class Raytracer{

    private final Scene scene;
    private final RenderStats stats = new RenderStats();
    public Raytracer(Scene scene) {
        this.scene = scene;
    }


    public BufferedImage render() {
        stats.startTimer();
        Camera camera = scene.getCamera();
        List<Object3D> objects = scene.getObjects();
        List<Light> lights = scene.getLights();

        int width  = camera.getWidth();
        int height = camera.getHeight();
        stats.setTotalPixels(width, height);
        stats.setRaysCast((long) width * height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        //amount of threads to use
        int threadCount = Runtime.getRuntime().availableProcessors();
        ArrayList<Thread> threads = new ArrayList<>();
        //per thread counters for intersections
        // I count how many intersections were tested and how many hits were found
        long[][] threadCounters = new long[threadCount][2]; //tested, found
        double near = camera.getNear();
        double far = camera.getFar();

        // create threads to render the image in parallel and check stats after all threads are done
        for (int i = 0; i < threadCount; i++) {
            int rowsPearThread = height/threadCount;
            int startY = i * rowsPearThread;
            int endY;


            if(i == threadCount -1){
                endY = height;
            } else {
                endY = startY + rowsPearThread;
            }

            // need to be final or effectively final to be used in the lambda for Runnable
            final int threadIndex = i;
            final int threadStarty = startY;
            final int threadEndY = endY;

           Runnable task = () -> {
               long[] localCounters = new long[] {0L, 0L}; // [tested, found]
               for (int y = threadStarty; y < threadEndY; y++) {
                   for (int x = 0; x < width; x++) {
                       Ray ray = camera.getRayForPixel(x, y);
                       image.setRGB(x, y, trace(ray, near, far, objects, lights, localCounters).getRGB());
                   }
               }
               threadCounters[threadIndex][0] = localCounters[0];
               threadCounters[threadIndex][1] = localCounters[1];
           };

            Thread thread = new Thread(task);
           threads.add(thread);
        }

        for (Thread thread : threads){
            thread.start();
        }

        try {
            for (Thread thread : threads){
                thread.join();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long intersectionsTested = 0;
        long intersectionsFound = 0;
        for (long[] threadCounter : threadCounters) {
            intersectionsTested += threadCounter[0];
            intersectionsFound += threadCounter[1];
        }
        stats.setIntersectionCounts(intersectionsTested, intersectionsFound);
        stats.stopTimer();
        System.out.println(stats);
        return image;
    }

    // trace the closest hit in the camera clip range
    private Color trace(Ray ray, double near, double far, List<Object3D> objects, List<Light> lights, long[] localCounters) {
        Intersection closest = null;

        for (Object3D obj : objects) {
            Intersection hit = obj.calculateIntersection(ray);
            localCounters[0]++;

            if (hit == null || hit.getDistance() < near || hit.getDistance() > far) continue;

            if (closest == null || hit.getDistance() < closest.getDistance()) {
                closest = hit;
            }
        }

        if (closest == null) return scene.getBackgroundColor();

        localCounters[1]++;
        return shade(ray, closest, lights);
    }

    // lambert diffuse with interpolated normals from the hit object
    private Color shade(Ray ray, Intersection hit, List<Light> lights) {
        Color objectColor = hit.getObject().getColor();
        Vector3D N = hit.getObject().getNormal(hit);

        // keep the visible side lit even if triangle winding flips the normal
        if (N.dot(ray.getDirection()) > 0) N = N.negate();

        double r = 0.0, g = 0.0, b = 0.0;

        for (Light light : lights) {
            Vector3D L = light.getDirectionAt(hit.getPosition()).negate();
            double NdotL = N.dot(L);

            if (NdotL <= 0) continue;

            //shadow ray, starts at the intersection point and goes toward the light
            Ray shadowRay = new Ray(hit.getPosition(), L);

            //check for shadows
            for (Object3D obj : scene.getObjects()){
                Intersection shadowHit = obj.calculateIntersection(shadowRay);
                //set 0.001 as epsilon to avoid self-intersections
                if (shadowHit != null && shadowHit.getDistance() > 0.001) {
                   continue;
                }
            }


            Color  lc = light.getColor();
            double li = light.getAttenuatedIntensity(hit.getPosition());
            r += (lc.getRed()   / 255.0) * (objectColor.getRed()   / 255.0) * li * NdotL;
            g += (lc.getGreen() / 255.0) * (objectColor.getGreen() / 255.0) * li * NdotL;
            b += (lc.getBlue()  / 255.0) * (objectColor.getBlue()  / 255.0) * li * NdotL;
        }

        return new Color(clamp01(r), clamp01(g), clamp01(b));
    }

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
