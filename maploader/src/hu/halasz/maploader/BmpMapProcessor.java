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

        int leftRgb;
        int rightRgb;
        int upRgb;
        int downRgb;
        int rgb;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                rgb = image.getRGB(i, j);

                Province province;
                if (!provinceMap.containsKey(rgb)) {
                    province = new Province(rgb);
                    provinceMap.put(rgb, province);
                } else {
                    province = provinceMap.get(rgb);
                }

                int left = i - 1;
                if (left >= 0) {
                    leftRgb = image.getRGB(left, j);
                    if (leftRgb != rgb) {
                        province.addBorderPixel(new Pixel(i, j, rgb));
                    }
                } else {
                    province.addBorderPixel(new Pixel(i, j, rgb));
                }

                int right = i + 1;
                if (right < width) {
                    rightRgb =  image.getRGB(right, j);
                    if (rightRgb != rgb) {
                        province.addBorderPixel(new Pixel(i, j, rgb));
                    }
                } else {
                    province.addBorderPixel(new Pixel(i, j, rgb));
                }

                int up = j - 1;
                if (up >= 0) {
                    upRgb =  image.getRGB(i, up);
                    if (upRgb != rgb) {
                        province.addBorderPixel(new Pixel(i, j, rgb));
                    }
                } else {
                    province.addBorderPixel(new Pixel(i, j, rgb));
                }

                int down = j + 1;
                if (down < height) {
                    downRgb = image.getRGB(i, down);
                    if (downRgb != rgb) {
                        province.addBorderPixel(new Pixel(i, j, rgb));
                    }
                } else {
                    province.addBorderPixel(new Pixel(i, j, rgb));
                }
            }
        }
    }
}
