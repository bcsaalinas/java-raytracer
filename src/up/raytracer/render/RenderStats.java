package up.raytracer.render;

//stats for debugging and testing, not used in the actual rendering
public class RenderStats {

    private long raysCast;
    private long intersectionsTested;
    private long intersectionsFound;
    private long totalPixels;
    private long startTime;
    private long endTime;



    public void setTotalPixels(int width, int height) {
        this.totalPixels = (long) width * height;
    }

    public void setRaysCast(long raysCast) {
        this.raysCast = raysCast;
    }

    public void setIntersectionCounts(long intersectionsTested, long intersectionsFound) {
        this.intersectionsTested = intersectionsTested;
        this.intersectionsFound = intersectionsFound;
    }

    public void startTimer(){
       startTime =  System.nanoTime();
    }

    public void stopTimer(){
       endTime = System.nanoTime();
    }

    @Override
    public String toString() {
        double elapsed = getElapsedSeconds();
        //calculate rays per second in millions
        double mraysPerSec = elapsed > 0.0 ? (raysCast / elapsed) / 1_000_000.0 : 0.0;
        double testsPerRay = raysCast > 0 ? (double) intersectionsTested / raysCast : 0.0;
        //check for division by zero and calculate hit rate as a percentage
        double hitRate = raysCast > 0 ? (double) intersectionsFound / raysCast : 0.0;
        return String.format(
                "Pixels: %d, Rays: %d, Hits: %d (%.2f%%), Tests: %d, Tests/Ray: %.2f, Throughput: %.3f Mray/s, Time: %.4f s",
                totalPixels,
                raysCast,
                intersectionsFound,
                hitRate * 100.0,
                intersectionsTested,
                testsPerRay,
                mraysPerSec,
                elapsed
        );
    }

    public double getElapsedSeconds() {
        return (endTime - startTime) / 1_000_000_000.0;
    }
}
