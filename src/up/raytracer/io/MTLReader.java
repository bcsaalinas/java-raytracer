package up.raytracer.io;

import up.raytracer.scene.Material;
import up.raytracer.texture.ImageTexture;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MTLReader {

    // returns materials by name so obj usemtl lines can pick them later
    public static Map<String, Material> load(String path) throws IOException {
        Map<String, Material> materials = new HashMap<>();
        File file = new File(path);
        File baseDir = file.getParentFile();

        String currentName = null;
        MaterialBuilder builder = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");

                if (parts[0].equals("newmtl")) {
                    // save the previous material before starting the next one
                    if (currentName != null) {
                        materials.put(currentName, builder.build(baseDir, currentName));
                    }
                    currentName = parts[1];
                    builder = new MaterialBuilder();

                } else if (builder != null) {
                    readMaterialLine(builder, parts);
                }
            }
        }

        if (currentName != null) {
            materials.put(currentName, builder.build(baseDir, currentName));
        }

        return materials;
    }

    // blender exports many classic mtl fields, and may also export pbr extensions
    private static void readMaterialLine(MaterialBuilder builder, String[] parts) {
        if (parts[0].equals("Kd") && parts.length >= 4) {
            builder.color = readColor(parts);
        } else if (parts[0].equals("Ns") && parts.length >= 2) {
            builder.shininess = Float.parseFloat(parts[1]);
        } else if (parts[0].equals("Ks") && parts.length >= 4) {
            builder.specular = Math.max(0.0, Math.max(Double.parseDouble(parts[1]), Math.max(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]))));
        } else if (parts[0].equals("Ni") && parts.length >= 2) {
            builder.refractiveIndex = Double.parseDouble(parts[1]);
        } else if (parts[0].equals("d") && parts.length >= 2) {
            builder.transparency = 1.0 - Double.parseDouble(parts[1]);
        } else if (parts[0].equals("Tr") && parts.length >= 2) {
            builder.transparency = Double.parseDouble(parts[1]);
        } else if (parts[0].equals("Pr") && parts.length >= 2) {
            builder.roughness = Double.parseDouble(parts[1]);
            builder.useCookTorrance = true;
        } else if (parts[0].equals("Pm") && parts.length >= 2) {
            builder.metallic = Double.parseDouble(parts[1]);
            builder.useCookTorrance = true;
        } else if (parts[0].equals("map_Kd") && parts.length >= 2) {
            builder.texturePath = getMapPath(parts);
        } else if ((parts[0].equals("map_Bump") || parts[0].equals("bump") || parts[0].equals("norm")) && parts.length >= 2) {
            builder.normalMapPath = getMapPath(parts);
        }
    }

    private static Color readColor(String[] parts) {
        int r = toColor(Double.parseDouble(parts[1]));
        int g = toColor(Double.parseDouble(parts[2]));
        int b = toColor(Double.parseDouble(parts[3]));
        return new Color(r, g, b);
    }

    private static int toColor(double value) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, value)) * 255.0);
    }

    private static String getMapPath(String[] parts) {
        // map lines can contain options, so the image path is usually the last token
        return parts[parts.length - 1];
    }

    private static String resolvePath(File baseDir, String path) {
        String cleanPath = path.replace('\\', '/');
        File file = new File(cleanPath);
        if (file.exists()) return file.getPath();

        if (baseDir != null) {
            File relativeFile = new File(baseDir, cleanPath);
            if (relativeFile.exists()) return relativeFile.getPath();

            // blender may export absolute paths from another machine, so try the local texture folder
            File textureFile = new File(new File(baseDir, "textures"), file.getName());
            if (textureFile.exists()) return textureFile.getPath();

            File[] textureFiles = new File(baseDir, "textures").listFiles();
            if (textureFiles != null) {
                for (File candidate : textureFiles) {
                    if (candidate.getName().endsWith(file.getName())) {
                        return candidate.getPath();
                    }
                }
            }
        }

        return file.getPath();
    }

    private static class MaterialBuilder {
        private Color color = new Color(200, 200, 200);
        private float shininess = 80f;
        private double specular = 0.5;
        private double transparency = 0.0;
        private double refractiveIndex = 1.0;
        private double roughness = 0.45;
        private double metallic = 0.0;
        private double reflectivity = 0.0;
        private boolean useCookTorrance = false;
        private String texturePath;
        private String normalMapPath;

        public Material build(File baseDir, String name) throws IOException {
            applyNameDefaults(name);
            Material material;

            // pbr mtl extensions switch the material into cook-torrance mode
            if (useCookTorrance) {
                material = new Material(color, shininess, reflectivity, (float) specular, transparency, refractiveIndex, roughness, metallic, true);
            } else {
                material = new Material(color, shininess, reflectivity, (float) specular, transparency, refractiveIndex);
            }

            if (texturePath != null) {
                String texPath = resolvePath(baseDir, texturePath);
                if (new File(texPath).exists()) {
                    material.withTexture(new ImageTexture(texPath));
                }
            }

            if (normalMapPath != null) {
                String normalPath = resolvePath(baseDir, normalMapPath);
                if (new File(normalPath).exists()) {
                    material.withNormalMap(new ImageTexture(normalPath));
                }
            }

            return material;
        }

        private void applyNameDefaults(String name) {
            String lowerName = name.toLowerCase();

            // blender obj exports material names, but not real light transport settings
            if (lowerName.contains("mirror")) {
                roughness = 0.03;
                metallic = 1.0;
                reflectivity = 0.85;
                specular = 1.0;
                useCookTorrance = true;
            } else if (lowerName.contains("glass")) {
                roughness = 0.03;
                reflectivity = 0.08;
                transparency = Math.max(transparency, 0.72);
                refractiveIndex = Math.max(refractiveIndex, 1.45);
                specular = 1.0;
                useCookTorrance = true;
            } else if (lowerName.contains("pfeifen")) {
                color = new Color(210, 155, 78);
                roughness = 0.18;
                metallic = 0.65;
                reflectivity = 0.03;
                specular = 1.0;
                texturePath = null;
                useCookTorrance = true;
            } else if (lowerName.contains("gold")) {
                roughness = 0.20;
                metallic = 0.55;
                reflectivity = 0.04;
                specular = 1.0;
                useCookTorrance = true;
            } else if (lowerName.equals("dagger.002")) {
                roughness = 0.10;
                metallic = 0.20;
                reflectivity = 0.04;
                transparency = Math.max(transparency, 0.18);
                refractiveIndex = Math.max(refractiveIndex, 1.45);
                specular = 1.0;
                useCookTorrance = true;
            } else if (lowerName.contains("dagger")) {
                roughness = 0.16;
                metallic = 0.45;
                reflectivity = 0.04;
                specular = 1.0;
                useCookTorrance = true;
            } else if (lowerName.contains("wire_224") || lowerName.contains("heart")) {
                roughness = 0.22;
                reflectivity = 0.03;
                specular = 1.0;
                useCookTorrance = true;
            } else if (lowerName.contains("robe")) {
                roughness = 0.70;
                reflectivity = 0.0;
                specular = 0.25;
                useCookTorrance = true;
            }
        }
    }
}
