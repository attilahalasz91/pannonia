package hu.halasz.voronoi;

public class HalfEdge {
    FullEdge edge;
    boolean isRight;
    HalfEdge left;
    HalfEdge next;
    HalfEdge right;
    SiteVertex vertex;
    float yStar;

    HalfEdge() {
    }

    HalfEdge(FullEdge edge, boolean isRight) {
        this.edge = edge;
        this.isRight = isRight;
    }
}
