package hu.halasz;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import hu.halasz.maploader.Pixel;
import mikera.vectorz.Vector4;
import org.joda.time.LocalDateTime;
import org.joda.time.MutableDateTime;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.RectD;
import org.kynosarges.tektosyne.geometry.Voronoi;
import org.kynosarges.tektosyne.geometry.VoronoiResults;
import org.kynosarges.tektosyne.subdivision.Subdivision;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PannoniaVoroi extends ApplicationAdapter {
    static final int MASK_BMP_WIDTH = 1600;
    static final int MASK_BMP_HEIGHT = 1079;
    static final float MASK_TO_WORD_COORDINATE_RATE = 1f;
    static final int WORLD_WIDTH = MASK_BMP_WIDTH / (int) MASK_TO_WORD_COORDINATE_RATE;//160
    static final int WORLD_HEIGHT = Math.round(MASK_BMP_HEIGHT / MASK_TO_WORD_COORDINATE_RATE);//108
    static final int CAM_WORLD_WIDTH = 300;

    private OrthographicCamera cam;
    VoroiInputHandler voroiInputHandler;
    VoronoiResults voronoiResults;
    PointD[] pointDS;

    PointD[][] voronoiRegions;

    Set<Pixel> borderPixels;
    ShapeRenderer shapeRenderer;
    Subdivision source;

    VoronoiMapper voronoiMapper;

    //Pixmap pix;
    //EarClippingTriangulator triangulator;
    // PolygonRegion polyReg;
    PolygonSpriteBatch polygonSpriteBatch;
    PolygonSprite polygonSprite;
 /*   Texture texture;
    TextureRegion polygonTextureRegion;*/

    BitmapFont font;
    private SpriteBatch batch;
    MutableDateTime mutableDateTime;
    float timer;

    @Override
    public void create() {
        /*BufferedImage image = null;
        try {
            image = ImageIO.read(new File("sic.bmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<PointD> p = new ArrayList<>();
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int rgb = image.getRGB(i, j);
                if (rgb == -16776961) {
                    p.add(new PointD(i, j));
                }
            }
        }
        pointDS = new PointD[p.size()];
        for (int i = 0; i < p.size(); i++) {
            pointDS[i] = p.get(i);
        }*/
        Gdx.app.log("point generation start: ", LocalDateTime.now().toString());

        //random points
        pointDS = GeoUtils.randomPoints(10000, new RectD(500, 1, MASK_BMP_WIDTH, MASK_BMP_HEIGHT));

        Gdx.app.log("point generation end: ", LocalDateTime.now().toString());
        Gdx.app.log("voronoi generation start: ", LocalDateTime.now().toString());

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

        Gdx.app.log("voronoi generation end with 1 lloyd: ", LocalDateTime.now().toString());
        Gdx.app.log("mapper start: ", LocalDateTime.now().toString());
        voronoiMapper = new VoronoiMapper(pointDS);
        Gdx.app.log("mapper end: ", LocalDateTime.now().toString());

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Constructs a new OrthographicCamera, using the given viewport width and height
        // Height is multiplied by aspect ratio.
        cam = new OrthographicCamera(CAM_WORLD_WIDTH, CAM_WORLD_WIDTH * (h / w));
        cam.position.set(cam.viewportWidth / 2f, cam.viewportHeight / 2f, 0);
        cam.update();

        voroiInputHandler = new VoroiInputHandler(cam, voronoiMapper.getVoronoiResults().voronoiRegions(), voronoiMapper);
        Gdx.input.setInputProcessor(voroiInputHandler);

        shapeRenderer = new ShapeRenderer();

        font = new BitmapFont();
        font.getData().setScale(0.5f);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        batch = new SpriteBatch();

        mutableDateTime = new MutableDateTime(
                888, 12, 11, 1, 1, 1, 1);
        timer = 0;

        polygonSpriteBatch = new PolygonSpriteBatch();


    }

    @Override
    public void render() {
        timer += Gdx.graphics.getRawDeltaTime();
        if (timer > 0.5f) {
            mutableDateTime.addHours(1);
            timer = 0;
        }
        mapScrollWatcher();
        handleInput();
        cam.update();
        shapeRenderer.setProjectionMatrix(cam.combined);
        polygonSpriteBatch.setProjectionMatrix(cam.combined);
        batch.setProjectionMatrix(cam.combined);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        font.draw(batch, mutableDateTime.toString(), 100, 100);
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 100, 300);
        batch.end();

        polygonSpriteBatch.begin();
        for (VoronoiCell voronoiCell : voronoiMapper.voronoiCellList) {
            polygonSprite = voronoiCell.getPolygonSprite();
            polygonSprite.setColor(voronoiCell.getColor()); // felülírja pix colort
            polygonSprite.draw(polygonSpriteBatch);
        }
        polygonSpriteBatch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        //edgek
        shapeRenderer.setColor(Color.BLUE);
        for (VoronoiCell voronoiCell : voronoiMapper.getVoronoiCellList()) {
            List<LineD> edges = voronoiCell.getEdges();
            for (LineD lineD : edges) {
                shapeRenderer.line(((float) lineD.start.x), ((float) lineD.start.y), ((float) lineD.end.x), ((float) lineD.end.y));
            }
        }

        //selected
        if (voroiInputHandler.selectedEdges != null) {
            shapeRenderer.setColor(Color.RED);
            for (LineD lineD : voroiInputHandler.selectedEdges) {
                shapeRenderer.line(((float) lineD.start.x), ((float) lineD.start.y), ((float) lineD.end.x), ((float) lineD.end.y));
            }
        }

     /*   // draw edges of Delaunay triangulation - gráf pontok
        shapeRenderer.setColor(Color.GREEN);
        for (LineD edge: voronoiMapper.getDelaunayEdges()) {
            shapeRenderer.line((float) edge.start.x, (float)edge.start.y, (float)edge.end.x, (float)edge.end.y);;
        }*/

      /*  // draw generated points - not the exact center, az ->  Geoutils center of poly
        shapeRenderer.setColor(Color.BLACK);
        for (PointD point : voronoiMapper.getSiteList()) {
            shapeRenderer.circle(((float) point.x), ((float) point.y), 1);
        }*/

        if (voroiInputHandler.islandCellList != null) {
            shapeRenderer.setColor(Color.BLACK);
            for (VoronoiCell voronoiCell : voroiInputHandler.islandCellList) {
                for (VoronoiCell neighbour : voronoiCell.getNeighbours()) {
                    if (neighbour.getHeight() <= 0.2f) {
                        List<PointD> same = new ArrayList<>();
                        for (PointD vertex : voronoiCell.getVertices()) {
                            for (PointD neighbourVertex : neighbour.getVertices()) {
                                if (vertex.equals(neighbourVertex)) {
                                    same.add(vertex);
                                }
                            }
                        }
                        shapeRenderer.rectLine(((float) same.get(0).x), ((float) same.get(0).y),
                                ((float) same.get(1).x), ((float) same.get(1).y), 2);
                    }
                }
            }
        }

        shapeRenderer.end();
    }

    private int convertYCoordFromCoordSysYdownToYup(Pixel borderPixel) {
        return MASK_BMP_HEIGHT - borderPixel.getY();
    }

    //TODO: constansositani hogy win w/h változáshoz igazodjon a rec
    private void mapScrollWatcher() {
        int x = Gdx.input.getX();
        int y = Gdx.input.getY();
        int windowWidth = Gdx.graphics.getWidth();
        int windowHeight = Gdx.graphics.getHeight();

        //right rec
        if (findPoint(windowWidth - 50, 0, windowWidth, windowHeight, x, y)) {
            cam.translate(2f * cam.zoom, 0, 0);
        }

        //left rec
        if (findPoint(0, 0, 50, windowHeight, x, y)) {
            cam.translate(-2f * cam.zoom, 0, 0);
        }

        //top rec
        if (findPoint(0, 0, windowWidth, 50, x, y)) {
            cam.translate(0, 2f * cam.zoom, 0);
        }

        //down rec
        if (findPoint(0, windowHeight - 50, windowWidth, windowHeight, x, y)) {
            cam.translate(0, -2f * cam.zoom, 0);
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