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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static hu.halasz.PannoniaVoroi.WORLD_WIDTH;

public class VoroiInputHandler implements InputProcessor {
    public static final float SCROLL_SPEED = 0.5f;
    private OrthographicCamera cam;
    PointD[][] voronoiRegions;
    //PointD[] selectedRegion;
    VoronoiMapper voronoiMapper;
    List<LineD> selectedEdges;
    VoronoiCell voronoiCell;
    List<VoronoiCell> islandCellList;

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

        List<VoronoiCell> voronoiCellList = voronoiMapper.getVoronoiCellList();
        for (VoronoiCell voronoiCell : voronoiCellList) {
            PointD[] verticesD = voronoiCell.getVerticesD();
            PolygonLocation polygonLocation = GeoUtils.pointInPolygon(new PointD(tp.x, tp.y), verticesD);
            if (polygonLocation.equals(PolygonLocation.INSIDE)) {
                selectedEdges = voronoiCell.getEdges();
                this.voronoiCell = voronoiCell;
            }
        }

        if (voronoiCell != null) {
            float height = 0.2f;
            float decrement = 0.99f;
            Color color1 = new Color(94f / 255, 79f / 255, 162f / 255, 1);
            Color color2 = Color.RED;
            Set<VoronoiCell> usedSet = new HashSet<>(); //clear és kiemel majd másik classban

            if (button == 0) {
                float newHeight = MathUtils.clamp(voronoiCell.getHeight() + height, 0, 1);
                voronoiCell.setHeight(newHeight);
                voronoiCell.setColor(interpolateHSV(color1, color2, newHeight));
                usedSet.add(voronoiCell);

                Queue<VoronoiCell> queue = new ArrayDeque<>();
                queue.offer(voronoiCell);
                do {
                    height *= decrement;
                    VoronoiCell poll = queue.poll();
                    List<VoronoiCell> neighbours = poll.getNeighbours();
                    for (VoronoiCell neighbour : neighbours) {
                        if (!usedSet.contains(neighbour)) {
                            newHeight = MathUtils.clamp(neighbour.getHeight() + height, 0, 1);
                            neighbour.setHeight(newHeight);
                            neighbour.setColor(interpolateHSV(color1, color2, newHeight));
                            usedSet.add(neighbour);
                            queue.offer(neighbour);
                        }
                    }
                } while (height > 0.01 && queue.size() > 0);
            }

            if (button == 1) {
                float sharpness = 0.3f;
                height = 0.5f;
                //float newHeight = MathUtils.clamp(voronoiCell.getHeight() + height, 0, 1);
                voronoiCell.setHeight(height);
                voronoiCell.setColor(interpolateHSV(color1, color2, height));
                usedSet.add(voronoiCell);

                Queue<VoronoiCell> queue = new ArrayDeque<>();
                queue.offer(voronoiCell);
                do {
                    VoronoiCell poll = queue.poll();
                    height = poll.getHeight() * decrement;
                    List<VoronoiCell> neighbours = poll.getNeighbours();
                    for (VoronoiCell neighbour : neighbours) {
                        if (!usedSet.contains(neighbour)) {
                            double mod = Math.random() * sharpness + 1.1f - sharpness;
                            // if sharpness is 0 modifier should be ignored (=1)
                            //newHeight = MathUtils.clamp(neighbour.getHeight() + height, 0, 1);
                            float newHeight = neighbour.getHeight() + height * (float) mod;
                            neighbour.setHeight(newHeight);
                            neighbour.setColor(interpolateHSV(color1, color2, newHeight));
                            usedSet.add(neighbour);
                            queue.offer(neighbour);
                        }
                    }
                } while (height > 0.01 && queue.size() > 0);
            }

            islandCellList = new ArrayList<>();
            for (VoronoiCell cell : voronoiCellList) {
                if (cell.getHeight() > 0.2f){
                    islandCellList.add(cell);
                }
            }
        }

        return false;
    }

    private Color interpolateHSV(Color color1, Color color2, float fraction) {
        float[] color1HsvValues = new float[3];
        float[] color2HsvValues = new float[3];
        color1.toHsv(color1HsvValues);
        color2.toHsv(color2HsvValues);
        Vector4 vector4 = Vector4.of(color1HsvValues[0], color1HsvValues[1], color1HsvValues[2], color1.a);
        vector4.interpolate(new Vector4(color2HsvValues[0], color2HsvValues[1], color2HsvValues[2], color2.a), fraction);

        return new Color(1, 1, 1, (float) vector4.t).fromHsv(
                (float) vector4.x, (float) vector4.y, (float) vector4.z);
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
