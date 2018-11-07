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
    public static final float SCROLL_SPEED = 0.5f;
    protected static final int LEFT_MOUSE_CLICK = 0;
    protected static final int RIGHT_MOUSE_CLICK = 1;
    private OrthographicCamera cam;
    PointD[][] voronoiRegions;
    //PointD[] selectedRegion;
    VoronoiMapper voronoiMapper;
    List<LineD> selectedEdges;
    VoronoiCell voronoiCell;
    static List<VoronoiCell> islandCellList;
    private Pixmap spectralPalette;


    public VoroiInputHandler(OrthographicCamera cam, PointD[][] voronoiRegions, VoronoiMapper voronoiMapper, Pixmap spectralPalette) {
        this.cam = cam;
        this.voronoiRegions = voronoiRegions;
        this.voronoiMapper = voronoiMapper;
        this.spectralPalette = spectralPalette;
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
            Set<VoronoiCell> usedSet = new HashSet<>(); //clear és kiemel majd másik classban

            if (button == LEFT_MOUSE_CLICK) {
                float newHeight = MathUtils.clamp(voronoiCell.getHeight() + height, 0, 1);
                voronoiCell.setHeight(newHeight);
                voronoiCell.setColor(interpolate(1 - newHeight));
                usedSet.add(voronoiCell);

                Queue<VoronoiCell> queue = new ArrayDeque<>();
                queue.offer(voronoiCell);
                do {
                    height *= decrement;
                    VoronoiCell poll = queue.poll();
                    Set<VoronoiCell> neighbours = poll.getNeighbours();
                    for (VoronoiCell neighbour : neighbours) {
                        if (!usedSet.contains(neighbour)) {
                            newHeight = MathUtils.clamp(neighbour.getHeight() + height, 0, 1);
                            neighbour.setHeight(newHeight);
                            neighbour.setColor(interpolate(1 - newHeight));
                            usedSet.add(neighbour);
                            queue.offer(neighbour);
                        }
                    }
                } while (height > 0.01 && queue.size() > 0);
            }

            if (button == RIGHT_MOUSE_CLICK) {
                float sharpness = 0.3f;
                height = 0.5f;
                //float newHeight = MathUtils.clamp(voronoiCell.getHeight() + height, 0, 1);
                voronoiCell.setHeight(height);
                voronoiCell.setColor(interpolate(1 - height));
                usedSet.add(voronoiCell);

                Queue<VoronoiCell> queue = new ArrayDeque<>();
                queue.offer(voronoiCell);
                do {
                    VoronoiCell poll = queue.poll();
                    height = poll.getHeight() * decrement;
                    Set<VoronoiCell> neighbours = poll.getNeighbours();
                    for (VoronoiCell neighbour : neighbours) {
                        if (!usedSet.contains(neighbour)) {
                            double mod = Math.random() * sharpness + 1.1f - sharpness;
                            // if sharpness is 0 modifier should be ignored (=1)
                            //newHeight = MathUtils.clamp(neighbour.getHeight() + height, 0, 1);
                            float newHeight = neighbour.getHeight() + height * (float) mod;
                            neighbour.setHeight(newHeight);
                            neighbour.setColor(interpolate(1 - newHeight));
                            usedSet.add(neighbour);
                            queue.offer(neighbour);
                        }
                    }
                } while (height > 0.01 && queue.size() > 0);
            }

            islandCellList = new ArrayList<>();
            for (VoronoiCell cell : voronoiCellList) {
                if (cell.getHeight() > 0.2f) {
                    islandCellList.add(cell);
                }
            }
            PannoniaVoroi.updateVerticiesColor();
        }

        return false;
    }

    private Color interpolate(float xn) {
        int y1 = 0;
        int y2 = 888;
        int x1 = 0;
        int x2 = 1;
        int yn = (int) (((xn - x1) / (x2 - x1)) * (y2 - y1) + y1);

        return new Color().set(spectralPalette.getPixel(yn, 0));
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
