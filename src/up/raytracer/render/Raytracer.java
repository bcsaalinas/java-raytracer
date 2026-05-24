package up.raytracer.render;

import up.raytracer.camera.Camera;
import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;
import up.raytracer.light.Light;
import up.raytracer.light.PointLight;
import up.raytracer.scene.Material;
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
    private static final double SHADOW_EPSILON = 1e-4;
    private static final double AMBIENT_STRENGTH = 0.05;

    private final Scene scene;
    private final RenderStats stats = new RenderStats();
    public Raytracer(Scene scene) {
        this.scene = scene;
    }


    public BufferedImage render(int depth) {
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
                       image.setRGB(x, y, trace(ray, near, far, objects, lights, localCounters, depth).getRGB());
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
    private Color trace(Ray ray, double near, double far, List<Object3D> objects, List<Light> lights, long[] localCounters, int depth) {
        //if the ray is out of bounces, then stop 
        if(depth <= 0) return scene.getBackgroundColor();

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
        return shade(ray, closest, lights, depth);
    }

    // lambert diffuse with interpolated normals from the hit object
    private Color shade(Ray ray, Intersection hit, List<Light> lights, int depth) {
        Material material = hit.getObject().getMaterial(); //object material for color and other properties

        Color objectColor = material.getColor(); // actual color of the object

        Vector3D N = hit.getObject().getNormal(hit); // surface normal at the hit point

        // keep the visible side lit even if triangle winding flips the normal
        if (N.dot(ray.getDirection()) > 0) N = N.negate();

        // small ambient term keeps scene readable while still letting specular dominate
        double r = (objectColor.getRed() / 255.0) * AMBIENT_STRENGTH;
        double g = (objectColor.getGreen() / 255.0) * AMBIENT_STRENGTH;
        double b = (objectColor.getBlue() / 255.0) * AMBIENT_STRENGTH;

        for (Light light : lights) {

            Vector3D L = light.getDirectionAt(hit.getPosition()).negate();

            double NdotL = N.dot(L); //lambert cosine term, also used for shadow ray direction

            boolean inShadow = false;

            if (NdotL <= 0) continue;


            Vector3D V = ray.getDirection().negate(); //direction from hit to camera
            Vector3D halfVector = L.add(V); // blinn-phong half-vector
            double halfMagnitude = halfVector.magnitude();
            if (halfMagnitude < 1e-8) continue;
            Vector3D H = halfVector.scale(1.0 / halfMagnitude);

            // start shadow ray slightly above surface to avoid self-hit acne
            Vector3D shadowOrigin = hit.getPosition().add(N.scale(SHADOW_EPSILON));
            Ray shadowRay = new Ray(shadowOrigin, L);
            double maxShadowDistance = Double.POSITIVE_INFINITY;
            if (light instanceof PointLight pointLight) {
                // point lights should only be blocked by objects between point and light
                maxShadowDistance = pointLight.getDistanceAt(hit.getPosition()) - SHADOW_EPSILON;
            }

            //check for shadows
            for (Object3D obj : scene.getObjects()){
                if (obj == hit.getObject()) continue;
                Intersection shadowHit = obj.calculateIntersection(shadowRay);
                if (shadowHit == null) continue;
                double t = shadowHit.getDistance();
                if (t > SHADOW_EPSILON && t < maxShadowDistance) {
                    inShadow = true;
                    break;
                }
            }

            if (inShadow) continue;




            Color  lc = light.getColor();

            //diffuse terms
            double li = light.getAttenuatedIntensity(hit.getPosition());
            double diffuse = li * NdotL;
            r += (lc.getRed() / 255.0) * (objectColor.getRed() / 255.0) * diffuse;
            g += (lc.getGreen() / 255.0) * (objectColor.getGreen() / 255.0) * diffuse;
            b += (lc.getBlue() / 255.0) * (objectColor.getBlue() / 255.0) * diffuse;

            //specular term, theyre separated because they use different material properties, and the specular color is usually white or the light color instead of the object color
            double shininess = material.getShininess();
            double ks = material.getSpecularCoefficient(); //specular coefficient, how much the specular highlight contributes to the final color
            double NdotH = N.dot(H);
            double specular = ks * Math.pow(Math.max(NdotH, 0), shininess) * li;
            r += (lc.getRed() / 255.0) * specular;
            g += (lc.getGreen() / 255.0) * specular;
            b += (lc.getBlue() / 255.0) * specular;
        }

        if(material.getReflectivity() > 0){

            Vector3D R = ray.getDirection().subtract(N.scale(2 * ray.getDirection().dot(N))).normalize(); // perfect reflection direction
            Ray reflectedRay = new Ray(hit.getPosition().add(N.scale(SHADOW_EPSILON)), R); // start reflected ray slightly above surface

            //recursive tracing with new reflected ray
            Color reflcolor = trace(reflectedRay, scene.getCamera().getNear(), scene.getCamera().getFar(), scene.getObjects(), scene.getLights(), new long[2], depth - 1);
            double refl = material.getReflectivity();
            r = (1-refl) * r + refl * (reflcolor.getRed() / 255.0);
            g = (1-refl) * g + refl * (reflcolor.getGreen() / 255.0);
            b = (1-refl) * b + refl * (reflcolor.getBlue() / 255.0);

            return new Color(clamp01(r), clamp01(g), clamp01(b));
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
