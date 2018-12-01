package hu.halasz.voronoi;

import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.PointI;
import org.kynosarges.tektosyne.geometry.RectD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class MyVoronoi {

    private SiteVertex[] _sites;
    private double _minX;
    private double _maxX;
    private double _minY;
    private double _maxY;
    private double _minClipX;
    private double _maxClipX;
    private double _minClipY;
    private double _maxClipY;

    private List<PointI> _delaunayEdges;
    private List<VoronoiEdge> _voronoiEdges;
    private List<PointF> _voronoiVertices;
    private int[] _vertexIndices;

    private HalfEdge[] _edgeList;
    private HalfEdge _edgeListLeft;
    private HalfEdge _edgeListRight;
    private HalfEdge[] _priQueue;
    private int _priQueueCount;
    private int _priQueueMin;
    private int _vertexCount;

    private static final FullEdge DELETED_EDGE = new FullEdge();

    private MyVoronoi(PointF[] points, RectD[] clip, boolean findDelaunay) {
        if (points == null) {
            throw new NullPointerException("points");
        } else if (points.length < 2) {
            throw new IllegalArgumentException("points.length < 2");
        } else if (clip == null) {
            throw new NullPointerException("clip");
        } else if (clip.length != 1) {
            throw new IllegalArgumentException("clip.length != 1");
        } else {
            _sites = new SiteVertex[points.length];

            for (int i = 0; i < _sites.length; ++i) {
                _sites[i] = new SiteVertex(points[i], i);
            }

            Comparator<SiteVertex> comparator = (s, t) -> {
                if (s == t) {
                    return 0;
                } else if (s.y < t.y) {
                    return -1;
                } else if (s.y > t.y) {
                    return 1;
                } else if (s.x < t.x) {
                    return -1;
                } else {
                    return s.x > t.x ? 1 : 0;
                }
            };
            Arrays.sort(_sites, comparator);
            _minX = _maxX = _sites[0].x;

            int maxVertexCount;
            for (maxVertexCount = 1; maxVertexCount < _sites.length; ++maxVertexCount) {
                double x = _sites[maxVertexCount].x;
                if (x < _minX) {
                    _minX = x;
                }

                if (x > _maxX) {
                    _maxX = x;
                }
            }

            _minY = _sites[0].y;
            _maxY = _sites[_sites.length - 1].y;
            maxVertexCount = Math.max(0, 2 * _sites.length - 5);
            int maxEdgeCount = Math.max(1, 3 * _sites.length - 6);
            if (findDelaunay) {
                _delaunayEdges = new ArrayList(maxEdgeCount);
            } else {
                double dx = _maxX - _minX;
                double dy = _maxY - _minY;
                double d = Math.max(dx, dy) * 1.1D;
                _minClipX = _minX - (d - dx) / 2.0D;
                _maxClipX = _maxX + (d - dx) / 2.0D;
                _minClipY = _minY - (d - dy) / 2.0D;
                _maxClipY = _maxY + (d - dy) / 2.0D;
                if (clip[0].width() > 0.0D && clip[0].height() > 0.0D) {
                    _minClipX = Math.min(_minClipX, clip[0].min.x);
                    _maxClipX = Math.max(_maxClipX, clip[0].max.x);
                    _minClipY = Math.min(_minClipY, clip[0].min.y);
                    _maxClipY = Math.max(_maxClipY, clip[0].max.y);
                }

                clip[0] = new RectD(new PointD(_minClipX, _minClipY), new PointD(_maxClipX, _maxClipY));
                _voronoiVertices = new ArrayList(maxVertexCount + 5);
                _voronoiEdges = new ArrayList(maxEdgeCount);
                _vertexIndices = new int[maxVertexCount];
            }
        }
    }

    public static MyVoronoiResults findAll(PointF[] points, RectD clip) {
        RectD[] clipRef = new RectD[]{clip};
        MyVoronoi voronoi = new MyVoronoi(points, clipRef, false);
        voronoi.sweepLine();
        List<PointF> vertices = voronoi._voronoiVertices;
        List<VoronoiEdge> edges = voronoi._voronoiEdges;
        return new MyVoronoiResults(
                clipRef[0],
                points,
                vertices.toArray(new PointF[vertices.size()]),
                edges.toArray(new VoronoiEdge[edges.size()]));
    }

    private void priQueueInit() {
        _priQueueCount = _priQueueMin = 0;
        int n = (int) (4.0D * Math.sqrt((double) (_sites.length + 4)));
        _priQueue = new HalfEdge[n];

        for (int i = 0; i < _priQueue.length; ++i) {
            _priQueue[i] = new HalfEdge();
        }
    }

    private PointF priQueuePeek() {
        while (_priQueue[_priQueueMin].next == null) {
            ++_priQueueMin;
        }

        return new PointF(_priQueue[_priQueueMin].next.vertex.x, _priQueue[_priQueueMin].next.yStar);
    }

    private void edgeListInit() {
        int n = (int) (2.0D * Math.sqrt((double) (_sites.length + 4)));
        _edgeList = new HalfEdge[n];
        _edgeListLeft = new HalfEdge();
        _edgeListRight = new HalfEdge();
        _edgeListLeft.right = _edgeListRight;
        _edgeListRight.left = _edgeListLeft;
        _edgeList[0] = _edgeListLeft;
        _edgeList[n - 1] = _edgeListRight;
    }

    private void sweepLine() {
        PointF minSite = PointF.EMPTY;
        priQueueInit();
        edgeListInit();
        int newSiteIndex = 1;
        SiteVertex newSite = _sites[newSiteIndex];

        while (true) {
            while (true) {
                if (_priQueueCount != 0) {
                    minSite = priQueuePeek();
                }

                SiteVertex lowSite;
                SiteVertex p;
                HalfEdge leftHE;
                HalfEdge rightHE;
                HalfEdge bisectHE;
                FullEdge bisector;
                if (newSite != null && (_priQueueCount == 0 || newSite.y < minSite.y || newSite.y == minSite.y && newSite.x < minSite.x)) {
                    leftHE = edgeListLeftBound(newSite);
                    rightHE = leftHE.right;
                    lowSite = getRightSite(leftHE);
                    bisector = bisectSites(lowSite, newSite);
                    bisectHE = new HalfEdge(bisector, false);
                    edgeListInsert(leftHE, bisectHE);
                    p = intersect(leftHE, bisectHE);
                    if (p != null) {
                        priQueueDelete(leftHE);
                        priQueueInsert(leftHE, p, getDistance(p, newSite));
                    }

                    leftHE = bisectHE;
                    bisectHE = new HalfEdge(bisector, true);
                    edgeListInsert(leftHE, bisectHE);
                    p = intersect(bisectHE, rightHE);
                    if (p != null) {
                        priQueueInsert(bisectHE, p, getDistance(p, newSite));
                    }

                    newSite = null;
                    ++newSiteIndex;
                    if (newSiteIndex < _sites.length) {
                        newSite = _sites[newSiteIndex];
                    }
                } else {
                    if (_priQueueCount == 0) {
                        if (_voronoiEdges != null) {
                            HashSet<FullEdge> fullEdges = new HashSet();

                            for (HalfEdge he = _edgeListLeft.right; he != _edgeListRight; he = he.right) {
                                if (!fullEdges.contains(he.edge)) {
                                    storeVoronoiEdge(he.edge);
                                    fullEdges.add(he.edge);
                                }
                            }
                        }

                        return;
                    }

                    leftHE = priQueuePop();
                    rightHE = leftHE.right;
                    HalfEdge prevHE = leftHE.left;
                    HalfEdge nextHE = rightHE.right;
                    lowSite = getLeftSite(leftHE);
                    SiteVertex highSite = getRightSite(rightHE);
                    SiteVertex v = leftHE.vertex;
                    v.index = _vertexCount++;
                    if (_voronoiEdges != null && v.x >= _minClipX && v.x <= _maxClipX && v.y >= _minClipY && v.y <= _maxClipY) {
                        _vertexIndices[v.index] = _voronoiVertices.size();
                        _voronoiVertices.add(new PointF(v.x, v.y));
                    }

                    addVertex(leftHE.edge, leftHE.isRight, v);
                    addVertex(rightHE.edge, rightHE.isRight, v);
                    edgeListDelete(leftHE);
                    priQueueDelete(rightHE);
                    edgeListDelete(rightHE);
                    boolean isRight = false;
                    if (lowSite.y > highSite.y) {
                        SiteVertex tmpSite = lowSite;
                        lowSite = highSite;
                        highSite = tmpSite;
                        isRight = true;
                    }

                    bisector = bisectSites(lowSite, highSite);
                    bisectHE = new HalfEdge(bisector, isRight);
                    edgeListInsert(prevHE, bisectHE);
                    addVertex(bisector, !isRight, v);
                    p = intersect(prevHE, bisectHE);
                    if (p != null) {
                        priQueueDelete(prevHE);
                        priQueueInsert(prevHE, p, getDistance(p, lowSite));
                    }

                    p = intersect(bisectHE, nextHE);
                    if (p != null) {
                        priQueueInsert(bisectHE, p, getDistance(p, lowSite));
                    }
                }
            }
        }
    }

    private static float getDistance(SiteVertex s, SiteVertex t) {
        float fx = s.x - t.x;
        float fy = s.y - t.y;
        return (float)Math.sqrt(fx * fx + fy * fy);
    }

    private void priQueueInsert(HalfEdge he, SiteVertex v, float offset) {
        he.vertex = v;
        he.yStar = v.y + offset;
        HalfEdge hash = this._priQueue[this.priQueueBucket(he)];

        for (HalfEdge next = hash.next; next != null && (he.yStar > next.yStar || he.yStar == next.yStar && v.x > next.vertex.x); next = next.next) {
            hash = next;
        }

        he.next = hash.next;
        hash.next = he;
        ++this._priQueueCount;
    }

    private void priQueueDelete(HalfEdge he) {
        if (he.vertex != null) {
            HalfEdge hash;
            for (hash = this._priQueue[this.priQueueBucket(he)]; hash.next != he; hash = hash.next) {
                ;
            }

            hash.next = he.next;
            --this._priQueueCount;
            he.vertex = null;
        }
    }

    private int priQueueBucket(HalfEdge he) {
        int n = this._priQueue.length;
        int bucket = (int) ((he.yStar - this._minY) / (this._maxY - this._minY) * (double) n);
        if (bucket < 0) {
            bucket = 0;
        }

        if (bucket >= n) {
            bucket = n - 1;
        }

        if (bucket < this._priQueueMin) {
            this._priQueueMin = bucket;
        }

        return bucket;
    }

    private static SiteVertex intersect(HalfEdge he1, HalfEdge he2) {
        FullEdge e1 = he1.edge;
        FullEdge e2 = he2.edge;
        if (e1 != null && e2 != null) {
            if (e1.rightSite == e2.rightSite) {
                return null;
            } else {
                float d = e1.a * e2.b - e1.b * e2.a;
                if (Math.abs(d) < 1.0E-10D) {
                    return null;
                } else {
                    float xint = (e1.c * e2.b - e2.c * e1.b) / d;
                    float yint = (e2.c * e1.a - e1.c * e2.a) / d;
                    HalfEdge el;
                    FullEdge e;
                    if (e1.rightSite.y >= e2.rightSite.y && (e1.rightSite.y != e2.rightSite.y || e1.rightSite.x >= e2.rightSite.x)) {
                        el = he2;
                        e = e2;
                    } else {
                        el = he1;
                        e = e1;
                    }

                    boolean isRightOfSite = xint >= e.rightSite.x;
                    return (!isRightOfSite || el.isRight) && (isRightOfSite || !el.isRight) ? new SiteVertex(xint, yint) : null;
                }
            }
        } else {
            return null;
        }
    }

    private static void edgeListInsert(HalfEdge hePos, HalfEdge heNew) {
        heNew.left = hePos;
        heNew.right = hePos.right;
        hePos.right.left = heNew;
        hePos.right = heNew;
    }

    private static void edgeListDelete(HalfEdge he) {
        he.left.right = he.right;
        he.right.left = he.left;
        he.edge = DELETED_EDGE;
    }

    private FullEdge bisectSites(SiteVertex s, SiteVertex t) {
        FullEdge e = new FullEdge();
        e.leftSite = s;
        e.rightSite = t;
        float dx = t.x - s.x;
        float dy = t.y - s.y;
        float adx = dx > 0.0f ? dx : -dx;
        float ady = dy > 0.0f ? dy : -dy;
        e.c = s.x * dx + s.y * dy + (dx * dx + dy * dy) / 2.0f;
        if (adx > ady) {
            e.a = 1.0f;
            e.b = dy / dx;
            e.c /= dx;
        } else {
            e.a = dx / dy;
            e.b = 1.0f;
            e.c /= dy;
        }

        if (this._delaunayEdges != null) {
            this._delaunayEdges.add(new PointI(s.index, t.index));
        }

        return e;
    }

    private void addVertex(FullEdge e, boolean isRight, SiteVertex s) {
        e.setVertex(isRight, s);
        if (_voronoiEdges != null && e.getVertex(!isRight) != null) {
            storeVoronoiEdge(e);
        }

    }

    private HalfEdge edgeListLeftBound(SiteVertex s) {
        int n = this._edgeList.length;
        int bucket = (int) ((s.x - this._minX) / (this._maxX - this._minX) * (double) n);
        if (bucket < 0) {
            bucket = 0;
        }

        if (bucket >= n) {
            bucket = n - 1;
        }

        HalfEdge he = this.edgeListHash(bucket);
        if (he == null) {
            int i = 1;

            while (true) {
                he = this.edgeListHash(bucket - i);
                if (he != null) {
                    break;
                }

                he = this.edgeListHash(bucket + i);
                if (he != null) {
                    break;
                }

                ++i;
            }
        }

        assert he != null;

        if (he == this._edgeListLeft || he != this._edgeListRight && isRightOf(he, s)) {
            do {
                he = he.right;
            } while (he != this._edgeListRight && isRightOf(he, s));

            he = he.left;
        } else {
            do {
                he = he.left;
            } while (he != this._edgeListLeft && !isRightOf(he, s));
        }

        if (bucket > 0 && bucket < n - 1) {
            this._edgeList[bucket] = he;
        }

        return he;
    }

    private SiteVertex getLeftSite(HalfEdge he) {
        if (he.edge == null) {
            return this._sites[0];
        } else {
            return he.isRight ? he.edge.rightSite : he.edge.leftSite;
        }
    }

    private SiteVertex getRightSite(HalfEdge he) {
        if (he.edge == null) {
            return this._sites[0];
        } else {
            return he.isRight ? he.edge.leftSite : he.edge.rightSite;
        }
    }

    private HalfEdge priQueuePop() {
        HalfEdge he = this._priQueue[this._priQueueMin].next;
        this._priQueue[this._priQueueMin].next = he.next;
        --this._priQueueCount;
        return he;
    }

    private static boolean isRightOf(HalfEdge he, SiteVertex p) {
        FullEdge e = he.edge;
        boolean isRightOfSite = p.x > e.rightSite.x;
        if (isRightOfSite && !he.isRight) {
            return true;
        } else if (!isRightOfSite && he.isRight) {
            return false;
        } else {
            boolean isAbove;
            double dyp;
            double dxp;
            if (e.a == 1.0D) {
                dyp = p.y - e.rightSite.y;
                dxp = p.x - e.rightSite.x;
                boolean isFast = false;
                if (!isRightOfSite && e.b < 0.0D || isRightOfSite && e.b >= 0.0D) {
                    isAbove = dyp >= e.b * dxp;
                    isFast = isAbove;
                } else {
                    isAbove = p.x + p.y * e.b > e.c;
                    if (e.b < 0.0D) {
                        isAbove = !isAbove;
                    }

                    if (!isAbove) {
                        isFast = true;
                    }
                }

                if (!isFast) {
                    double dxs = e.rightSite.x - e.leftSite.x;
                    isAbove = e.b * (dxp * dxp - dyp * dyp) < dxs * dyp * (1.0D + 2.0D * dxp / dxs + e.b * e.b);
                    if (e.b < 0.0D) {
                        isAbove = !isAbove;
                    }
                }
            } else {
                dyp = e.c - e.a * p.x;
                dxp = p.y - dyp;
                double t2 = p.x - e.rightSite.x;
                double t3 = dyp - e.rightSite.y;
                isAbove = dxp * dxp > t2 * t2 + t3 * t3;
            }

            return he.isRight != isAbove;
        }
    }

    private HalfEdge edgeListHash(int bucket) {
        if (bucket >= 0 && bucket < this._edgeList.length) {
            HalfEdge he = this._edgeList[bucket];
            if (he != null && he.edge == DELETED_EDGE) {
                this._edgeList[bucket] = null;
                return null;
            } else {
                return he;
            }
        } else {
            return null;
        }
    }

    private void storeVoronoiEdge(FullEdge e) {
        assert this._voronoiEdges != null;

        SiteVertex s1;
        SiteVertex s2;
        if (e.a == 1.0D && e.b >= 0.0D) {
            s1 = e.rightVertex;
            s2 = e.leftVertex;
        } else {
            s1 = e.leftVertex;
            s2 = e.rightVertex;
        }

        assert e.a == 1.0D || e.b == 1.0D;

        double x1;
        double x2;
        double y1;
        double y2;
        if (e.a == 1.0D) {
            if (s1 != null && s1.y > this._minClipY) {
                y1 = s1.y;
                if (y1 > this._maxClipY) {
                    return;
                }
            } else {
                y1 = this._minClipY;
            }

            if (s2 != null && s2.y < this._maxClipY) {
                y2 = s2.y;
                if (y2 < this._minClipY) {
                    return;
                }
            } else {
                y2 = this._maxClipY;
            }

            x1 = e.c - e.b * y1;
            x2 = e.c - e.b * y2;
            if (x1 > this._maxClipX) {
                if (x2 > this._maxClipX) {
                    return;
                }

                x1 = this._maxClipX;
                y1 = (e.c - x1) / e.b;
            } else if (x1 < this._minClipX) {
                if (x2 < this._minClipX) {
                    return;
                }

                x1 = this._minClipX;
                y1 = (e.c - x1) / e.b;
            }

            if (x2 > this._maxClipX) {
                x2 = this._maxClipX;
                y2 = (e.c - x2) / e.b;
            } else if (x2 < this._minClipX) {
                x2 = this._minClipX;
                y2 = (e.c - x2) / e.b;
            }
        } else {
            if (s1 != null && s1.x > this._minClipX) {
                x1 = s1.x;
                if (x1 > this._maxClipX) {
                    return;
                }
            } else {
                x1 = this._minClipX;
            }

            if (s2 != null && s2.x < this._maxClipX) {
                x2 = s2.x;
                if (x2 < this._minClipX) {
                    return;
                }
            } else {
                x2 = this._maxClipX;
            }

            y1 = e.c - e.a * x1;
            y2 = e.c - e.a * x2;
            if (y1 > this._maxClipY) {
                if (y2 > this._maxClipY) {
                    return;
                }

                y1 = this._maxClipY;
                x1 = (e.c - y1) / e.a;
            } else if (y1 < this._minClipY) {
                if (y2 < this._minClipY) {
                    return;
                }

                y1 = this._minClipY;
                x1 = (e.c - y1) / e.a;
            }

            if (y2 > this._maxClipY) {
                y2 = this._maxClipY;
                x2 = (e.c - y2) / e.a;
            } else if (y2 < this._minClipY) {
                y2 = this._minClipY;
                x2 = (e.c - y2) / e.a;
            }
        }

        int vertex1;
        if (s1 != null && s1.x >= this._minClipX && s1.x <= this._maxClipX && s1.y >= this._minClipY && s1.y <= this._maxClipY) {
            vertex1 = this._vertexIndices[s1.index];
        } else {
            vertex1 = this._voronoiVertices.size();
            this._voronoiVertices.add(new PointF((float)x1, (float)y1));
        }

        int vertex2;
        if (s2 != null && s2.x >= this._minClipX && s2.x <= this._maxClipX && s2.y >= this._minClipY && s2.y <= this._maxClipY) {
            vertex2 = this._vertexIndices[s2.index];
        } else {
            vertex2 = this._voronoiVertices.size();
            this._voronoiVertices.add(new PointF((float)x2, (float)y2));
        }

        VoronoiEdge ve = new VoronoiEdge(e.leftSite.index, e.rightSite.index, vertex1, vertex2);
        this._voronoiEdges.add(ve);
    }
}
