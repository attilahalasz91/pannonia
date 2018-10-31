package hu.halasz;

import lombok.Getter;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.Voronoi;
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
    Map<PointD, VoronoiCell> voronoiCellMap;
    @Getter
    List<VoronoiCell> voronoiCellList;
    @Getter
    LineD[] delaunayEdges;

    public VoronoiMapper(PointD[] sites) {
        voronoiCellMap = new HashMap<>();
        voronoiCellList = new ArrayList<>();
        siteList = Arrays.asList(sites);

        voronoiResults = Voronoi.findAll(sites);
        VoronoiMap voronoiMap = new VoronoiMap(voronoiResults);
        Subdivision source = voronoiMap.source();

        Subdivision delaunaySubdivision = voronoiResults.toDelaunaySubdivision(true);

        for (PointD site : sites) {
            //locate the face to this site on the planar subdivision graph
            SubdivisionFace siteFace = source.findFace(site);

            //one of the subdivision edge on the outer boundary of this subdivision face
            SubdivisionEdge outerEdge = siteFace.outerEdge();

            //cycle through this edge for the polygon vertices
            PointD[] polygonVertices = outerEdge.cyclePolygon();
            List<PointD> polygonVericesList = Arrays.asList(polygonVertices);

            //the center of the polygon
            PointD centroid = outerEdge.cycleCentroid();

            //all the edges of the polygon as lineD
            List<LineD> polygonEdgesList = new ArrayList<>();
            List<SubdivisionEdge> subdivisionEdges = outerEdge.cycleEdges();
            for (SubdivisionEdge subdivisionEdge : subdivisionEdges) {
                LineD polygonEdge = subdivisionEdge.toLine();
                polygonEdgesList.add(polygonEdge);
            }

            //get the neighbour sites from the delunarySubdivision graph (because it's a graph from the sites)
            List<PointD> neighborSitesList = delaunaySubdivision.getNeighbors(site);

            VoronoiCell voronoiCell = new VoronoiCell(site, centroid, polygonEdgesList, polygonVericesList, neighborSitesList, polygonVertices);
            voronoiCellMap.put(site, voronoiCell);
            voronoiCellList.add(voronoiCell);
        }

        delaunayEdges = voronoiResults.delaunayEdges();

    }

}
