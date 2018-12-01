package hu.halasz.voronoi;

import org.kynosarges.tektosyne.geometry.RectD;

public class MyVoronoiResults {
    public final RectD clippingBounds;
    public final PointF[] generatorSites;
    public final VoronoiEdge[] voronoiEdges;
    public final PointF[] voronoiVertices;
    private final double _clipMinX;
    private final double _clipMaxX;
    private final double _clipMinY;
    private final double _clipMaxY;
    private final PointF _clipMinXMaxY;
    private final PointF _clipMaxXMinY;
    private PointF[][] _voronoiRegions;

    MyVoronoiResults(RectD clippingBounds, PointF[] generatorSites, PointF[] voronoiVertices, VoronoiEdge[] voronoiEdges) {
        if (clippingBounds == null) {
            throw new NullPointerException("clippingBounds");
        } else if (generatorSites == null) {
            throw new NullPointerException("generatorSites");
        } else if (voronoiVertices == null) {
            throw new NullPointerException("voronoiVertices");
        } else if (voronoiEdges == null) {
            throw new NullPointerException("voronoiEdges");
        } else {
            this.clippingBounds = clippingBounds;
            this.generatorSites = generatorSites;
            this.voronoiVertices = voronoiVertices;
            this.voronoiEdges = voronoiEdges;
            this._clipMinX = clippingBounds.min.x;
            this._clipMinY = clippingBounds.min.y;
            this._clipMaxX = clippingBounds.max.x;
            this._clipMaxY = clippingBounds.max.y;
            this._clipMinXMaxY = new PointF((float)_clipMinX, (float)_clipMaxY);
            this._clipMaxXMinY = new PointF((float)_clipMaxX, (float)_clipMinY);
        }
    }
}
