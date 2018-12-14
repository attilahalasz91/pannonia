package hu.halasz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import mikera.vectorz.Vector4;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.PolygonLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static hu.halasz.PannoniaVoroi.WORLD_WIDTH;

public class VoroiInputHandler implements InputProcessor {
    public static final float SCROLL_SPEED = 0.1f;
    protected static final int LEFT_MOUSE_CLICK = 0;
    protected static final int RIGHT_MOUSE_CLICK = 1;
    private OrthographicCamera cam;
    PointD[][] voronoiRegions;
    //PointD[] selectedRegion;
    VoronoiMapper voronoiMapper;
    List<LineD> selectedEdges;
    VoronoiCell voronoiCell;
    //static List<VoronoiCell> islandCellList;
    private Pixmap spectralPalette;
    MapGenerator mapGenerator;


    public VoroiInputHandler(OrthographicCamera cam, PointD[][] voronoiRegions, VoronoiMapper voronoiMapper, Pixmap spectralPalette, MapGenerator mapGenerator) {
        this.cam = cam;
        this.voronoiRegions = voronoiRegions;
        this.voronoiMapper = voronoiMapper;
        this.spectralPalette = spectralPalette;
        this.mapGenerator = mapGenerator;
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
        Gdx.app.log("eger: ", tp.x + " " + tp.y);

        //select a cell
        this.voronoiCell = getVoronoiCellByMouseLocation(tp);

        if (voronoiCell != null) {
            float decrement = 0.99f;
            if (button == LEFT_MOUSE_CLICK) {
                float height = 0.2f;
                mapGenerator.heightGeneration(height, decrement, voronoiCell);
            }

            if (button == RIGHT_MOUSE_CLICK) {
                float sharpness = 0.3f;
                float height = 0.5f;
                double landVsSeaBorder = 0.2;
                //float newHeight = MathUtils.clamp(voronoiCell.getHeight() + height, 0, 1);
                mapGenerator.erosionHeightGeneration(height, decrement, sharpness, landVsSeaBorder, voronoiCell);
            }

            mapGenerator.setLandSites();
            PannoniaVoroi.updateVerticiesColor();

        }

        return false;
    }


    private VoronoiCell getVoronoiCellByMouseLocation(Vector3 mouseCoordinate) {
        VoronoiCell selectedVoronoiCell = null;
        List<VoronoiCell> voronoiCellList = VoronoiMapper.getVoronoiCellList();
        for (VoronoiCell voronoiCell : voronoiCellList) {
            PointD[] verticesD = voronoiCell.getVerticesD();
            PolygonLocation polygonLocation = GeoUtils.pointInPolygon(new PointD(mouseCoordinate.x, mouseCoordinate.y), verticesD);
            if (polygonLocation.equals(PolygonLocation.INSIDE)) {
                selectedEdges = voronoiCell.getEdges();
                selectedVoronoiCell = voronoiCell;
            }
        }
        return selectedVoronoiCell;
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
        cam.zoom = MathUtils.clamp(cam.zoom, 0.2f, WORLD_WIDTH / cam.viewportWidth);
        cam.update();

        if (cam.zoom != WORLD_WIDTH / cam.viewportWidth) {
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
