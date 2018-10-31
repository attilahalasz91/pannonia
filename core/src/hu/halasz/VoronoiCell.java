package hu.halasz;

import lombok.Getter;
import lombok.Setter;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;

import java.util.List;

public class VoronoiCell {

    @Getter
    private PointD site;
    @Getter
    private PointD centroid;
    @Getter
    private List<VoronoiCell> neighbours;
    @Getter
    private List<LineD> edges;
    @Getter
    private List<PointD> vertices;
    @Getter
    private float[] verticesF;
    @Getter
    private PointD[] verticesD;
    @Getter
    List<PointD> neighborSites;
    @Getter
    @Setter
    float height;

    public VoronoiCell(PointD site, PointD centroid, List<LineD> edges, List<PointD> vertices, List<PointD> neighborSites, PointD[] verticesD) {
        this.verticesD = verticesD;
        this.site = site;
        this.centroid = centroid;
        this.edges = edges;
        this.vertices = vertices;
        this.neighborSites = neighborSites;

        double[] doubles = PointD.toDoubles(verticesD);
        verticesF = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            verticesF[i] = ((float) doubles[i]);
        }
    }

}
