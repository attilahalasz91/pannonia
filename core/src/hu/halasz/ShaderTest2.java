package hu.halasz;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;

public class ShaderTest2 implements ApplicationListener {

    private final String VERT =
            "attribute vec4 a_position;\n" +
                    "attribute vec2 a_texCoord0;\n" +
                    "uniform mat4 u_projTrans;\n" +
                    "\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    vTexCoord = a_texCoord0;\n" +
                    "    gl_Position =  u_projTrans * a_position;\n" +
                    "}";
    private final String FRAG =
            "uniform sampler2D u_texture; //default GL_TEXTURE0, expected by SpriteBatch\\n\" +\n" +
                    "varying vec2 vTexCoord;\n" +
                    "\n" +
                    "uniform sampler2D spectralColorPalette;\n" +
                    "\n" +
                    "vec4 interpolate(vec4 terrainColor) {\n" +
                    "    float xn = terrainColor.r + terrainColor.g + terrainColor.b;\n" +
                    "\n" +
                    "    float y1 = 0.0;\n" +
                    "    float y2 = 888.0;\n" +
                    "    float x1 = 0.0;\n" +
                    "    float x2 = 3.0;\n" +
                    "    int yn = int( (((xn - x1) / (x2 - x1)) * (y2 - y1) + y1) ) ;\n" +
                    "\n" +
                    "    return texture(spectralColorPalette, vec2(1.0-yn/y2, 0.0));\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec4 terrainColor = texture(u_texture, vTexCoord);\n" +
                    "\n" +
                    "    gl_FragColor = interpolate(terrainColor);\n" +
                    "}\n" +
                    "\n";

    private Texture tex;
    private Texture spectral;
    //private Texture tex;
    private OrthographicCamera cam;
    private ShaderProgram shaderProgram;
    private SpriteBatch batch;
    private SpriteBatch fontbatch;
    private BitmapFont font;

    @Override
    public void create() {

        tex = new Texture(Gdx.files.internal("ireland.png"));
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);//makes it not pixely when zoomed
        spectral = new Texture(Gdx.files.internal("Spectral.png"));

        //important since we aren't using some uniforms and attributes that SpriteBatch expects
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

        spectral.bind(1);
        //now we need to reset glActiveTexture to zero!!!! since sprite batch does not do this for us
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);


        batch = new SpriteBatch(1000, shaderProgram);
        batch.setShader(shaderProgram);

        shaderProgram.begin();
        //set our sampler2D uniforms
        shaderProgram.setUniformi("spectralColorPalette", 1);
        shaderProgram.end();

        fontbatch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        ShaderTest2InputHandler inputHandler = new ShaderTest2InputHandler(cam);
        Gdx.input.setInputProcessor(inputHandler);

    }

    @Override
    public void resize(int width, int height) {
        cam.setToOrtho(false, width, height);
        //batch.setProjectionMatrix(cam.combined);
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mapScrollWatcher();
       // handleInput();
        cam.update();

        batch.begin();
        batch.setProjectionMatrix(cam.combined);
        batch.draw(tex, 0, 0);
        batch.end();

        fontbatch.begin();
        fontbatch.setProjectionMatrix(cam.combined);
        font.draw(fontbatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 100, 300);
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
