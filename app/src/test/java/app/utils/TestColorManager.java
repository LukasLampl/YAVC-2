package app.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.rendering.ColorManager;

public class TestColorManager {
	private static int INCREMENT = 1;
	private final static List<Color> colorsToTest = new ArrayList<Color>();
	
	static {
		for (int r = 0; r <= 0xFF; r += INCREMENT) {
			for (int g = 0; g <= 0xFF; g += INCREMENT) {
				for (int b = 0; b <= 0xFF; b += INCREMENT) {
					colorsToTest.add(new Color(r, g, b));
				}
			}
		}
		
		System.out.println("Initialized " + colorsToTest.size() + " color to test.");
		System.out.println(String.format(" > Equivalent of %.2f%% of the total RGB space.",
				((((double)colorsToTest.size()) / (double)(Math.pow(256, 3))) * 100)));
	}
	
	@Test
	public void testConvertRGBToYUV_001() {
		for (Color color : colorsToTest) {
			double[] YUV = ColorManager.convertRGBToYUV(color);
			int rgb = ColorManager.convertYUVToRGB(YUV);
			int colorRGB = getRGBOfColor(color);
			assertEquals(colorRGB, rgb);
		}
	}
	
	@Test
	public void testConvertRGBToYUV_002() {
		int[] colors = new int[colorsToTest.size()];
		
		for (int i = 0; i < colorsToTest.size(); i++) {
			Color color = colorsToTest.get(i);
			colors[i] = getRGBOfColor(color);
		}
		
		for (int color : colors) {
			double[] YUV = ColorManager.convertRGBToYUV(color);
			int rgb = ColorManager.convertYUVToRGB(YUV);
			assertEquals(color, rgb);
		}
	}
	
	@Test
	public void testConvertYUVToRGB_intARR_001() {
		for (Color color : colorsToTest) {
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
		
		for (Color color : colorsToTest) {
			double[] YUV = ColorManager.convertRGBToYUV(color);
			ColorManager.convertYUVToRGB_intARR(YUV, cache);
			assertEquals(color.getRed(), cache[0]);
			assertEquals(color.getGreen(), cache[1]);
			assertEquals(color.getBlue(), cache[2]);
		}
	}
	
	@Test
	public void testConversion_001() {
		int[] cache = new int[3];
		
		for (Color color : colorsToTest) {
			double[] YUV = ColorManager.convertRGBToYUV(color);
			ColorManager.convertYUVToRGB_intARR(YUV, cache);
			int rgb = getRGBOfColor(cache);
			double[] convertedYUV = ColorManager.convertRGBToYUV(rgb);
			assertArrayEquals(YUV, convertedYUV);
		}
	}
	
	@Test
	public void testConversion_002() {
		int[] cache = new int[3];
		int counter = 0;
		int percent_5 = MathUtils.round(colorsToTest.size() * 0.05);
		
		for (Color color : colorsToTest) {
			if (counter % percent_5 == 0) {
				System.out.println(String.format("Conversion 2 status: %.2f%% checked.",
						((double)counter / (double)colorsToTest.size()) * 100));
			}
			
			double[] YUV = ColorManager.convertRGBToYUV(color);
			double[] convertedYUV = YUV;
			
			for (int i = 0; i < 120; i++) {
				ColorManager.convertYUVToRGB_intARR(convertedYUV, cache);
				int rgb = getRGBOfColor(cache);
				convertedYUV = ColorManager.convertRGBToYUV(rgb);
			}
			
			assertArrayEquals(YUV, convertedYUV);
			counter++;
		}
	}
	
	private int getRGBOfColor(int[] color) {
		return 0xFF000000 | ((color[ColorManager.R_INDEX] & 0xFF) << 16)
				| ((color[ColorManager.G_INDEX] & 0xFF) << 8)
				| (color[ColorManager.B_INDEX] & 0xFF);
	}
	
	private int getRGBOfColor(Color color) {
		return 0xFF000000 | ((color.getRed() & 0xFF) << 16)
				| ((color.getGreen() & 0xFF) << 8)
				| (color.getBlue() & 0xFF);
	}
}
