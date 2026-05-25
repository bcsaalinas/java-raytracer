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

        Vector3D surfaceNormal = hit.getObject().getNormal(hit); // surface normal at the hit point
        Vector3D N = surfaceNormal;

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

        double reflectionAmount = clamp01Double(material.getReflectivity());
        double refractionAmount = clamp01Double(material.getTransparency());

        if (refractionAmount > 0) {
            double fresnel = fresnel(ray.getDirection(), surfaceNormal, material.getRefractiveIndex());
            reflectionAmount = Math.max(reflectionAmount, fresnel * refractionAmount);
        }

        Color reflectedColor = null;
        if(reflectionAmount > 0){
            Vector3D R = reflect(ray.getDirection(), N);
            Ray reflectedRay = new Ray(hit.getPosition().add(N.scale(SHADOW_EPSILON)), R); // start reflected ray slightly above surface
            reflectedColor = trace(reflectedRay, scene.getCamera().getNear(), scene.getCamera().getFar(), scene.getObjects(), scene.getLights(), new long[2], depth - 1);
        }

        Color refractedColor = null;
        if (refractionAmount > 0) {
            Vector3D T = refract(ray.getDirection(), surfaceNormal, material.getRefractiveIndex());
            if (T == null) {
                reflectionAmount = clamp01Double(reflectionAmount + refractionAmount);
                refractionAmount = 0;
            } else {
                Ray refractedRay = new Ray(hit.getPosition().add(T.scale(SHADOW_EPSILON)), T);
                refractedColor = trace(refractedRay, scene.getCamera().getNear(), scene.getCamera().getFar(), scene.getObjects(), scene.getLights(), new long[2], depth - 1);
            }
        }

        if (reflectionAmount > 0 || refractionAmount > 0) {
            double localAmount = Math.max(0.0, 1.0 - reflectionAmount - refractionAmount);
            double totalAmount = localAmount + reflectionAmount + refractionAmount;

            r = r * localAmount;
            g = g * localAmount;
            b = b * localAmount;

            if (reflectedColor != null) {
                r += (reflectedColor.getRed() / 255.0) * reflectionAmount;
                g += (reflectedColor.getGreen() / 255.0) * reflectionAmount;
                b += (reflectedColor.getBlue() / 255.0) * reflectionAmount;
            }

            if (refractedColor != null) {
                r += (refractedColor.getRed() / 255.0) * refractionAmount;
                g += (refractedColor.getGreen() / 255.0) * refractionAmount;
                b += (refractedColor.getBlue() / 255.0) * refractionAmount;
            }

            r /= totalAmount;
            g /= totalAmount;
            b /= totalAmount;
        }




        return new Color(clamp01(r), clamp01(g), clamp01(b));
    }

    private static float clamp01(double v) {
        if (v < 0) return 0f;
        if (v > 1) return 1f;
        return (float) v;
    }

    private static double clamp01Double(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static Vector3D reflect(Vector3D direction, Vector3D normal) {
        return direction.subtract(normal.scale(2 * direction.dot(normal))).normalize();
    }

    private static Vector3D refract(Vector3D direction, Vector3D normal, double refractiveIndex) {
        double etaI = 1.0;
        double etaT = Math.max(1.0, refractiveIndex);
        Vector3D N = normal;
        double cosI = clamp(direction.dot(N), -1.0, 1.0);

        if (cosI < 0) {
            cosI = -cosI;
        } else {
            double temp = etaI;
            etaI = etaT;
            etaT = temp;
            N = N.negate();
        }

        double eta = etaI / etaT;
        double k = 1.0 - eta * eta * (1.0 - cosI * cosI);
        if (k < 0) return null;

        return direction.scale(eta).add(N.scale(eta * cosI - Math.sqrt(k))).normalize();
    }

    private static double fresnel(Vector3D direction, Vector3D normal, double refractiveIndex) {
        double etaI = 1.0;
        double etaT = Math.max(1.0, refractiveIndex);
        double cosI = clamp(direction.dot(normal), -1.0, 1.0);

        if (cosI > 0) {
            double temp = etaI;
            etaI = etaT;
            etaT = temp;
        }

        double sinT = etaI / etaT * Math.sqrt(Math.max(0.0, 1.0 - cosI * cosI));
        if (sinT >= 1.0) return 1.0;

        double cosT = Math.sqrt(Math.max(0.0, 1.0 - sinT * sinT));
        cosI = Math.abs(cosI);
        double rs = ((etaT * cosI) - (etaI * cosT)) / ((etaT * cosI) + (etaI * cosT));
        double rp = ((etaI * cosI) - (etaT * cosT)) / ((etaI * cosI) + (etaT * cosT));
        return (rs * rs + rp * rp) / 2.0;
    }

    public void saveImage(BufferedImage image, String path) throws IOException {
        ImageIO.write(image, "png", new File(path));
        System.out.println("saved: " + path);
    }
}
