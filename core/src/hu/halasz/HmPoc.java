package hu.halasz;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;

public class HmPoc implements ApplicationListener {

    private final String VERT =
            "#version 130\n" +
                    "\n" +
                    "//our attributes\n" +
                    "attribute vec2 a_position;\n" +
                    "\n" +
                    "//hightmap\n" +
                    "uniform sampler2D u_texture;\n" +
                    "//our camera matrix\n" +
                    "uniform mat4 u_projTrans;\n" +
                    "\n" +
                    "//send the color out to the fragment shader\n" +
                    "varying vec4 vColor;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vColor = texture2D(u_texture, floor( a_position / vec2(10)) /vec2(100));\n" +
                    "\n" +
                    "    gl_Position = u_projTrans * vec4(a_position.xy, 0.0, 1.0);\n" +
                    "}";
    private final String FRAG =
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
                    "\n" +
                    "void main() {\n" +
                    "    gl_FragColor = vColor;\n" +
                    "}\n";

    private Texture tex;
    private OrthographicCamera cam;
    private ShaderProgram shaderProgram;
    private SpriteBatch fontbatch;
    private BitmapFont font;
    private static Mesh mesh;

    @Override
    public void create() {

        tex = new Texture(Gdx.files.internal("hmpoc.bmp"));

        ShaderProgram.pedantic = false;
        shaderProgram = new ShaderProgram(VERT, FRAG);
        if (!shaderProgram.isCompiled()) {
            System.err.println(shaderProgram.getLog());
            System.exit(0);
        }
        if (shaderProgram.getLog().length() != 0)
            System.out.println(shaderProgram.getLog());

        cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.setToOrtho(false);

        fontbatch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        ShaderTest2InputHandler inputHandler = new ShaderTest2InputHandler(cam);
        Gdx.input.setInputProcessor(inputHandler);

        int count = 0;
        float[] vertices = new float[100 * 100 * 12];
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                vertices[count] = j * 10;
                vertices[count + 1] = i * 10;
                vertices[count + 2] = (j + 1) * 10;
                vertices[count + 3] = i * 10;
                vertices[count + 4] = j * 10;
                vertices[count + 5] = (i + 1) * 10;

                vertices[count + 6] = (j + 1) * 10;
                vertices[count + 7] = i*10;
                vertices[count + 8] = (j + 1) * 10;
                vertices[count + 9] = (i + 1) * 10;
                vertices[count + 10] = j * 10;
                vertices[count + 11] = (i + 1) * 10;
                count += 12;
            }
        }

        mesh = new Mesh(true, vertices.length, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"));

        mesh.setVertices(vertices);
    }

    @Override
    public void resize(int width, int height) {
        cam.setToOrtho(false, width, height);
        //batch.setProjectionMatrix(cam.combined);
    }

    @Override
    public void render() {
       // Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl20.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl20.glEnable(GL20.GL_TEXTURE_2D);
        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        mapScrollWatcher();
        // handleInput();
        cam.update();

        tex.bind();
        shaderProgram.begin();
        //
        shaderProgram.setUniformMatrix("u_projTrans", cam.combined);
        shaderProgram.setUniformi("u_texture", 0);
        //set our sampler2D uniforms
        shaderProgram.setUniformf("zoom", cam.zoom);

        mesh.render(shaderProgram, GL20.GL_TRIANGLES);
        shaderProgram.end();


        fontbatch.begin();
        fontbatch.setProjectionMatrix(cam.combined);
        font.draw(fontbatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 100, 300);
        font.draw(fontbatch, "CAM ZOOM: " + cam.zoom, 100, 150);
        fontbatch.end();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        //batch.dispose();
        shaderProgram.dispose();
        tex.dispose();
    }

    private void mapScrollWatcher() {
        int x = Gdx.input.getX();
        int y = Gdx.input.getY();
        int windowWidth = Gdx.graphics.getWidth();
        int windowHeight = Gdx.graphics.getHeight();

        //right rec
        if (findPoint(windowWidth - 50, 0, windowWidth, windowHeight, x, y)) {
            cam.translate(4f * cam.zoom, 0, 0);
        }

        //left rec
        if (findPoint(0, 0, 50, windowHeight, x, y)) {
            cam.translate(-4f * cam.zoom, 0, 0);
        }

        //top rec
        if (findPoint(0, 0, windowWidth, 50, x, y)) {
            cam.translate(0, 4f * cam.zoom, 0);
        }

        //down rec
        if (findPoint(0, windowHeight - 50, windowWidth, windowHeight, x, y)) {
            cam.translate(0, -4f * cam.zoom, 0);
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
        cam.position.y = MathUtils.clamp(cam.position.y, effectiveViewportHeight / 2f, Gdx.graphics.getHeight() - effectiveViewportHeight / 2f);
    }

}
