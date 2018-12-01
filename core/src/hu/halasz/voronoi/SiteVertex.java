package hu.halasz.voronoi;

public class SiteVertex {
    int index;
    final float x;
    final float y;

    SiteVertex(float x, float y) {
        this.x = x;
        this.y = y;
    }

    SiteVertex(PointF p, int index) {
        this.x = p.x;
        this.y = p.y;
        this.index = index;
    }
}
