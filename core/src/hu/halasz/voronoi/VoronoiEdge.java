package hu.halasz.voronoi;

public final class VoronoiEdge {
    public final int site1;
    public final int site2;
    public final int vertex1;
    public final int vertex2;

    VoronoiEdge(int site1, int site2, int vertex1, int vertex2) {
        if (site1 < 0) {
            throw new IllegalArgumentException("site1 < 0");
        } else if (site2 < 0) {
            throw new IllegalArgumentException("site2 < 0");
        } else if (vertex1 < 0) {
            throw new IllegalArgumentException("vertex1 < 0");
        } else if (vertex2 < 0) {
            throw new IllegalArgumentException("vertex2 < 0");
        } else {
            this.site1 = site1;
            this.site2 = site2;
            this.vertex1 = vertex1;
            this.vertex2 = vertex2;
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj != null && obj instanceof VoronoiEdge) {
            VoronoiEdge edge = (VoronoiEdge)obj;
            return this.site1 == edge.site1 && this.site2 == edge.site2 && this.vertex1 == edge.vertex1 && this.vertex2 == edge.vertex2;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return 31 * (31 * (31 * this.site1 + this.site2) + this.vertex1) + this.vertex2;
    }

    public String toString() {
        return String.format("VoronoiEdge[site1=%d, site2=%d, vertex1=%d, vertex2=%d]", this.site1, this.site2, this.vertex1, this.vertex2);
    }
}
