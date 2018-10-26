package hu.halasz;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import hu.halasz.maploader.BmpMapProcessor;
import hu.halasz.maploader.Pixel;

import java.util.Set;

public class Pannonia extends ApplicationAdapter {
    static final int MASK_BMP_WIDTH = 1600;
    static final int MASK_BMP_HEIGHT = 1079;
    static final float MASK_TO_WORD_COORDINATE_RATE = 10f;
    static final int WORLD_WIDTH = MASK_BMP_WIDTH / (int)MASK_TO_WORD_COORDINATE_RATE;//160
    static final int WORLD_HEIGHT = Math.round(MASK_BMP_HEIGHT / MASK_TO_WORD_COORDINATE_RATE);//108
    static final int CAM_WORLD_WIDTH = 30;

    private OrthographicCamera cam;
    private SpriteBatch batch;

    private Sprite mapSprite;
    private float rotationSpeed;
    Pixmap pixmap;
    Texture pixmapTexture;
    DefaultInputHandler defaultInputHandler;
    Set<Pixel> borderPixels;
    ShapeRenderer shapeRenderer;

    @Override
    public void create() {
        BmpMapProcessor bmpMapProcessor = new BmpMapProcessor();
        rotationSpeed = 0.5f;

        mapSprite = new Sprite(new Texture(Gdx.files.internal("hunoriginal.bmp")));
        mapSprite.setPosition(0, 0);
        mapSprite.setSize(WORLD_WIDTH, WORLD_HEIGHT);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Constructs a new OrthographicCamera, using the given viewport width and height
        // Height is multiplied by aspect ratio.
        cam = new OrthographicCamera(CAM_WORLD_WIDTH,CAM_WORLD_WIDTH * (h / w));

        cam.position.set(cam.viewportWidth / 2f, cam.viewportHeight / 2f, 0);
        cam.update();

        batch = new SpriteBatch();
        defaultInputHandler = new DefaultInputHandler(cam, bmpMapProcessor.getImage(), bmpMapProcessor.getProvinceMap());
        Gdx.input.setInputProcessor(defaultInputHandler);

		/*pixmap = new Pixmap(1,1, Pixmap.Format.RGB888);
		pixmap.setColor(Color.RED);
		pixmapTexture = new Texture(pixmap, Pixmap.Format.RGB888, false);*/
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setColor(Color.RED);

    }

    @Override
    public void render() {
        mapScrollWatcher();
        handleInput();
        cam.update();
        batch.setProjectionMatrix(cam.combined);
        shapeRenderer.setProjectionMatrix(cam.combined);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        mapSprite.draw(batch);
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (defaultInputHandler.province != null) {
            borderPixels = defaultInputHandler.province.getBorderPixels();
            for (Pixel borderPixel : borderPixels) {
                //shapeRenderer.point(borderPixel.getX(),(1079 -borderPixel.getY()), 0);
                shapeRenderer.circle(
                        borderPixel.getX() / MASK_TO_WORD_COORDINATE_RATE,
                        convertYCoordFromCoordSysYdownToYup(borderPixel) / MASK_TO_WORD_COORDINATE_RATE,
                        0.2f);
            }
        }
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
            Gdx.app.log("s", cam.zoom+"");
            cam.translate(0.5f*cam.zoom, 0, 0);
        }

        //left rec
        if (findPoint(0, 0, 50, windowHeight, x, y)){
            cam.translate(-0.5f*cam.zoom, 0, 0);
        }

        //top rec
        if (findPoint(0, 0, windowWidth, 50, x, y)){
            cam.translate(0, 0.5f*cam.zoom, 0);
        }

        //down rec
        if (findPoint(0, windowHeight-50, windowWidth, windowHeight, x, y)){
            cam.translate(0, -0.5f*cam.zoom, 0);
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
        mapSprite.getTexture().dispose();
        batch.dispose();
    }

    @Override
    public void pause() {
    }
}
