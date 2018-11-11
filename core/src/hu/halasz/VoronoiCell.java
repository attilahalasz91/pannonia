package hu.halasz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.EarClippingTriangulator;
import lombok.Getter;
import lombok.Setter;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VoronoiCell {

    @Getter
    private PointD site;
    @Getter
    private Set<VoronoiCell> neighbours;
    @Getter
    private List<LineD> edges;
    @Getter
    private float[] verticesF;
    @Getter
    private PointD[] verticesD;
    /*@Getter
    private Set<PointD> neighborSites;*/
    @Getter
    @Setter
    private float height;
    @Getter
    @Setter
    private Color color;
    @Getter
    private short[] triangles;
    @Getter
    private static int numOfTriangles;

    private static EarClippingTriangulator triangulator;

    static {
        triangulator = new EarClippingTriangulator();
    }

    public VoronoiCell(PointD site, PointD[] verteces) {
        this.site = site;
        neighbours = new HashSet<>();
        //neighborSites = new HashSet<>();
        edges = new ArrayList<>();

        this.verticesD = verteces;
        double[] doubles = PointD.toDoubles(verticesD);
        verticesF = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            verticesF[i] = ((float) doubles[i]);
        }

        triangles = triangulator.computeTriangles(verticesF).toArray();
        numOfTriangles += triangles.length;

        this.color = new Color().set(1582277375);
        this.height = 0;
    }

    public void addNeighbour(VoronoiCell neighbour) {
        neighbours.add(neighbour);
    }

   /* public void addNeighbourSite(PointD neighbourSite) {
        neighborSites.add(neighbourSite);
    }*/

    public void addEdge(LineD edge) {
        edges.add(edge);
    }

}
