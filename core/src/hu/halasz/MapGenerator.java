package hu.halasz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.PolygonLocation;
import org.kynosarges.tektosyne.geometry.RectD;
import org.kynosarges.tektosyne.geometry.Voronoi;
import org.kynosarges.tektosyne.geometry.VoronoiResults;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static hu.halasz.PannoniaVoroi.spectralPalette;

public class MapGenerator {

    List<VoronoiCell> islandCellList;

    //new RectD(800, 100, 1500, 800)
    public List<VoronoiCell> generateSites(int numberOfSites, RectD bounds){
        List<VoronoiCell> sites = new ArrayList<>();
        PointD[] pointDS = GeoUtils.randomPoints(numberOfSites, bounds);
        for (int i = 0; i < pointDS.length; i++) {
            List<VoronoiCell> voronoiCellList = VoronoiMapper.getVoronoiCellList();
            for (VoronoiCell voronoiCell : voronoiCellList) {
                PointD[] verticesD = voronoiCell.getVerticesD();
                PolygonLocation polygonLocation = GeoUtils.pointInPolygon(pointDS[i], verticesD);
                if (polygonLocation.equals(PolygonLocation.INSIDE)) {
                    sites.add(voronoiCell);
                }
            }
        }
        return sites;
    }

    private VoronoiResults generateVoronoiDiagram(PointD[] sites){
        return Voronoi.findAll(sites);
    }

    /*private PointD[] generateErosionSites(int numberOfSites, RectD bounds){
        return GeoUtils.randomPoints(numberOfSites, bounds);
    }*/

    public MapGenerator(int mapWidth, int mapHeight) {


    }

    public void setLandSites() {
        islandCellList = new ArrayList<>();
        for (VoronoiCell cell : VoronoiMapper.voronoiCellList) {
            if (cell.getHeight() > 0.2f) {
                islandCellList.add(cell);
            }
        }
    }

    //0.5f, 0.99f, 0.3f, 0.2f, bigSite
    public void heightGeneration(float height, float decrement, VoronoiCell startingCell) {
        Set<VoronoiCell> usedSet = new HashSet<>();
        float newHeight = MathUtils.clamp(startingCell.getHeight() + height, 0, 1);
        startingCell.setHeight(newHeight);
        startingCell.setColor(interpolate(1 - newHeight));
        usedSet.add(startingCell);

        Queue<VoronoiCell> queue = new ArrayDeque<>();
        queue.offer(startingCell);
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

    public void erosionHeightGeneration(float height, float decrement, float sharpness, double landVsSeaBorder, VoronoiCell startingCell) {
        Set<VoronoiCell> usedSet = new HashSet<>();
        startingCell.setHeight(height);
        startingCell.setColor(interpolate(1 - height));
        usedSet.add(startingCell);

        Queue<VoronoiCell> queue = new ArrayDeque<>();
        queue.offer(startingCell);
        do {
            VoronoiCell poll = queue.poll();
            height = poll.getHeight() * decrement;
            Set<VoronoiCell> neighbours = poll.getNeighbours();
            for (VoronoiCell neighbour : neighbours) {
                if (!usedSet.contains(neighbour)) {
                    float newHeight;
                    if (neighbour.getHeight() < landVsSeaBorder){
                        double mod = Math.random() * sharpness + 1.1f - sharpness;
                        // if sharpness is 0 modifier should be ignored (=1)
                        newHeight = height * (float) mod;
                    }else{
                        newHeight = neighbour.getHeight();
                    }
                    //newHeight = MathUtils.clamp(newHeight, 0, 1);
                    neighbour.setHeight(newHeight);
                    neighbour.setColor(interpolate(1 - newHeight));
                    usedSet.add(neighbour);
                    queue.offer(neighbour);
                }
            }
        } while (height > 0.01 && queue.size() > 0);
    }

    private Color interpolate(float xn) {
        int y1 = 0;
        int y2 = 888;
        int x1 = 0;
        int x2 = 1;
        int yn = (int) (((xn - x1) / (x2 - x1)) * (y2 - y1) + y1);

        return new Color().set(spectralPalette.getPixel(yn, 0));
    }
}
