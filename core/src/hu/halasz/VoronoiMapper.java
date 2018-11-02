package hu.halasz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.EarClippingTriangulator;
import lombok.Getter;
import org.joda.time.LocalDateTime;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.Voronoi;
import org.kynosarges.tektosyne.geometry.VoronoiEdge;
import org.kynosarges.tektosyne.geometry.VoronoiResults;
import org.kynosarges.tektosyne.subdivision.Subdivision;
import org.kynosarges.tektosyne.subdivision.SubdivisionEdge;
import org.kynosarges.tektosyne.subdivision.SubdivisionFace;
import org.kynosarges.tektosyne.subdivision.VoronoiMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoronoiMapper {

    @Getter
    VoronoiResults voronoiResults;
    @Getter
    List<PointD> siteList;
    @Getter
    Map<Integer, VoronoiCell> voronoiCellMap;
    @Getter
    List<VoronoiCell> voronoiCellList;
   /* @Getter
    LineD[] delaunayEdges;*/

    public VoronoiMapper(PointD[] sites) {
        voronoiCellMap = new HashMap<>();
        voronoiCellList = new ArrayList<>();
        siteList = Arrays.asList(sites);

        voronoiResults = Voronoi.findAll(sites);

        PointD[][] pointDS = voronoiResults.voronoiRegions();
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
            voronoiCell1.addNeighbourSite(voronoiResults.generatorSites[site2]);
            voronoiCell1.addEdge(new LineD(vertex1Point.x, vertex1Point.y, vertex2Point.x, vertex2Point.y));
            voronoiCell1.addNeighbour(voronoiCell2);
            voronoiCell2.addNeighbourSite(voronoiResults.generatorSites[site1]);
            voronoiCell2.addEdge(new LineD(vertex1Point.x, vertex1Point.y, vertex2Point.x, vertex2Point.y));
            voronoiCell2.addNeighbour(voronoiCell1);
        }
    }

}
