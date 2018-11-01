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
import java.util.List;

public class VoronoiCell {

    @Getter
    private PointD site;
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
    private float height;
    @Getter
    @Setter
    Color color;
    @Getter
    PolygonRegion polygonRegion;

    private static EarClippingTriangulator triangulator;
    private static Texture texture;
    private static TextureRegion polygonTextureRegion;

    static {
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(Color.LIGHT_GRAY);
        pix.fill();
        triangulator = new EarClippingTriangulator();
        texture = new Texture(pix);
        polygonTextureRegion = new TextureRegion(texture);
    }

    public VoronoiCell(PointD site, List<LineD> edges, List<PointD> vertices, List<PointD> neighborSites, PointD[] verticesD) {
        this.verticesD = verticesD;
        this.site = site;
        this.edges = edges;
        this.vertices = vertices;
        this.neighborSites = neighborSites;
        neighbours = new ArrayList<>();

        double[] doubles = PointD.toDoubles(verticesD);
        verticesF = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            verticesF[i] = ((float) doubles[i]);
        }


        this.polygonRegion = new PolygonRegion(polygonTextureRegion, verticesF, triangulator.computeTriangles(verticesF).toArray());

        this.color = new Color(94f / 255, 79f / 255, 162f / 255, 1);
        this.height = 0;
    }

    public void addNeighbour(VoronoiCell neighbour) {
        neighbours.add(neighbour);
    }

}
