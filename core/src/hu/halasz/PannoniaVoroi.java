package hu.halasz;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.GdxRuntimeException;
import hu.halasz.maploader.Pixel;
import mikera.vectorz.Vector4;
import org.apache.commons.lang.ArrayUtils;
import org.joda.time.LocalDateTime;
import org.joda.time.MutableDateTime;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.RectD;
import org.kynosarges.tektosyne.geometry.RegularPolygon;
import org.kynosarges.tektosyne.geometry.Voronoi;
import org.kynosarges.tektosyne.geometry.VoronoiEdge;
import org.kynosarges.tektosyne.geometry.VoronoiResults;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PannoniaVoroi extends ApplicationAdapter {
    static final int MASK_BMP_WIDTH = 1600;
    static final int MASK_BMP_HEIGHT = 1079;
    static final float MASK_TO_WORD_COORDINATE_RATE = 1f;
    static final int WORLD_WIDTH = MASK_BMP_WIDTH / (int) MASK_TO_WORD_COORDINATE_RATE;//160
    static final int WORLD_HEIGHT = Math.round(MASK_BMP_HEIGHT / MASK_TO_WORD_COORDINATE_RATE);//108
    static final int CAM_WORLD_WIDTH = 300;

    private OrthographicCamera cam;
    VoroiInputHandler voroiInputHandler;
    PointD[] pointDS;
    ShapeRenderer shapeRenderer;
    VoronoiMapper voronoiMapper;

    private BitmapFont font;
    private Pixmap spectralPalette;
    private SpriteBatch batch;
    private MutableDateTime mutableDateTime;
    float timer;

    private ShaderProgram shaderProgram;
    private static Mesh mesh;
    private static float[] vertices;
    private static Mesh mesh2;
    private static float[] voronoiEdges;

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
        //random points
        pointDS = GeoUtils.randomPoints(10000, new RectD(500, 1, MASK_BMP_WIDTH, MASK_BMP_HEIGHT));

        Gdx.app.log("mapper start: ", LocalDateTime.now().toString());
        voronoiMapper = new VoronoiMapper(pointDS);
        Gdx.app.log("mapper end: ", LocalDateTime.now().toString());

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        // Constructs a new OrthographicCamera, using the given viewport width and height
        // Height is multiplied by aspect ratio.
        cam = new OrthographicCamera(CAM_WORLD_WIDTH, CAM_WORLD_WIDTH * (h / w));
        cam.position.set(cam.viewportWidth / 2f, cam.viewportHeight / 2f, 0);
        cam.setToOrtho(false);
        cam.update();

        spectralPalette = new Pixmap(Gdx.files.internal("Spectral.png"));
        voroiInputHandler = new VoroiInputHandler(cam, voronoiMapper.getVoronoiResults().voronoiRegions(), voronoiMapper, spectralPalette);
        Gdx.input.setInputProcessor(voroiInputHandler);

        shapeRenderer = new ShapeRenderer();

        font = new BitmapFont();
        font.getData().setScale(0.5f);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        batch = new SpriteBatch();

        mutableDateTime = new MutableDateTime(
                888, 12, 11, 1, 1, 1, 1);
        timer = 0;

        vertices = new float[VoronoiCell.getNumOfTriangles() * 3];
        int i = 0;
        for (VoronoiCell voronoiCell : VoronoiMapper.getVoronoiCellList()) {
            short[] triangles = voronoiCell.getTriangles();
            for (int j = 0; j < triangles.length; j++) {
                vertices[i] = (float) voronoiCell.getVerticesD()[triangles[j]].x;
                vertices[i + 1] = (float) voronoiCell.getVerticesD()[triangles[j]].y;
                vertices[i + 2] = Color.argb8888(voronoiCell.getColor());
                i += 3;
            }

        }

        mesh = new Mesh(true, vertices.length, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 1, "a_color"));

        mesh.setVertices(vertices);

        /*String vertexShader = "//our attributes\n" +
                "attribute vec2 a_position;\n" +
                "attribute vec4 a_color;\n" +
                "\n" +
                "//our camera matrix\n" +
                "uniform mat4 u_projTrans;\n" +
                "\n" +
                "//send the color out to the fragment shader\n" +
                "varying vec4 vColor;\n" +
                "\n" +
                "void main() {\n" +
                "\tvColor = a_color;\n" +
                "\tgl_Position = u_projTrans * vec4(a_position.xy, 0.0, 1.0);\n" +
                "}";
        String fragmentShader = "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "\n" +
                "//input from vertex shader\n" +
                "varying vec4 vColor;\n" +
                "\n" +
                "void main() {\n" +
                "\tgl_FragColor = vColor;\n" +
                "}";*/
        String vertexShader = "//our attributes\n" +
                "attribute vec2 a_position;\n" +
                "attribute float a_color;\n" +
                "\n" +
                "//our camera matrix\n" +
                "uniform mat4 u_projTrans;\n" +
                "\n" +
                "//send the color out to the fragment shader\n" +
                "varying float vColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vColor = a_color;\n" +
                "    gl_Position = u_projTrans * vec4(a_position.xy, 0.0, 1.0);\n" +
                "}";
        String fragmentShader = " #version 130\n" +
                "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "\n" +
                "//input from vertex shader\n" +
                "varying float vColor;\n" +
                "\n" +
                "void main() {\n" +
                "int value = int(vColor);\n" +
                "float rValue = ((value & 0x00ff0000) >> 16) / 255.0;\n" +
                "float gValue = ((value & 0x0000ff00) >> 8) / 255.0;\n" +
                "float bValue = ((value & 0x000000ff)) / 255.0;\n" +
                "gl_FragColor = vec4(rValue, gValue, bValue, 1.0);" +
                "}";


        ShaderProgram.pedantic = false;
        shaderProgram = new ShaderProgram(vertexShader, fragmentShader);
        String log = shaderProgram.getLog();
        if (!shaderProgram.isCompiled())
            throw new GdxRuntimeException(log);
        if (log != null && log.length() != 0)
            System.out.println("Shader Log: " + log);

        voronoiEdges = new float[voronoiMapper.getVoronoiResults().voronoiEdges.length * 6];
        i = 0;
        for (VoronoiEdge voronoiEdge : voronoiMapper.getVoronoiResults().voronoiEdges) {
            float x = (float) voronoiMapper.getVoronoiResults().voronoiVertices[voronoiEdge.vertex1].x;
            float y = (float) voronoiMapper.getVoronoiResults().voronoiVertices[voronoiEdge.vertex1].y;
            float x2 = (float) voronoiMapper.getVoronoiResults().voronoiVertices[voronoiEdge.vertex2].x;
            float y2 = (float) voronoiMapper.getVoronoiResults().voronoiVertices[voronoiEdge.vertex2].y;
            Color blue = Color.BLUE;

            voronoiEdges[i] = x;
            voronoiEdges[i + 1] = y;
            voronoiEdges[i + 2] = Color.argb8888(blue);

            voronoiEdges[i + 3] = x2;
            voronoiEdges[i + 4] = y2;
            voronoiEdges[i + 5] = Color.argb8888(blue);

            i += 6;
        }

        mesh2 = new Mesh(true, voronoiEdges.length, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 1, "a_color"));

        mesh2.setVertices(voronoiEdges);

    }

    public static void updateVerticiesColor() {
        //TODO: csak változott cellákon végigmenni
        List<Float> verticesWithColorList = new ArrayList<>();
        for (VoronoiCell voronoiCell : VoronoiMapper.getVoronoiCellList()) {
            short[] triangles = voronoiCell.getTriangles();
            for (int j = 0; j < triangles.length; j++) {
                verticesWithColorList.add((float) voronoiCell.getVerticesD()[triangles[j]].x);
                verticesWithColorList.add((float) voronoiCell.getVerticesD()[triangles[j]].y);
                verticesWithColorList.add((float) Color.argb8888(voronoiCell.getColor()));
            }

        }
        vertices = ArrayUtils.toPrimitive(verticesWithColorList.toArray(new Float[verticesWithColorList.size()]));
        mesh.setVertices(vertices);
    }

    @Override
    public void render() {
        long start = System.currentTimeMillis();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        //no need for depth...
        Gdx.gl.glDepthMask(false);
        //enable blending, for alpha
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        mapScrollWatcher();
        handleInput();
        cam.update();

        timer += Gdx.graphics.getRawDeltaTime();
        if (timer > 0.5f) {
            mutableDateTime.addHours(1);
            timer = 0;
        }

        batch.begin();
        batch.setProjectionMatrix(cam.combined);
        font.draw(batch, mutableDateTime.toString(), 100, 100);
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 100, 300);
        font.draw(batch, "CAM ZOOM: " + cam.zoom, 100, 150);
        batch.end();

        shaderProgram.begin();
        //update the projection matrix so our triangles are rendered in 2D
        shaderProgram.setUniformMatrix("u_projTrans", cam.combined);
        //polygons
        mesh.render(shaderProgram, GL20.GL_TRIANGLES);
        //edges
        mesh2.render(shaderProgram, GL20.GL_LINES);
        shaderProgram.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setProjectionMatrix(cam.combined);
        // if (cam.zoom < 2.5f){


        // }

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

        System.out.println("render() took: " + (System.currentTimeMillis() - start) + "ms");
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
        batch.dispose();
        mesh.dispose();
        spectralPalette.dispose();
    }

    @Override
    public void pause() {
    }
}