package app.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;

import org.junit.jupiter.api.Test;

public class TestColorManager {
	private final Color colorsToTest[] = {
		new Color(255, 0, 0),      // Red
		new Color(0, 255, 0),      // Green
		new Color(0, 0, 255),      // Blue
		new Color(255, 255, 0),    // Yellow
		new Color(255, 0, 255),    // Magenta
		new Color(0, 255, 255),    // Cyan
		new Color(128, 0, 0),      // Maroon
		new Color(0, 128, 0),      // Dark Green
		new Color(0, 0, 128),      // Navy Blue
		new Color(128, 128, 0),    // Olive
		new Color(128, 0, 128),    // Purple
		new Color(0, 128, 128),    // Teal
		new Color(192, 192, 192),  // Silver
		new Color(128, 128, 128),  // Gray
		new Color(255, 165, 0),    // Orange
		new Color(255, 192, 203),  // Pink
		new Color(255, 215, 0),    // Gold
		new Color(75, 0, 130),     // Indigo
		new Color(173, 216, 230),  // Light Blue
		new Color(144, 238, 144),  // Light Green
		new Color(245, 222, 179),  // Wheat
		new Color(255, 69, 0),     // Red-Orange
		new Color(60, 179, 113),   // Medium Sea Green
		new Color(139, 69, 19),    // Saddle Brown
		new Color(250, 128, 114),  // Salmon
		new Color(123, 104, 238),  // Medium Slate Blue
		new Color(255, 99, 71),    // Tomato
		new Color(0, 255, 127),    // Spring Green
		new Color(70, 130, 180),   // Steel Blue
		new Color(233, 150, 122),  // Dark Salmon
		new Color(176, 224, 230),  // Powder Blue
		new Color(238, 130, 238),  // Violet
		new Color(0, 206, 209),    // Dark Turquoise
		new Color(244, 164, 96),   // Sandy Brown
		new Color(199, 21, 133),   // Medium Violet Red
		new Color(255, 228, 181),  // Moccasin
		new Color(32, 178, 170),   // Light Sea Green
		new Color(218, 112, 214),  // Orchid
		new Color(210, 105, 30),   // Chocolate
		new Color(220, 20, 60),    // Crimson
		new Color(255, 218, 185),  // Peach Puff
		new Color(64, 224, 208),   // Turquoise
		new Color(221, 160, 221),  // Plum
		new Color(176, 196, 222),  // Light Steel Blue
		new Color(245, 245, 220),  // Beige
		new Color(119, 136, 153),  // Light Slate Gray
		new Color(255, 235, 205),  // Blanched Almond
		new Color(153, 50, 204),   // Dark Orchid
		new Color(102, 205, 170),  // Medium Aquamarine
		new Color(255, 160, 122)   // Light Salmon
	};
	
	@Test
	public void testConvertRGBToYUV_001() {
		for (Color color : this.colorsToTest) {
			double[] YUV = ColorManager.convertRGBToYUV(color);
			int rgb = ColorManager.convertYUVToRGB(YUV);
			int colorRGB = (0xFF000000 | ((color.getRed() & 0xFF) << 16) | ((color.getGreen() & 0xFF) << 8) | (color.getBlue() & 0xFF));
			assertEquals(colorRGB, rgb);
		}
	}
	
	@Test
	public void testConvertRGBToYUV_002() {
		int[] colors = new int[this.colorsToTest.length];
		
		for (int i = 0; i < this.colorsToTest.length; i++) {
			Color color = this.colorsToTest[i];
			colors[i] = (0xFF000000 | ((color.getRed() & 0xFF) << 16) | ((color.getGreen() & 0xFF) << 8) | (color.getBlue() & 0xFF));
		}
		
		for (int color : colors) {
			double[] YUV = ColorManager.convertRGBToYUV(color);
			int rgb = ColorManager.convertYUVToRGB(YUV);
			assertEquals(color, rgb);
		}
	}
	
	@Test
	public void testConvertYUVToRGB_intARR_001() {
		for (Color color : this.colorsToTest) {
			double[] YUV = ColorManager.convertRGBToYUV(color);
			int[] rgb = ColorManager.convertYUVToRGB_intARR(YUV, null);
			assertEquals(color.getRed(), rgb[0]);
			assertEquals(color.getGreen(), rgb[1]);
			assertEquals(color.getBlue(), rgb[2]);
		}
	}
	
	@Test
	public void testConvertYUVToRGB_intARR_002() {
		int[] cache = new int[3];
		
		for (Color color : this.colorsToTest) {
			double[] YUV = ColorManager.convertRGBToYUV(color);
			ColorManager.convertYUVToRGB_intARR(YUV, cache);
			assertEquals(color.getRed(), cache[0]);
			assertEquals(color.getGreen(), cache[1]);
			assertEquals(color.getBlue(), cache[2]);
		}
	}
}
