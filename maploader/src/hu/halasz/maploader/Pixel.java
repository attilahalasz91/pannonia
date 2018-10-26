package hu.halasz.maploader;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Pixel {

    @Getter
    private int x;
    @Getter
    private int y;
    @Getter
    private List<Pixel> neighborPixels = new ArrayList<>();
    @Getter
    private int rgbId;

    public Pixel(int x, int y, int rgbId){
        this.x = x;
        this.y = y;
        this.rgbId = rgbId;
    }

    public void addNeighbourPixel(Pixel pixel){
        neighborPixels.add(pixel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pixel pixel = (Pixel) o;
        return x == pixel.x &&
                y == pixel.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
