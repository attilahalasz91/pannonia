package hu.halasz;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import hu.halasz.maploader.Pixel;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.RectD;
import org.kynosarges.tektosyne.geometry.Voronoi;
import org.kynosarges.tektosyne.geometry.VoronoiEdge;
import org.kynosarges.tektosyne.geometry.VoronoiResults;

import java.util.Arrays;
import java.util.Set;

public class PannoniaVoroi extends ApplicationAdapter {
    static final int MASK_BMP_WIDTH = 1600;
    static final int MASK_BMP_HEIGHT = 1079;
    static final float MASK_TO_WORD_COORDINATE_RATE = 1f;
    static final int WORLD_WIDTH = MASK_BMP_WIDTH / (int)MASK_TO_WORD_COORDINATE_RATE;//160
    static final int WORLD_HEIGHT = Math.round(MASK_BMP_HEIGHT / MASK_TO_WORD_COORDINATE_RATE);//108
    static final int CAM_WORLD_WIDTH = 300;

    private OrthographicCamera cam;
    VoroiInputHandler voroiInputHandler;
    VoronoiResults voronoiResults;
    PointD[] pointDS;

    PointD[][] voronoiRegions;
    PointD[] voronoiVertices;
    VoronoiEdge[] voronoiEdges;
    LineD[] delaunayEdges;
    PointD[] generatorSites;

    Set<Pixel> borderPixels;
    ShapeRenderer shapeRenderer;

    @Override
    public void create() {
        //random points
        pointDS = GeoUtils.randomPoints(10000, new RectD(1, 1, MASK_BMP_WIDTH, MASK_BMP_HEIGHT));

        voronoiResults = Voronoi.findAll(pointDS);
        voronoiRegions = voronoiResults.voronoiRegions();

        //Lloyd relaxation - 1 iteration - to spread out the points more evenly
        pointDS = new PointD[pointDS.length];
        for (int i = 0; i < voronoiRegions.length; i++) {
            PointD[] voronoiRegion = voronoiRegions[i];
            PointD pointD = GeoUtils.polygonCentroid(voronoiRegion);
            pointDS[i] = pointD;
        }
        voronoiResults = Voronoi.findAll(pointDS);

        voronoiRegions = voronoiResults.voronoiRegions();
        voronoiVertices = voronoiResults.voronoiVertices;
        voronoiEdges = voronoiResults.voronoiEdges;
        delaunayEdges = voronoiResults.delaunayEdges();
        generatorSites = voronoiResults.generatorSites;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Constructs a new OrthographicCamera, using the given viewport width and height
        // Height is multiplied by aspect ratio.
        cam = new OrthographicCamera(CAM_WORLD_WIDTH,CAM_WORLD_WIDTH * (h / w));
        cam.position.set(cam.viewportWidth / 2f, cam.viewportHeight / 2f, 0);
        cam.update();

        voroiInputHandler = new VoroiInputHandler(cam, voronoiRegions);
        Gdx.input.setInputProcessor(voroiInputHandler);

        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render() {
        mapScrollWatcher();
        handleInput();
        cam.update();
        shapeRenderer.setProjectionMatrix(cam.combined);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        ///////////
        //egész régió pontjai
        shapeRenderer.setColor(Color.BLUE);
        for (PointD[] voronoiRegion : voronoiRegions) {
            double[] doubles = PointD.toDoubles(voronoiRegion);
            float[] floats = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) {
                floats[i] = ((float) doubles[i]);
            }
            shapeRenderer.polygon(floats);
        }

        //selected
        if (voroiInputHandler.selectedRegion != null){
            shapeRenderer.setColor(Color.ORANGE);
            /*double[] doubles = PointD.toDoubles(voroiInputHandler.selectedRegion);
            float[] floats = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) {
                floats[i] = ((float) doubles[i]);
            }*/
            for (PointD point : voroiInputHandler.selectedRegion) {
                shapeRenderer.circle(((float) point.x), ((float) point.y), 1);
            }
            //shapeRenderer.polygon(floats);
        }

        //edgek
        shapeRenderer.setColor(Color.RED);
        for (VoronoiEdge voronoiEdge : voronoiEdges) {
            PointD start = voronoiResults.voronoiVertices[voronoiEdge.vertex1];
            PointD end = voronoiResults.voronoiVertices[voronoiEdge.vertex2];
            shapeRenderer.line(((float) start.x), ((float) start.y), ((float) end.x), ((float) end.y));
        }

     /*   // draw edges of Delaunay triangulation - gráf pontok
        shapeRenderer.setColor(Color.GREEN);
        for (LineD edge: voronoiResults.delaunayEdges()) {
            shapeRenderer.line((float) edge.start.x, (float)edge.start.y, (float)edge.end.x, (float)edge.end.y);;
        }*/

        // draw generated points - not the exact center, az ->  Geoutils center of poly
        shapeRenderer.setColor(Color.WHITE);
        for (PointD point: pointDS) {
            shapeRenderer.circle(((float) point.x), ((float) point.y), 1);
        }

        //////////////
        /*if (voroiInputHandler.province != null) {
            borderPixels = voroiInputHandler.province.getBorderPixels();
            for (Pixel borderPixel : borderPixels) {
                //shapeRenderer.point(borderPixel.getX(),(1079 -borderPixel.getY()), 0);
                shapeRenderer.rect(
                        borderPixel.getX() / MASK_TO_WORD_COORDINATE_RATE -0.2f,
                        convertYCoordFromCoordSysYdownToYup(borderPixel) / MASK_TO_WORD_COORDINATE_RATE - 0.2f,
                        0.5f,0.5f);
            }
        }*/
        shapeRenderer.end();
    }

    private int convertYCoordFromCoordSysYdownToYup(Pixel borderPixel) {
        return MASK_BMP_HEIGHT - borderPixel.getY();
    }

    //TODO: constansositani hogy win w/h változáshoz igazodjon a rec
    private void mapScrollWatcher(){
        int x = Gdx.input.getX();
        int y = Gdx.input.getY();
        int windowWidth = Gdx.graphics.getWidth();
        int windowHeight = Gdx.graphics.getHeight();

        //right rec
        if (findPoint(windowWidth - 50, 0, windowWidth, windowHeight, x, y)){
            cam.translate(2f*cam.zoom, 0, 0);
        }

        //left rec
        if (findPoint(0, 0, 50, windowHeight, x, y)){
            cam.translate(-2f*cam.zoom, 0, 0);
        }

        //top rec
        if (findPoint(0, 0, windowWidth, 50, x, y)){
            cam.translate(0, 2f*cam.zoom, 0);
        }

        //down rec
        if (findPoint(0, windowHeight-50, windowWidth, windowHeight, x, y)){
            cam.translate(0, -2f*cam.zoom, 0);
        }

    }

    boolean findPoint(int x1, int y1, int x2,
                      int y2, int x, int y) {
        if (x > x1 && x < x2 && y > y1 && y < y2) {
            return true;
        }
        return false;
    }

    private void handleInput() {
        float effectiveViewportWidth = cam.viewportWidth * cam.zoom;
        float effectiveViewportHeight = cam.viewportHeight * cam.zoom;

        cam.position.x = MathUtils.clamp(cam.position.x, effectiveViewportWidth / 2f, WORLD_WIDTH - effectiveViewportWidth / 2f);
        cam.position.y = MathUtils.clamp(cam.position.y, effectiveViewportHeight / 2f, WORLD_HEIGHT - effectiveViewportHeight / 2f);
    }

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = CAM_WORLD_WIDTH;
        cam.viewportHeight = (float) CAM_WORLD_WIDTH * height / width;
        cam.update();
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }

    @Override
    public void pause() {
    }
}

/* for filled polygons
PolygonSprite poly;
PolygonSpriteBatch polyBatch = new PolygonSpriteBatch(); // To assign at the beginning
Texture textureSolid;

// Creating the color filling (but textures would work the same way)
Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
pix.setColor(0xDEADBEFF); // DE is red, AD is green and BE is blue.
pix.fill();
textureSolid = new Texture(pix);
PolygonRegion polyReg = new PolygonRegion(new TextureRegion(textureSolid),
  new float[] {      // Four vertices
    0, 0,            // Vertex 0         3--2
    100, 0,          // Vertex 1         | /|
    100, 100,        // Vertex 2         |/ |
    0, 100           // Vertex 3         0--1
}, new short[] {
    0, 1, 2,         // Two triangles using vertex indices.
    0, 2, 3          // Take care of the counter-clockwise direction.
});
poly = new PolygonSprite(polyReg);
poly.setOrigin(a, b);
polyBatch = new PolygonSpriteBatch();
 */