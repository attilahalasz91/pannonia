package hu.halasz.voronoi;

import hu.halasz.voronoi.PointF;
import org.kynosarges.tektosyne.geometry.RectD;

import java.util.concurrent.ThreadLocalRandom;

public class MyGeoUtils {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    public static PointF[] randomPoints(int count, RectD bounds) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        } else {
            double width = bounds.width();
            double height = bounds.height();
            if (width == 0.0D) {
                throw new IllegalArgumentException("bounds.width == 0");
            } else if (height == 0.0D) {
                throw new IllegalArgumentException("bounds.height == 0");
            } else {
                PointF[] points = new PointF[count];

                for(int i = 0; i < points.length; ++i) {
                    points[i] = new PointF((float)(bounds.min.x + RANDOM.nextDouble(width)),(float)(bounds.min.y + RANDOM.nextDouble(height)));
                }

                return points;
            }
        }
    }
}
