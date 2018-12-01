package hu.halasz.voronoi;

import org.kynosarges.tektosyne.geometry.Voronoi;

public class FullEdge {
    float a;
    float b;
    float c;
    SiteVertex leftSite;
    SiteVertex leftVertex;
    SiteVertex rightSite;
    SiteVertex rightVertex;

    public FullEdge() {
    }

    SiteVertex getVertex(boolean isRight) {
        return isRight ? this.rightVertex : this.leftVertex;
    }

    void setVertex(boolean isRight, SiteVertex vertex) {
        if (isRight) {
            this.rightVertex = vertex;
        } else {
            this.leftVertex = vertex;
        }

    }
}
