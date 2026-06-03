package up.raytracer.texture;

import java.awt.Color;

public class WaveNormalTexture implements Texture {

    private final double frequency;
    private final double strength;

    public WaveNormalTexture(double frequency, double strength) {
        this.frequency = frequency;
        this.strength = strength;
    }

    @Override
    public Color sample(double u, double v) {
        double waveX = Math.sin(u * frequency * Math.PI * 2.0) * strength;
        double waveY = Math.cos(v * frequency * Math.PI * 2.0) * strength;
        double z = 1.0;

        double length = Math.sqrt(waveX * waveX + waveY * waveY + z * z);
        double nx = waveX / length;
        double ny = waveY / length;
        double nz = z / length;

        return new Color(encode(nx), encode(ny), encode(nz));
    }

    private int encode(double value) {
        return (int) Math.round((value * 0.5 + 0.5) * 255.0);
    }
}
