package hu.halasz;

import com.badlogic.gdx.graphics.Color;
import lombok.Getter;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.PolygonLocation;
import org.kynosarges.tektosyne.geometry.RectD;
import org.kynosarges.tektosyne.geometry.Voronoi;
import org.kynosarges.tektosyne.geometry.VoronoiEdge;
import org.kynosarges.tektosyne.geometry.VoronoiResults;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static hu.halasz.PannoniaVoroi.spectralPalette;

public class VoronoiMapper {

    @Getter
    VoronoiResults voronoiResults;
    @Getter
    List<PointD> siteList;
    @Getter
    static Map<Integer, VoronoiCell> voronoiCellMap;
    @Getter
    static List<VoronoiCell> voronoiCellList;
   /* @Getter
    LineD[] delaunayEdges;*/
   private MapGenerator mapGenerator;

    public VoronoiMapper(PointD[] sites, MapGenerator mapGenerator) {
        this.mapGenerator = mapGenerator;
        voronoiCellMap = new HashMap<>();
        voronoiCellList = new ArrayList<>();

        voronoiResults = Voronoi.findAll(sites);

        //Lloyd relaxation - 1 iteration - to spread out the points more evenly
        PointD[][] pointDS = voronoiResults.voronoiRegions();
        PointD[] voronoiRegion;
        PointD pointD;
        for (int i = 0; i < pointDS.length; i++) {
            voronoiRegion = pointDS[i];
            pointD = GeoUtils.polygonCentroid(voronoiRegion);
            sites[i] = pointD;
        }
        voronoiResults = Voronoi.findAll(sites);

        siteList = Arrays.asList(sites);

        pointDS = voronoiResults.voronoiRegions();
        for (int i = 0; i < pointDS.length; i++) {
            VoronoiCell voronoiCell = new VoronoiCell(voronoiResults.generatorSites[i], pointDS[i]);//site, vertices
            voronoiCellMap.put(i, voronoiCell);
            voronoiCellList.add(voronoiCell);
        }

        for (VoronoiEdge voronoiEdge : voronoiResults.voronoiEdges) {
            int site1 = voronoiEdge.site1;
            int site2 = voronoiEdge.site2;
            VoronoiCell voronoiCell1 = getVoronoiCellMap().get(site1);
            VoronoiCell voronoiCell2 = getVoronoiCellMap().get(site2);

            PointD vertex1Point = voronoiResults.voronoiVertices[voronoiEdge.vertex1];
            PointD vertex2Point = voronoiResults.voronoiVertices[voronoiEdge.vertex2];
            //voronoiCell1.addNeighbourSite(voronoiResults.generatorSites[site2]);
            voronoiCell1.addEdge(new LineD(vertex1Point.x, vertex1Point.y, vertex2Point.x, vertex2Point.y));
            voronoiCell1.addNeighbour(voronoiCell2);
            //voronoiCell2.addNeighbourSite(voronoiResults.generatorSites[site1]);
            voronoiCell2.addEdge(new LineD(vertex1Point.x, vertex1Point.y, vertex2Point.x, vertex2Point.y));
            voronoiCell2.addNeighbour(voronoiCell1);
        }

        List<VoronoiCell> bigSites = mapGenerator.generateSites(10, new RectD(800, 100, 1500, 800));
        for (VoronoiCell bigSite : bigSites) {
            mapGenerator.erosionHeightGeneration(0.5f, 0.99f, 0.3f, 0.2f, bigSite);
        }
        List<VoronoiCell> smallSites = mapGenerator.generateSites(20, new RectD(650, 50, 1550, 900));
        for (VoronoiCell smallSite : smallSites) {
            mapGenerator.heightGeneration(0.2f, 0.99f, smallSite);
        }
        mapGenerator.setLandSites();
    }



    private void lloydRelaxation() {

    }

}
