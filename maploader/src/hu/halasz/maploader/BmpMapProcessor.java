package hu.halasz.maploader;

import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BmpMapProcessor {
    @Getter
    Map<Integer, Province> provinceMap;
    @Getter
    Map<Pixel, Pixel> pixelMap;
    @Getter
    BufferedImage image;

    public BmpMapProcessor() {
        try {
            image = ImageIO.read(new File("hun.bmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        provinceMap = new HashMap<>();
        pixelMap = new HashMap<>();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = image.getRGB(i, j);
                Pixel pixel = new Pixel(i, j, rgb);
                pixelMap.put(pixel, pixel);

                Province province;
                if (!provinceMap.containsKey(rgb)) {
                    province = new Province(rgb);
                    provinceMap.put(rgb, province);
                } else {
                    province = provinceMap.get(rgb);
                }
                province.addPixel(pixel);
            }
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = image.getRGB(i, j);
                Pixel pixel = pixelMap.get(new Pixel(i, j, 0));
                Province province = provinceMap.get(rgb);

                int left = i - 1;
                if (left >= 0) {
                    Pixel leftPixel = pixelMap.get(new Pixel(left, j, 0));
                    addNeighbors(rgb, pixel, province, leftPixel);
                } else {
                    province.addBorderPixel(pixel);
                }

                int right = i + 1;
                if (right < width) {
                    Pixel rightPixel = pixelMap.get(new Pixel(right, j, 0));
                    addNeighbors(rgb, pixel, province, rightPixel);
                } else {
                    province.addBorderPixel(pixel);
                }

                int up = j - 1;
                if (up >= 0) {
                    Pixel upPixel = pixelMap.get(new Pixel(i, up, 0));
                    addNeighbors(rgb, pixel, province, upPixel);
                } else {
                    province.addBorderPixel(pixel);
                }

                int down = j + 1;
                if (down < height) {
                    Pixel downPixel = pixelMap.get(new Pixel(i, down, 0));
                    addNeighbors(rgb, pixel, province, downPixel);
                } else {
                    province.addBorderPixel(pixel);
                }

            }
        }
    }

    private static void addNeighbors(int rgb, Pixel currentPixel, Province province, Pixel neighborPixel) {
        currentPixel.addNeighbourPixel(neighborPixel);
        if (neighborPixel.getRgbId() != rgb) {
            province.addBorderPixel(currentPixel);
        }
    }
}
