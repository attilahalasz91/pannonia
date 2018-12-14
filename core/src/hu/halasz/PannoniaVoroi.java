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
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;
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
    static final int MASK_BMP_WIDTH = 1600;//1600
    static final int MASK_BMP_HEIGHT = 1079;//1079
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
    public static Pixmap spectralPalette;
    private SpriteBatch batch;
    private MutableDateTime mutableDateTime;
    float timer;

    private ShaderProgram shaderProgram;
    private static Mesh mesh;
    private static float[] vertices;
    private static Mesh mesh2;
    private static float[] voronoiEdges;
    private MapGenerator mapGenerator;

    static float iTime = 0;

    @Override
    public void create() {
        System.out.println(new Color(94f / 255, 79f / 255, 162f / 255, 1).toIntBits());
        System.out.println(Color.argb8888(new Color(94f / 255, 79f / 255, 162f / 255, 1)));
        System.out.println(Color.rgba8888(new Color(94f / 255, 79f / 255, 162f / 255, 1)));

        spectralPalette = new Pixmap(Gdx.files.internal("Spectral.png"));
        mapGenerator = new MapGenerator(WORLD_WIDTH, WORLD_HEIGHT);
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
        pointDS = GeoUtils.randomPoints(100000, new RectD(500, 1, MASK_BMP_WIDTH, MASK_BMP_HEIGHT));

        Gdx.app.log("mapper start: ", LocalDateTime.now().toString());
        voronoiMapper = new VoronoiMapper(pointDS, mapGenerator);
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
        voroiInputHandler = new VoroiInputHandler(cam, voronoiMapper.getVoronoiResults().voronoiRegions(), voronoiMapper, spectralPalette, mapGenerator);
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

        /*String vertexShader = "#version 130\n//our attributes\n" +
                "attribute vec2 a_position;\n" +
                "attribute float a_color;\n" +
                "\n" +
                "//our camera matrix\n" +
                "uniform mat4 u_projTrans;\n" +
                "\n" +
                "//send the color out to the fragment shader\n" +
                "varying vec4 vColor;\n" +
                "\n" +
                "void main() {\n" +
                "int value = int(a_color);\n" +
                "float rValue = ((value & 0x00ff0000) >> 16) / 255.0;\n" +
                "float gValue = ((value & 0x0000ff00) >> 8) / 255.0;\n" +
                "float bValue = ((value & 0x000000ff)) / 255.0;\n" +
                "    vColor = vec4(rValue, gValue, bValue, 1.0);\n" +
                "    gl_Position = u_projTrans * vec4(a_position.xy, 0.0, 1.0);\n" +
                "}";
        String fragmentShader = " #version 130\n" +
                "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "\n" +
                "//input from vertex shader\n" +
                "varying vec4 vColor;\n" +
                "\n" +
                "void main() {\n" +
                "gl_FragColor = vColor;" +
                "}";*/
        String vertexShader = "#version 130\n" +
                "\n" +
                "//our attributes\n" +
                "attribute vec2 a_position;\n" +
                "attribute float a_color;\n" +
                "\n" +
                "//our camera matrix\n" +
                "uniform mat4 u_projTrans;\n" +
                "\n" +
                "//send the color out to the fragment shader\n" +
                "varying vec4 vColor;\n" +
                "//send the position out to the fragment shader\n" +
                "varying vec2 a_position2;\n" +
                "\n" +
                "void main() {\n" +
                "    int value = int(a_color);\n" +
                "    float rValue = ((value & 0x00ff0000) >> 16) / 255.0;\n" +
                "    float gValue = ((value & 0x0000ff00) >> 8) / 255.0;\n" +
                "    float bValue = ((value & 0x000000ff)) / 255.0;\n" +
                "    vColor = vec4(rValue, gValue, bValue, 1.0);\n" +
                "    a_position2 = a_position;\n" +
                "\n" +
                "    gl_Position = u_projTrans * vec4(a_position.xy, 0.0, 1.0);\n" +
                "}";
        String fragmentShader = "// Simple Water shader. (c) Victor Korsun, bitekas@gmail.com; 2012.\n" +
                "//\n" +
                "// Attribution-ShareAlike CC License.\n" +
                "#version 130\n" +
                "\n" +
                "#ifdef GL_ES\n" +
                "precision highp float;\n" +
                "#endif\n" +
                "\n" +
                "uniform float iTime;\n" +
                "uniform vec3 iResolution;\n" +
                "//uniform sampler2D iChannel0;\n" +
                "varying vec4 vColor;\n" +
                "//our camera matrix\n" +
                "uniform mat4 u_projTrans;\n" +
                "//a_position from vertex shader - varying to interpolate between vertex coords\n" +
                "varying vec2 a_position2;\n" +
                "\n" +
                "const float PI = 3.1415926535897932;\n" +
                "\n" +
                "// play with these parameters to custimize the effect\n" +
                "// ===================================================\n" +
                "\n" +
                "//speed\n" +
                "const float speed = 0.05;//0.1\n" +
                "const float speed_x = 0.1;//0.2\n" +
                "const float speed_y = 0.1;//0.2\n" +
                "\n" +
                "// refraction\n" +
                "const float emboss = 0.50;\n" +
                "const float intensity = 2.4;\n" +
                "const int steps = 8;\n" +
                "const float frequency = 6.0;\n" +
                "const int angle = 7; // better when a prime\n" +
                "\n" +
                "// reflection\n" +
                "const float delta = 60.;\n" +
                "const float intence = 700.;\n" +
                "\n" +
                "const float reflectionCutOff = 0.012;\n" +
                "const float reflectionIntence = 200000.;\n" +
                "\n" +
                "// ===================================================\n" +
                "\n" +
                "float col(vec2 coord,float time) {\n" +
                "    float delta_theta = 2.0 * PI / float(angle);\n" +
                "    float col = 0.0;\n" +
                "    float theta = 0.0;\n" +
                "    for (int i = 0; i < steps; i++) {\n" +
                "        vec2 adjc = coord;\n" +
                "        theta = delta_theta*float(i);\n" +
                "        adjc.x += cos(theta)*time*speed + time * speed_x;\n" +
                "        adjc.y -= sin(theta)*time*speed - time * speed_y;\n" +
                "        col = col + cos( (adjc.x*cos(theta) - adjc.y*sin(theta))*frequency)*intensity;\n" +
                "    }\n" +
                "\n" +
                "    return cos(col);\n" +
                "}\n" +
                "\n" +
                "//---------- main\n" +
                "\n" +
                "void main() {\n" +
                "//94f / 255, 79f / 255, 162f / 255\n" +
                "    if (length(vColor - vec4(0.0,0.0,76.5/255.0, 1.0)) <= 0.01){\n" +
                "        float time = iTime*1.3;\n" +
                "\n" +
                "            //vec2 p = (gl_FragCoord.xy) / iResolution.xy, c1 = p, c2 = p;\n" +
                "            vec2 p = (a_position2.xy) / iResolution.xy*400.0, c1 = p, c2 = p;\n" +
                "            float cc1 = col(c1,time);\n" +
                "\n" +
                "            c2.x += iResolution.x/delta;\n" +
                "            float dx = emboss*(cc1-col(c2,time))/delta;\n" +
                "\n" +
                "            c2.x = p.x;\n" +
                "            c2.y += iResolution.y/delta;\n" +
                "            float dy = emboss*(cc1-col(c2,time))/delta;\n" +
                "\n" +
                "            c1.x += dx*2.;\n" +
                "            c1.y = -(c1.y+dy*2.);\n" +
                "\n" +
                "            float alpha = 1.+dot(dx,dy)*intence;\n" +
                "\n" +
                "            float ddx = dx - reflectionCutOff;\n" +
                "            float ddy = dy - reflectionCutOff;\n" +
                "            if (ddx > 0. && ddy > 0.)\n" +
                "                alpha = pow(alpha, ddx*ddy*reflectionIntence);\n" +
                "\n" +
                "            //vec4 col = texture(iChannel0,c1)*(alpha);\n" +
                "            vec4 col = vColor*(alpha);\n" +
                "            gl_FragColor = col;\n" +
                "    } else {\n" +
                "        gl_FragColor = vColor;\n" +
                "    }\n" +
                "\n" +
                "}\n";

        ShaderProgram.pedantic = false;
        shaderProgram = new ShaderProgram(vertexShader, fragmentShader);
        String log = shaderProgram.getLog();
        if (!shaderProgram.isCompiled())
            throw new GdxRuntimeException(log);
        if (log != null && log.length() != 0)
            System.out.println("Shader Log: " + log);

        voronoiEdges = new float[voronoiMapper.getVoronoiResults().voronoiEdges.length * 6];
        i = 0;
        Color blue = Color.BLUE;
        for (VoronoiEdge voronoiEdge : voronoiMapper.getVoronoiResults().voronoiEdges) {
            float x = (float) voronoiMapper.getVoronoiResults().voronoiVertices[voronoiEdge.vertex1].x;
            float y = (float) voronoiMapper.getVoronoiResults().voronoiVertices[voronoiEdge.vertex1].y;
            float x2 = (float) voronoiMapper.getVoronoiResults().voronoiVertices[voronoiEdge.vertex2].x;
            float y2 = (float) voronoiMapper.getVoronoiResults().voronoiVertices[voronoiEdge.vertex2].y;

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
        //long start = System.currentTimeMillis();
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
        shaderProgram.setUniformf("iResolution", new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 1.0f));
        iTime+= 0.01;
        shaderProgram.setUniformf("iTime", iTime);
        //update the projection matrix so our triangles are rendered in 2D
        shaderProgram.setUniformMatrix("u_projTrans", cam.combined);
        //polygons
        mesh.render(shaderProgram, GL20.GL_TRIANGLES);
        //edges
        //mesh2.render(shaderProgram, GL20.GL_LINES);
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
        }

        // draw generated points - not the exact center, az ->  Geoutils center of poly
        shapeRenderer.setColor(Color.BLACK);
        for (PointD point : voronoiMapper.getSiteList()) {
            shapeRenderer.circle(((float) point.x), ((float) point.y), 1);
        }
*/
        if (mapGenerator.islandCellList != null) {
            shapeRenderer.setColor(Color.BLACK);
            for (VoronoiCell voronoiCell : mapGenerator.islandCellList) {
                for (VoronoiCell neighbour : voronoiCell.getNeighbours()) {
                    if (neighbour.getHeight() <= 0.2f) {
                        List<PointD> same = new ArrayList<>();
                        PointD[] voronoiCellVerticesD = voronoiCell.getVerticesD();
                        PointD[] neighbourVerticesD = neighbour.getVerticesD();
                        for (PointD aVoronoiCellVerticesD : voronoiCellVerticesD) {
                            for (PointD aNeighbourVerticesD : neighbourVerticesD) {
                                if (aVoronoiCellVerticesD.equals(aNeighbourVerticesD)) {
                                    same.add(aVoronoiCellVerticesD);
                                }
                            }
                        }
                        shapeRenderer.line(((float) same.get(0).x), ((float) same.get(0).y),
                                ((float) same.get(1).x), ((float) same.get(1).y));
                    }
                }
            }
        }

        shapeRenderer.end();

        //System.out.println("render() took: " + (System.currentTimeMillis() - start) + "ms");
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


//        cam.position.x = MathUtils.clamp(cam.position.x, effectiveViewportWidth / 2f, WORLD_WIDTH - effectiveViewportWidth / 2f);
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