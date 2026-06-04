package up.raytracer.render;

import up.raytracer.camera.Camera;
import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;
import up.raytracer.light.AreaLight;
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
    private BVHNode bvh;
    public Raytracer(Scene scene) {
        this.scene = scene;
    }


    public BufferedImage render(int depth) {
        stats.startTimer();
        Camera camera = scene.getCamera();
        List<Object3D> objects = scene.getObjects();
        List<Light> lights = scene.getLights();

        // build once before rendering so every ray can reuse the same tree
        bvh = new BVHNode(objects);

        int width  = camera.getWidth();
        int height = camera.getHeight();
        int lensSamples = camera.getLensSamples();
        stats.setTotalPixels(width, height);
        stats.setRaysCast((long) width * height * lensSamples);
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
                       image.setRGB(x, y, renderPixel(camera, x, y, near, far, lights, localCounters, depth).getRGB());
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

    private Color renderPixel(
            Camera camera,
            int x,
            int y,
            double near,
            double far,
            List<Light> lights,
            long[] localCounters,
            int depth
    ) {
        double r = 0.0;
        double g = 0.0;
        double b = 0.0;
        int samples = camera.getLensSamples();

        // average several lens rays so only the focal distance stays sharp
        for (int i = 0; i < samples; i++) {
            Ray ray = camera.getRayForPixel(x, y, i);
            Color color = trace(ray, near, far, lights, localCounters, depth);
            r += color.getRed();
            g += color.getGreen();
            b += color.getBlue();
        }

        return new Color(
                clamp255(r / samples),
                clamp255(g / samples),
                clamp255(b / samples)
        );
    }

    // trace the closest hit in the camera clip range
    private Color trace(Ray ray, double near, double far, List<Light> lights, long[] localCounters, int depth) {
        //if the ray is out of bounces, then stop 
        if(depth <= 0) return scene.getBackgroundColor();

        Intersection closest = bvh.findClosest(ray, near, far, localCounters);

        if (closest == null) return scene.getBackgroundColor();

        localCounters[1]++;
        return applyFog(shade(ray, closest, lights, depth), closest.getDistance());
    }

    // lambert diffuse with interpolated normals from the hit object
    private Color shade(Ray ray, Intersection hit, List<Light> lights, int depth) {
        Material material = hit.getObject().getMaterial(); //object material for color and other properties

        Color objectColor = material.getColor(hit); // actual color of the object

        Vector3D surfaceNormal = material.getNormal(hit, hit.getObject().getNormal(hit)); // surface normal at the hit point
        Vector3D N = surfaceNormal;

        // keep the visible side lit even if triangle winding flips the normal
        if (N.dot(ray.getDirection()) > 0) N = N.negate();

        // small ambient term keeps scene readable while still letting specular dominate
        double r = (objectColor.getRed() / 255.0) * AMBIENT_STRENGTH;
        double g = (objectColor.getGreen() / 255.0) * AMBIENT_STRENGTH;
        double b = (objectColor.getBlue() / 255.0) * AMBIENT_STRENGTH;

        for (Light light : lights) {
            double[] lightColor = shadeLight(ray, hit, material, objectColor, N, light);
            r += lightColor[0];
            g += lightColor[1];
            b += lightColor[2];
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
            reflectedColor = trace(reflectedRay, scene.getCamera().getNear(), scene.getCamera().getFar(), scene.getLights(), new long[2], depth - 1);
        }

        Color refractedColor = null;
        if (refractionAmount > 0) {
            Vector3D T = refract(ray.getDirection(), surfaceNormal, material.getRefractiveIndex());
            if (T == null) {
                reflectionAmount = clamp01Double(reflectionAmount + refractionAmount);
                refractionAmount = 0;
            } else {
                Ray refractedRay = new Ray(hit.getPosition().add(T.scale(SHADOW_EPSILON)), T);
                refractedColor = trace(refractedRay, scene.getCamera().getNear(), scene.getCamera().getFar(), scene.getLights(), new long[2], depth - 1);
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

    private double[] shadeLight(Ray ray, Intersection hit, Material material, Color objectColor, Vector3D N, Light light) {
        if (light instanceof AreaLight areaLight) {
            double[] color = new double[] {0.0, 0.0, 0.0};

            // each sample acts like a small point light on the area light surface
            for (int i = 0; i < areaLight.getSamples(); i++) {
                double[] sampleColor = shadeLightSample(
                        ray,
                        hit,
                        material,
                        objectColor,
                        N,
                        areaLight,
                        areaLight.getDirectionAt(hit.getPosition(), i).negate(),
                        areaLight.getAttenuatedIntensity(hit.getPosition(), i),
                        areaLight.getDistanceAt(hit.getPosition(), i) - SHADOW_EPSILON
                );
                color[0] += sampleColor[0];
                color[1] += sampleColor[1];
                color[2] += sampleColor[2];
            }

            color[0] /= areaLight.getSamples();
            color[1] /= areaLight.getSamples();
            color[2] /= areaLight.getSamples();
            return color;
        }

        double maxShadowDistance = Double.POSITIVE_INFINITY;
        if (light instanceof PointLight pointLight) {
            maxShadowDistance = pointLight.getDistanceAt(hit.getPosition()) - SHADOW_EPSILON;
        }

        return shadeLightSample(
                ray,
                hit,
                material,
                objectColor,
                N,
                light,
                light.getDirectionAt(hit.getPosition()).negate(),
                light.getAttenuatedIntensity(hit.getPosition()),
                maxShadowDistance
        );
    }

    private double[] shadeLightSample(
            Ray ray,
            Intersection hit,
            Material material,
            Color objectColor,
            Vector3D N,
            Light light,
            Vector3D L,
            double li,
            double maxShadowDistance
    ) {
        double NdotL = N.dot(L);
        if (NdotL <= 0) return new double[] {0.0, 0.0, 0.0};

        if (isInShadow(hit, N, L, maxShadowDistance)) return new double[] {0.0, 0.0, 0.0};

        Vector3D V = ray.getDirection().negate();
        double NdotV = Math.max(N.dot(V), 1e-6);
        Vector3D halfVector = L.add(V);
        double halfMagnitude = halfVector.magnitude();
        if (halfMagnitude < 1e-8) return new double[] {0.0, 0.0, 0.0};
        Vector3D H = halfVector.scale(1.0 / halfMagnitude);

        // pbr materials use cook-torrance, older materials keep blinn-phong
        if (material.usesCookTorrance()) {
            return shadeCookTorrance(material, objectColor, light.getColor(), N, L, V, H, li, NdotL, NdotV);
        }

        Color lc = light.getColor();
        double r = 0.0;
        double g = 0.0;
        double b = 0.0;

        double diffuse = li * NdotL;
        r += (lc.getRed() / 255.0) * (objectColor.getRed() / 255.0) * diffuse;
        g += (lc.getGreen() / 255.0) * (objectColor.getGreen() / 255.0) * diffuse;
        b += (lc.getBlue() / 255.0) * (objectColor.getBlue() / 255.0) * diffuse;

        double shininess = material.getShininess();
        double ks = material.getSpecularCoefficient();
        double NdotH = N.dot(H);
        double specular = ks * Math.pow(Math.max(NdotH, 0), shininess) * li;
        r += (lc.getRed() / 255.0) * specular;
        g += (lc.getGreen() / 255.0) * specular;
        b += (lc.getBlue() / 255.0) * specular;

        return new double[] {r, g, b};
    }

    private double[] shadeCookTorrance(
            Material material,
            Color objectColor,
            Color lightColor,
            Vector3D N,
            Vector3D L,
            Vector3D V,
            Vector3D H,
            double li,
            double NdotL,
            double NdotV
    ) {
        double roughness = material.getRoughness();
        double metallic = material.getMetallic();

        double NdotH = Math.max(N.dot(H), 0.0);
        double VdotH = Math.max(V.dot(H), 0.0);

        double d = ggxDistribution(NdotH, roughness);
        double g = smithGeometry(NdotV, NdotL, roughness);

        double albedoR = objectColor.getRed() / 255.0;
        double albedoG = objectColor.getGreen() / 255.0;
        double albedoB = objectColor.getBlue() / 255.0;

        double f0R = blend(0.04, albedoR, metallic);
        double f0G = blend(0.04, albedoG, metallic);
        double f0B = blend(0.04, albedoB, metallic);

        double fR = schlickFresnel(VdotH, f0R);
        double fG = schlickFresnel(VdotH, f0G);
        double fB = schlickFresnel(VdotH, f0B);

        double denominator = Math.max(4.0 * NdotV * NdotL, 1e-6);
        double specularBase = d * g / denominator;
        double diffuseBase = (1.0 - metallic) / Math.PI;
        double energy = li * NdotL;

        double lr = lightColor.getRed() / 255.0;
        double lg = lightColor.getGreen() / 255.0;
        double lb = lightColor.getBlue() / 255.0;

        // cook-torrance uses fresnel to split energy between diffuse and specular
        double r = lr * energy * ((1.0 - fR) * albedoR * diffuseBase + fR * specularBase);
        double gg = lg * energy * ((1.0 - fG) * albedoG * diffuseBase + fG * specularBase);
        double b = lb * energy * ((1.0 - fB) * albedoB * diffuseBase + fB * specularBase);

        return new double[] {r, gg, b};
    }

    private static double ggxDistribution(double NdotH, double roughness) {
        // ggx controls how wide or tight the microfacet highlight is
        double a = roughness * roughness;
        double a2 = a * a;
        double value = NdotH * NdotH * (a2 - 1.0) + 1.0;
        return a2 / (Math.PI * value * value);
    }

    private static double smithGeometry(double NdotV, double NdotL, double roughness) {
        // smith geometry reduces light when microfacets block each other
        double k = Math.pow(roughness + 1.0, 2.0) / 8.0;
        double gv = NdotV / (NdotV * (1.0 - k) + k);
        double gl = NdotL / (NdotL * (1.0 - k) + k);
        return gv * gl;
    }

    private static double schlickFresnel(double cosTheta, double f0) {
        // schlick fresnel makes grazing angles more reflective
        return f0 + (1.0 - f0) * Math.pow(1.0 - clamp01Double(cosTheta), 5.0);
    }

    private boolean isInShadow(Intersection hit, Vector3D N, Vector3D L, double maxShadowDistance) {
        Vector3D shadowOrigin = hit.getPosition().add(N.scale(SHADOW_EPSILON));
        Ray shadowRay = new Ray(shadowOrigin, L);

        // bvh avoids checking every triangle for every shadow sample
        return bvh.isOccluded(shadowRay, maxShadowDistance, hit.getObject());
    }

    private Color applyFog(Color color, double distance) {
        if (!scene.hasFog()) return color;

        // exponential fog gets denser with distance but never fully hides the object
        double fogAmount = 1.0 - Math.exp(-scene.getFogDensity() * distance);
        fogAmount = clamp01Double(fogAmount);

        Color fogColor = scene.getFogColor();
        double r = blend(color.getRed(), fogColor.getRed(), fogAmount);
        double g = blend(color.getGreen(), fogColor.getGreen(), fogAmount);
        double b = blend(color.getBlue(), fogColor.getBlue(), fogAmount);

        return new Color(clamp255(r), clamp255(g), clamp255(b));
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

    private static int clamp255(double v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return (int) Math.round(v);
    }

    private static double blend(double a, double b, double amount) {
        return a * (1.0 - amount) + b * amount;
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
