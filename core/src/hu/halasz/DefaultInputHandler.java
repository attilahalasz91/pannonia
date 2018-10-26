package hu.halasz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import hu.halasz.maploader.Province;

import java.awt.image.BufferedImage;
import java.util.Map;

import static hu.halasz.Pannonia.WORLD_WIDTH;

public class DefaultInputHandler implements InputProcessor {
    public static final float SCROLL_SPEED = 0.9f;
    private OrthographicCamera cam;
    public Province province;
    BufferedImage image;
    Map<Integer, Province> provinceMap;

    public DefaultInputHandler(OrthographicCamera cam, BufferedImage image, Map<Integer, Province> provinceMap) {
        this.cam = cam;
        this.image = image;
        this.provinceMap = provinceMap;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        Vector3 tp = getMousePosInGameWorld();
        int rgb = image.getRGB((int) (tp.x * Pannonia.MASK_TO_WORD_COORDINATE_RATE), Pannonia.MASK_BMP_HEIGHT - (int) (tp.y * Pannonia.MASK_TO_WORD_COORDINATE_RATE));
        province = provinceMap.get(rgb);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {// amount: -1 || 1
        Vector3 tp = getMousePosInGameWorld();
        float px = tp.x;
        float py = tp.y;
        cam.zoom += amount * SCROLL_SPEED;
        cam.zoom = MathUtils.clamp(cam.zoom, 0.5f, WORLD_WIDTH / cam.viewportWidth);
        cam.update();

        if (cam.zoom != WORLD_WIDTH / cam.viewportWidth){
            tp = getMousePosInGameWorld();
            cam.position.add(px - tp.x, py - tp.y, 0);
            cam.update();
        }

        return true;
    }

    private Vector3 getMousePosInGameWorld() {
        return cam.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
    }


}
