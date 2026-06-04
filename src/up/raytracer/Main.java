package up.raytracer;

import up.raytracer.camera.Camera;
import up.raytracer.core.Vector3D;
import up.raytracer.io.OBJReader;
import up.raytracer.light.AreaLight;
import up.raytracer.render.Raytracer;
import up.raytracer.scene.Material;
import up.raytracer.scene.Scene;
import up.raytracer.scene.Triangle;

import java.awt.Color;
import java.io.IOException;

public class Main {
    private static final String CHESS_OBJ = "assets/blender_scenes/scene_03/12926_Wooden_Chess_King_Side_A_v1_l3.obj";
    private static final Vector3D CHESS_BASE_CENTER = new Vector3D(2.33295, -16.11515, -0.0009);

    public static void main(String[] args) throws IOException {
        Camera camera = new Camera(
                new Vector3D(0.20, -30.00, 8.20),
                new Vector3D(-0.20, 30.00, -2.80),
                new Vector3D(0.0, 0.0, 1.0),
                4096,
                2160,
                42.0,
                0.1000,
                1000.0000,
                0.045,
                30.0,
                5
        );
        Scene scene = new Scene(camera, new Color(10, 12, 15));

        Material warmWood = Material.cookTorrance(
                new Color(142, 84, 38),
                0.30,
                0.0,
                0.08
        );
        Material darkWood = Material.cookTorrance(
                new Color(54, 28, 18),
                0.36,
                0.0,
                0.06
        );
        Material amberWood = Material.cookTorrance(
                new Color(202, 128, 48),
                0.26,
                0.0,
                0.10
        );
        Material boardLight = Material.cookTorrance(
                new Color(194, 170, 126),
                0.48,
                0.0,
                0.03
        );
        Material boardDark = Material.cookTorrance(
                new Color(38, 42, 47),
                0.42,
                0.0,
                0.05
        );
        Material stage = Material.cookTorrance(
                new Color(24, 27, 31),
                0.58,
                0.0,
                0.04
        );
        Material backdrop = Material.cookTorrance(
                new Color(11, 14, 18),
                0.82,
                0.0,
                0.01
        );

        addBoard(scene, -7.2, -5.8, 14.4, 14.4, boardLight, boardDark);
        addQuad(scene,
                new Vector3D(-10.5, -9.2, -0.05),
                new Vector3D(10.5, -9.2, -0.05),
                new Vector3D(10.5, 9.4, -0.05),
                new Vector3D(-10.5, 9.4, -0.05),
                stage
        );
        addQuad(scene,
                new Vector3D(-10.5, 9.4, -0.05),
                new Vector3D(10.5, 9.4, -0.05),
                new Vector3D(10.5, 9.4, 12.5),
                new Vector3D(-10.5, 9.4, 12.5),
                backdrop
        );

        addChessPiece(scene, warmWood, 0.0, -0.8, 1.00, 0.0);
        addChessPiece(scene, darkWood, -5.2, 2.0, 0.74, 18.0);
        addChessPiece(scene, amberWood, 5.0, 2.4, 0.72, -15.0);
        addChessPiece(scene, darkWood, -2.8, 5.9, 0.48, 8.0);
        addChessPiece(scene, warmWood, 2.7, 6.2, 0.46, -10.0);

        scene.addLight(new AreaLight(
                new Color(255, 232, 205),
                780.0,
                new Vector3D(-5.8, -8.5, 13.5),
                new Vector3D(4.8, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 3.8),
                16
        ));

        scene.addLight(new AreaLight(
                new Color(170, 205, 255),
                220.0,
                new Vector3D(7.5, -1.5, 8.8),
                new Vector3D(2.8, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 2.4),
                9
        ));

        scene.addLight(new AreaLight(
                new Color(255, 190, 120),
                110.0,
                new Vector3D(-1.5, -9.0, 4.2),
                new Vector3D(2.0, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 1.8),
                4
        ));

        scene.addLight(new AreaLight(
                new Color(190, 220, 255),
                75.0,
                new Vector3D(0.0, 7.8, 8.6),
                new Vector3D(5.0, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 2.0),
                4
        ));

        Raytracer raytracer = new Raytracer(scene);
        raytracer.saveImage(raytracer.render(3), "render03-chess.png");
    }

    private static void addChessPiece(
            Scene scene,
            Material material,
            double x,
            double y,
            double scale,
            double zRotation
    ) throws IOException {
        double radians = Math.toRadians(zRotation);
        double scaledX = CHESS_BASE_CENTER.x * scale;
        double scaledY = CHESS_BASE_CENTER.y * scale;
        double rotatedX = scaledX * Math.cos(radians) - scaledY * Math.sin(radians);
        double rotatedY = scaledX * Math.sin(radians) + scaledY * Math.cos(radians);
        Vector3D offset = new Vector3D(
                x - rotatedX,
                y - rotatedY,
                -CHESS_BASE_CENTER.z * scale
        );

        for (Triangle triangle : OBJReader.load(CHESS_OBJ, material, offset, new Vector3D(0, 0, zRotation), scale)) {
            scene.addObject(triangle);
        }
    }

    private static void addBoard(
            Scene scene,
            double startX,
            double startY,
            double width,
            double depth,
            Material light,
            Material dark
    ) {
        double tileWidth = width / 8.0;
        double tileDepth = depth / 8.0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                double x0 = startX + col * tileWidth;
                double y0 = startY + row * tileDepth;
                double x1 = x0 + tileWidth;
                double y1 = y0 + tileDepth;
                addQuad(scene,
                        new Vector3D(x0, y0, 0.0),
                        new Vector3D(x1, y0, 0.0),
                        new Vector3D(x1, y1, 0.0),
                        new Vector3D(x0, y1, 0.0),
                        ((row + col) % 2 == 0) ? light : dark
                );
            }
        }
    }

    private static void addQuad(Scene scene, Vector3D a, Vector3D b, Vector3D c, Vector3D d, Material material) {
        scene.addObject(new Triangle(a, b, c, material));
        scene.addObject(new Triangle(a, c, d, material));
    }
}
