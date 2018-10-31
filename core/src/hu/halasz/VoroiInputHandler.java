package hu.halasz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import mikera.vectorz.Vector4;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.PolygonLocation;

import java.util.ArrayDeque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static hu.halasz.PannoniaVoroi.WORLD_WIDTH;

public class VoroiInputHandler implements InputProcessor {
    public static final float SCROLL_SPEED = 0.5f;
    private OrthographicCamera cam;
    PointD[][] voronoiRegions;
    PointD[] selectedRegion;
    VoronoiMapper voronoiMapper;
    List<LineD> selectedEdges;
    VoronoiCell voronoiCell;

    public VoroiInputHandler(OrthographicCamera cam, PointD[][] voronoiRegions, VoronoiMapper voronoiMapper) {
        this.cam = cam;
        this.voronoiRegions = voronoiRegions;
        this.voronoiMapper = voronoiMapper;
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
        float height = 1;
        float decrement = 0.98f;
        List<VoronoiCell> voronoiCellList = voronoiMapper.getVoronoiCellList();
        for (VoronoiCell voronoiCell : voronoiCellList) {
            PointD[] verticesD = voronoiCell.getVerticesD();
            PolygonLocation polygonLocation = GeoUtils.pointInPolygon(new PointD(tp.x, tp.y), verticesD);
            if (polygonLocation.equals(PolygonLocation.INSIDE)) {
                selectedEdges = voronoiCell.getEdges();
                selectedRegion = verticesD;
                voronoiCell.setHeight(height);
                voronoiCell.setColor(interpolate(voronoiCell.getColor(), Color.RED, height));
                voronoiCell.setUsed(true);
                this.voronoiCell = voronoiCell;
            }
        }

        /*for (PointD[] voronoiRegion : voronoiRegions) {
            PolygonLocation polygonLocation = GeoUtils.pointInPolygon(new PointD(tp.x, tp.y), voronoiRegion);
            if (polygonLocation.equals(PolygonLocation.INSIDE)){
                selectedRegion = voronoiRegion;
            }
        }*/

        if (voronoiCell != null) {
            Queue<VoronoiCell> queue = new ArrayDeque<>();
            queue.offer(voronoiCell);
            do {
                height *= decrement;
                VoronoiCell poll = queue.poll();
                List<VoronoiCell> neighbours = poll.getNeighbours();
                for (VoronoiCell neighbour : neighbours) {
                    if (!neighbour.isUsed()) {
                        neighbour.setHeight(height);
                        neighbour.setColor(interpolate(Color.BLUE, Color.RED, height));
                        neighbour.setUsed(true);
                        queue.offer(neighbour);
                    }
                }
            } while (height > 0.01 && queue.size() > 0);


        }

        return false;
    }

    private Color interpolate(Color color1, Color color2, float fraction) {
        Vector4 vector4 = Vector4.of(color1.r, color1.g, color1.b, color1.a);
        vector4.interpolate(new Vector4(color2.r, color2.g, color2.b, color2.a), fraction);
        return new Color((float) vector4.x, (float) vector4.y, (float) vector4.z, (float) vector4.t);
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
