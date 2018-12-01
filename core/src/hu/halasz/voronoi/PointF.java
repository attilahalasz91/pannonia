package hu.halasz.voronoi;

public class PointF {
    public final float x;
    public final float y;
    public static final PointF EMPTY = new PointF();

    public PointF() {
        this.x = 0.0f;
        this.y = 0.0f;
    }

    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj != null && obj instanceof PointF) {
            PointF point = (PointF)obj;
            return this.x == point.x && this.y == point.y;
        } else {
            return false;
        }
    }

    public int hashCode() {
        long xHash = Float.floatToIntBits(this.x);
        long yHash = Float.floatToIntBits(this.y);
        return 31 * (int)(xHash ^ xHash >>> 32) + (int)(yHash ^ yHash >>> 32);
    }
}
