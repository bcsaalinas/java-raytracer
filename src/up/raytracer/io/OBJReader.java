package up.raytracer.io;

import up.raytracer.core.Transform;
import up.raytracer.core.Vector3D;
import up.raytracer.scene.Material;
import up.raytracer.scene.Triangle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OBJReader {

    public static List<Triangle> load(String path, Material material) throws IOException {
        return load(path, material, Transform.identity());
    }

    public static List<Triangle> load(String path, Material material, Vector3D offset) throws IOException {
        return load(path, material, Transform.fromDegrees(offset, new Vector3D(0, 0, 0), 1.0));
    }

    public static List<Triangle> load(String path, Material material, Vector3D offset, double scale) throws IOException {
        return load(path, material, Transform.fromDegrees(offset, new Vector3D(0, 0, 0), scale));
    }

    public static List<Triangle> load(String path, Material material, Vector3D offset, Vector3D scale) throws IOException {
        return load(path, material, Transform.fromDegrees(offset, new Vector3D(0, 0, 0), scale));
    }

    public static List<Triangle> load(
            String path,
            Material material,
            Vector3D offset,
            Vector3D rotation,
            double scale
    ) throws IOException {
        return load(path, material, Transform.fromDegrees(offset, rotation, scale));
    }

    public static List<Triangle> load(
            String path,
            Material material,
            Vector3D offset,
            Vector3D rotation,
            Vector3D scale
    ) throws IOException {
        return load(path, material, Transform.fromDegrees(offset, rotation, scale));
    }

    public static List<Triangle> load(String path, Material material, Transform transform) throws IOException {
        List<Vector3D> vertices = new ArrayList<>();
        List<Vector3D> normals = new ArrayList<>();
        List<int[]> faceVerts = new ArrayList<>();
        List<int[]> faceNormals = new ArrayList<>();
        List<Integer> faceSmoothingGroups = new ArrayList<>();

        int currentSmoothingGroup = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");

                if (parts[0].equals("v")) {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    vertices.add(transform.applyToPoint(new Vector3D(x, y, z)));

                } else if (parts[0].equals("vn")) {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    normals.add(transform.applyToNormal(new Vector3D(x, y, z)));

                } else if (parts[0].equals("f")) {
                    int tokenCount = parts.length - 1;
                    int[] vIdx = new int[tokenCount];
                    int[] nIdx = new int[tokenCount];
                    boolean haveAllNormals = true;

                    for (int i = 0; i < tokenCount; i++) {
                        String[] sub = parts[i + 1].split("/");
                        vIdx[i] = Integer.parseInt(sub[0]) - 1;
                        if (sub.length >= 3 && !sub[2].isEmpty()) {
                            nIdx[i] = Integer.parseInt(sub[2]) - 1;
                        } else {
                            nIdx[i] = -1;
                            haveAllNormals = false;
                        }
                    }

                    for (int i = 1; i < tokenCount - 1; i++) {
                        faceVerts.add(new int[] { vIdx[0], vIdx[i], vIdx[i + 1] });
                        faceNormals.add(haveAllNormals
                            ? new int[] { nIdx[0], nIdx[i], nIdx[i + 1] }
                            : null);
                        faceSmoothingGroups.add(currentSmoothingGroup);
                    }

                } else if (parts[0].equals("s")) {
                    if (parts.length < 2 || parts[1].equalsIgnoreCase("off") || parts[1].equals("0")) {
                        currentSmoothingGroup = 0;
                    } else {
                        currentSmoothingGroup = parseSmoothingGroup(parts[1]);
                    }
                }
            }
        }

        // only faces in the same smoothing group share derived normals
        Map<Long, Vector3D> derivedNormals = needsDerivedNormals(faceNormals)
            ? deriveVertexNormals(vertices, faceVerts, faceSmoothingGroups)
            : null;

        List<Triangle> triangles = new ArrayList<>(faceVerts.size());
        for (int i = 0; i < faceVerts.size(); i++) {
            int[] f = faceVerts.get(i);
            Vector3D a = vertices.get(f[0]);
            Vector3D b = vertices.get(f[1]);
            Vector3D c = vertices.get(f[2]);
            Vector3D faceNormal = faceNormal(a, b, c);
            int smoothingGroup = faceSmoothingGroups.get(i);

            Vector3D na, nb, nc;
            int[] fn = faceNormals.get(i);
            if (fn != null) {
                na = normals.get(fn[0]);
                nb = normals.get(fn[1]);
                nc = normals.get(fn[2]);
            } else if (smoothingGroup <= 0) {
                na = faceNormal;
                nb = faceNormal;
                nc = faceNormal;
            } else {
                na = getDerivedNormal(derivedNormals, f[0], smoothingGroup, faceNormal);
                nb = getDerivedNormal(derivedNormals, f[1], smoothingGroup, faceNormal);
                nc = getDerivedNormal(derivedNormals, f[2], smoothingGroup, faceNormal);
            }
            triangles.add(new Triangle(a, b, c, na, nb, nc, material));
        }

        return triangles;
    }

    private static boolean needsDerivedNormals(List<int[]> faceNormals) {
        for (int[] fn : faceNormals) {
            if (fn == null) return true;
        }
        return false;
    }

    // slide formula: n = (n1 + n2 + ... + nk) / |n1 + n2 + ... + nk|
    private static Map<Long, Vector3D> deriveVertexNormals(
            List<Vector3D> vertices,
            List<int[]> faceVerts,
            List<Integer> faceSmoothingGroups
    ) {
        Map<Long, Vector3D> accumulated = new HashMap<>();

        for (int i = 0; i < faceVerts.size(); i++) {
            int smoothingGroup = faceSmoothingGroups.get(i);
            if (smoothingGroup <= 0) continue;

            int[] f = faceVerts.get(i);
            Vector3D a = vertices.get(f[0]);
            Vector3D b = vertices.get(f[1]);
            Vector3D c = vertices.get(f[2]);
            Vector3D faceNormal = faceNormal(a, b, c);

            accumulateNormal(accumulated, f[0], smoothingGroup, faceNormal);
            accumulateNormal(accumulated, f[1], smoothingGroup, faceNormal);
            accumulateNormal(accumulated, f[2], smoothingGroup, faceNormal);
        }

        Map<Long, Vector3D> normalized = new HashMap<>(accumulated.size());
        for (Map.Entry<Long, Vector3D> entry : accumulated.entrySet()) {
            Vector3D sum = entry.getValue();
            if (sum.magnitude() > 1e-12) {
                normalized.put(entry.getKey(), sum.normalize());
            }
        }
        return normalized;
    }

    private static void accumulateNormal(
            Map<Long, Vector3D> accumulated,
            int vertexIndex,
            int smoothingGroup,
            Vector3D faceNormal
    ) {
        long key = normalKey(vertexIndex, smoothingGroup);
        Vector3D current = accumulated.get(key);
        accumulated.put(key, current == null ? faceNormal : current.add(faceNormal));
    }

    private static Vector3D getDerivedNormal(
            Map<Long, Vector3D> derivedNormals,
            int vertexIndex,
            int smoothingGroup,
            Vector3D fallback
    ) {
        Vector3D normal = derivedNormals.get(normalKey(vertexIndex, smoothingGroup));
        return normal == null ? fallback : normal;
    }

    private static int parseSmoothingGroup(String raw) {
        try {
            int group = Integer.parseInt(raw);
            return group > 0 ? group : 0;
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static long normalKey(int vertexIndex, int smoothingGroup) {
        return (((long) smoothingGroup) << 32) | (vertexIndex & 0xffffffffL);
    }

    private static Vector3D faceNormal(Vector3D a, Vector3D b, Vector3D c) {
        return b.subtract(a).cross(a.subtract(c)).normalize();
    }
}
