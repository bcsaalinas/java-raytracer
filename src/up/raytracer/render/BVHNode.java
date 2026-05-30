package up.raytracer.render;

import up.raytracer.core.AABB;
import up.raytracer.core.Intersection;
import up.raytracer.core.Ray;
import up.raytracer.core.Vector3D;
import up.raytracer.scene.Object3D;

import java.util.ArrayList;
import java.util.List;

public class BVHNode {

    private static final int LEAF_SIZE = 4;
    private static final int SAH_BUCKETS = 12;
    private static final double TRAVERSAL_COST = 1.0;
    private static final double INTERSECTION_COST = 1.0;
    private static final double SHADOW_EPSILON = 1e-4;

    private final AABB bounds;
    private final BVHNode left;
    private final BVHNode right;
    private final List<Object3D> objects;

    public BVHNode(List<Object3D> sourceObjects) {
        List<Object3D> workingObjects = new ArrayList<>(sourceObjects);
        this.bounds = buildBounds(workingObjects);

        // leaves keep a few objects so the tree does not become too deep
        Split split = chooseSplit(workingObjects, bounds);
        if (split == null) {
            this.objects = workingObjects;
            this.left = null;
            this.right = null;
            return;
        }

        this.left = new BVHNode(split.leftObjects);
        this.right = new BVHNode(split.rightObjects);
        this.objects = null;
    }

    public Intersection findClosest(Ray ray, double near, double far, long[] counters) {
        if (!bounds.intersects(ray, near, far)) return null;

        if (objects != null) {
            return findClosestInLeaf(ray, near, far, counters);
        }

        // visit the closest child first so the second child can be skipped more often
        double leftDistance = left.bounds.hitDistance(ray, near, far);
        double rightDistance = right.bounds.hitDistance(ray, near, far);

        BVHNode first = left;
        BVHNode second = right;
        double firstDistance = leftDistance;
        double secondDistance = rightDistance;

        if (rightDistance < leftDistance) {
            first = right;
            second = left;
            firstDistance = rightDistance;
            secondDistance = leftDistance;
        }

        Intersection closest = null;
        if (firstDistance != Double.POSITIVE_INFINITY) {
            closest = first.findClosest(ray, near, far, counters);
            if (closest != null) far = closest.getDistance();
        }

        if (secondDistance != Double.POSITIVE_INFINITY && secondDistance <= far) {
            Intersection secondHit = second.findClosest(ray, near, far, counters);
            if (secondHit != null) closest = secondHit;
        }

        return closest;
    }

    public boolean isOccluded(Ray ray, double maxDistance, Object3D ignoredObject) {
        if (!bounds.intersects(ray, 0.0, maxDistance)) return false;

        if (objects != null) {
            return isOccludedInLeaf(ray, maxDistance, ignoredObject);
        }

        // shadow rays can stop as soon as any object blocks the light
        double leftDistance = left.bounds.hitDistance(ray, 0.0, maxDistance);
        double rightDistance = right.bounds.hitDistance(ray, 0.0, maxDistance);

        BVHNode first = left;
        BVHNode second = right;
        double firstDistance = leftDistance;
        double secondDistance = rightDistance;

        if (rightDistance < leftDistance) {
            first = right;
            second = left;
            firstDistance = rightDistance;
            secondDistance = leftDistance;
        }

        if (firstDistance != Double.POSITIVE_INFINITY && first.isOccluded(ray, maxDistance, ignoredObject)) {
            return true;
        }

        return secondDistance != Double.POSITIVE_INFINITY && second.isOccluded(ray, maxDistance, ignoredObject);
    }

    private Intersection findClosestInLeaf(Ray ray, double near, double far, long[] counters) {
        Intersection closest = null;

        for (Object3D obj : objects) {
            Intersection hit = obj.calculateIntersection(ray);
            counters[0]++;

            if (hit == null || hit.getDistance() < near || hit.getDistance() > far) continue;

            closest = hit;
            far = hit.getDistance();
        }

        return closest;
    }

    private boolean isOccludedInLeaf(Ray ray, double maxDistance, Object3D ignoredObject) {
        for (Object3D obj : objects) {
            if (obj == ignoredObject) continue;

            Intersection hit = obj.calculateIntersection(ray);
            if (hit == null) continue;
            double t = hit.getDistance();
            if (t > SHADOW_EPSILON && t < maxDistance) return true;
        }

        return false;
    }

    private static Split chooseSplit(List<Object3D> objects, AABB nodeBounds) {
        if (objects.size() <= LEAF_SIZE) return null;

        // split by centroid spread, not by object size
        AABB centroidBounds = buildCentroidBounds(objects);
        int axis = centroidBounds.getLongestAxis();
        double min = getAxisValue(centroidBounds.getMin(), axis);
        double max = getAxisValue(centroidBounds.getMax(), axis);

        if (max - min < 1e-8) return null;

        Bucket[] buckets = makeBuckets(objects, axis, min, max);
        Split bestSplit = findBestSplit(objects, buckets, nodeBounds, axis, min, max);

        // median split is a fallback for clustered objects where sah has no winner
        if (bestSplit == null) return medianSplit(objects, axis);
        return bestSplit;
    }

    private static Split findBestSplit(
            List<Object3D> objects,
            Bucket[] buckets,
            AABB nodeBounds,
            int axis,
            double min,
            double max
    ) {
        double leafCost = objects.size() * INTERSECTION_COST;
        double parentArea = Math.max(nodeBounds.getSurfaceArea(), 1e-8);
        double bestCost = leafCost;
        int bestBucket = -1;

        // try each bucket boundary and keep the cheapest surface-area split
        for (int i = 0; i < SAH_BUCKETS - 1; i++) {
            Bucket leftBucket = mergeBuckets(buckets, 0, i);
            Bucket rightBucket = mergeBuckets(buckets, i + 1, SAH_BUCKETS - 1);

            if (leftBucket.count == 0 || rightBucket.count == 0) continue;

            double leftCost = leftBucket.bounds.getSurfaceArea() * leftBucket.count;
            double rightCost = rightBucket.bounds.getSurfaceArea() * rightBucket.count;
            double cost = TRAVERSAL_COST + INTERSECTION_COST * (leftCost + rightCost) / parentArea;

            if (cost < bestCost) {
                bestCost = cost;
                bestBucket = i;
            }
        }

        if (bestBucket < 0) return null;

        List<Object3D> leftObjects = new ArrayList<>();
        List<Object3D> rightObjects = new ArrayList<>();

        for (Object3D obj : objects) {
            int bucketIndex = getBucketIndex(obj, axis, min, max);
            if (bucketIndex <= bestBucket) {
                leftObjects.add(obj);
            } else {
                rightObjects.add(obj);
            }
        }

        if (leftObjects.isEmpty() || rightObjects.isEmpty()) return null;
        return new Split(leftObjects, rightObjects);
    }

    private static Split medianSplit(List<Object3D> objects, int axis) {
        List<Object3D> sortedObjects = new ArrayList<>(objects);
        sortedObjects.sort((a, b) -> Double.compare(
                getAxisValue(a.getBounds().getCenter(), axis),
                getAxisValue(b.getBounds().getCenter(), axis)
        ));

        int middle = sortedObjects.size() / 2;
        if (middle <= 0 || middle >= sortedObjects.size()) return null;

        return new Split(
                new ArrayList<>(sortedObjects.subList(0, middle)),
                new ArrayList<>(sortedObjects.subList(middle, sortedObjects.size()))
        );
    }

    private static Bucket[] makeBuckets(List<Object3D> objects, int axis, double min, double max) {
        Bucket[] buckets = new Bucket[SAH_BUCKETS];
        for (int i = 0; i < SAH_BUCKETS; i++) {
            buckets[i] = new Bucket();
        }

        // buckets approximate many possible split positions without sorting every time
        for (Object3D obj : objects) {
            int bucketIndex = getBucketIndex(obj, axis, min, max);
            buckets[bucketIndex].add(obj.getBounds());
        }

        return buckets;
    }

    private static Bucket mergeBuckets(Bucket[] buckets, int start, int end) {
        Bucket result = new Bucket();

        for (int i = start; i <= end; i++) {
            if (buckets[i].count == 0) continue;
            result.add(buckets[i]);
        }

        return result;
    }

    private static int getBucketIndex(Object3D obj, int axis, double min, double max) {
        double center = getAxisValue(obj.getBounds().getCenter(), axis);
        double scaled = (center - min) / (max - min);
        int bucket = (int) (scaled * SAH_BUCKETS);

        if (bucket < 0) return 0;
        if (bucket >= SAH_BUCKETS) return SAH_BUCKETS - 1;
        return bucket;
    }

    private static AABB buildBounds(List<Object3D> objects) {
        AABB bounds = objects.get(0).getBounds();

        for (int i = 1; i < objects.size(); i++) {
            bounds = AABB.surrounding(bounds, objects.get(i).getBounds());
        }

        return bounds;
    }

    private static AABB buildCentroidBounds(List<Object3D> objects) {
        Vector3D center = objects.get(0).getBounds().getCenter();
        AABB bounds = new AABB(center, center);

        for (int i = 1; i < objects.size(); i++) {
            center = objects.get(i).getBounds().getCenter();
            bounds = AABB.surrounding(bounds, new AABB(center, center));
        }

        return bounds;
    }

    private static double getAxisValue(Vector3D value, int axis) {
        if (axis == 0) return value.x;
        if (axis == 1) return value.y;
        return value.z;
    }

    private static class Bucket {
        private int count;
        private AABB bounds;

        public void add(AABB newBounds) {
            count++;
            bounds = bounds == null ? newBounds : AABB.surrounding(bounds, newBounds);
        }

        public void add(Bucket bucket) {
            count += bucket.count;
            bounds = bounds == null ? bucket.bounds : AABB.surrounding(bounds, bucket.bounds);
        }
    }

    private static class Split {
        private final List<Object3D> leftObjects;
        private final List<Object3D> rightObjects;

        public Split(List<Object3D> leftObjects, List<Object3D> rightObjects) {
            this.leftObjects = leftObjects;
            this.rightObjects = rightObjects;
        }
    }
}
