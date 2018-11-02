package hu.halasz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.EarClippingTriangulator;
import lombok.Getter;
import lombok.Setter;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import sun.plugin2.util.ColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class VoronoiCell {

    @Getter
    private PointD site;
    @Getter
    private Set<VoronoiCell> neighbours;
    @Getter
    private List<LineD> edges;
    @Getter
    private List<PointD> vertices;
    @Getter
    private float[] verticesF;
    @Getter
    private PointD[] verticesD;
    @Getter
    Set<PointD> neighborSites;
    @Getter
    @Setter
    private float height;
    @Getter
    @Setter
    Color color;
    @Getter
    PolygonRegion polygonRegion;
    @Getter
    PolygonSprite polygonSprite;
    @Getter
    short[] triangles;

    private static EarClippingTriangulator triangulator;
    private static Texture texture;
    private static TextureRegion polygonTextureRegion;

    static {
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(Color.WHITE);
        pix.fill();
        triangulator = new EarClippingTriangulator();
        texture = new Texture(pix);
        polygonTextureRegion = new TextureRegion(texture);
        pix.dispose();
    }

    public VoronoiCell(PointD site, PointD[] verteces) {
        this.site = site;
        vertices = new ArrayList<>();
        neighbours = new HashSet<>();
        neighborSites = new HashSet<>();
        edges = new ArrayList<>();

        this.verticesD = verteces;
        this.vertices = Arrays.asList(verteces);
        double[] doubles = PointD.toDoubles(verticesD);
        verticesF = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            verticesF[i] = ((float) doubles[i]);
        }

        triangles = triangulator.computeTriangles(verticesF).toArray();
        this.polygonRegion = new PolygonRegion(polygonTextureRegion, verticesF, triangles);

        this.color = new Color(94f / 255, 79f / 255, 162f / 255, 1);
        this.height = 0;

        polygonSprite = new PolygonSprite(polygonRegion);
    }

    public void addNeighbour(VoronoiCell neighbour) {
        neighbours.add(neighbour);
    }

    public void addNeighbourSite(PointD neighbourSite) {
        neighborSites.add(neighbourSite);
    }

    public void addEdge(LineD edge) {
        edges.add(edge);
    }

}
