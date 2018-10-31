package hu.halasz;

import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.Voronoi;
import org.kynosarges.tektosyne.geometry.VoronoiEdge;
import org.kynosarges.tektosyne.geometry.VoronoiResults;

public class VoronoiMap {
    PointD[] basePoints;

    VoronoiResults voronoiResults;
    PointD[][] voronoiRegions;
    PointD[] voronoiVertices;
    VoronoiEdge[] voronoiEdges;
    LineD[] delaunayEdges;
    PointD[] generatorSites;

    public VoronoiMap(PointD[] basePoints) {
        this.basePoints = basePoints;
    }

    private void initializeMap() {
        voronoiResults = Voronoi.findAll(basePoints);

        voronoiRegions = voronoiResults.voronoiRegions();
        voronoiVertices = voronoiResults.voronoiVertices;
        voronoiEdges = voronoiResults.voronoiEdges;
        delaunayEdges = voronoiResults.delaunayEdges();
        generatorSites = voronoiResults.generatorSites;
    }

    public void relaxPointsWithLloydRelaxation(int numberOfIterations) {
        for (int j = 0; j < numberOfIterations; j++) {
            basePoints = new PointD[basePoints.length];
            for (int i = 0; i < voronoiRegions.length; i++) {
                PointD[] voronoiRegion = voronoiRegions[i];
                PointD pointD = GeoUtils.polygonCentroid(voronoiRegion);
                basePoints[i] = pointD;
            }
            initializeMap();
        }
    }
}
