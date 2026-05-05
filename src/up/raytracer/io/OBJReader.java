package up.raytracer.io;

import up.raytracer.core.Vector3D;
import up.raytracer.scene.Triangle;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//reads a .obj file and turns it into a list of triangles
public class OBJReader {

    public static List<Triangle> load(String path, Color color) throws IOException {
        return load(path, color, new Vector3D(0, 0, 0));
    }

    // load with an offset so the same model can be placed in different parts of the scene
    public static List<Triangle> load(String path, Color color, Vector3D offset) throws IOException {
        List<Vector3D> vertices  = new ArrayList<>();
        List<Triangle> triangles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");

                if (parts[0].equals("v")) {
                    // vertex: v x y z, shifted by the offset before saving it
                    double x = Double.parseDouble(parts[1]) + offset.x;
                    double y = Double.parseDouble(parts[2]) + offset.y;
                    double z = Double.parseDouble(parts[3]) + offset.z;
                    vertices.add(new Vector3D(x, y, z));

                } else if (parts[0].equals("f")) {
                    //face f v/vt/vn  v/vt/vn  v/vt/vn  (also v, v//vn, v/vt)
                    // we only care about the vertex index, the first number
                    int[] indices = new int[parts.length - 1];
                    for (int i = 0; i < indices.length; i++) {
                        String token = parts[i + 1].split("/")[0];
                        // obj indices start at 1, java arrays start at 0
                        indices[i] = Integer.parseInt(token) - 1;
                    }

                    //fan triangulation works for triangles, quads, and any n-gon
                    for (int i = 1; i < indices.length - 1; i++) {
                        Vector3D a = vertices.get(indices[0]);
                        Vector3D b = vertices.get(indices[i]);
                        Vector3D c = vertices.get(indices[i + 1]);
                        triangles.add(new Triangle(a, b, c, color));
                    }
                }
                //vn, vt, o, g, s and anything else are ignored for now
            }
        }

        return triangles;
    }
}
