package hu.halasz.maploader;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Province {

    @Getter
    Set<Pixel> borderPixels = new HashSet<>();
    @Getter
    int rgbId;

    public Province(int rgbId){
        this.rgbId = rgbId;
    }

    public void addBorderPixel(Pixel pixel) {
        borderPixels.add(pixel);
    }

}
