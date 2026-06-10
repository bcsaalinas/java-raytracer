package up.raytracer.texture;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageTexture implements Texture {

    private final BufferedImage image;

    public ImageTexture(String path) throws IOException {
        this.image = ImageIO.read(new File(path));
    }

    @Override
    public Color sample(double u, double v) {
        double wrappedU = wrap(u);
        double wrappedV = wrap(v);

        int x = (int) Math.floor(wrappedU * (image.getWidth() - 1));
        int y = (int) Math.floor((1.0 - wrappedV) * (image.getHeight() - 1));

        return new Color(image.getRGB(x, y));
    }

    private double wrap(double value) {
        double wrapped = value - Math.floor(value);
        return wrapped < 0 ? wrapped + 1.0 : wrapped;
    }
}
