package app.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class TestArrayUtils {
	private Random random = new Random();
	
	@Test
	public void testCopy2DArray001() {
		int width = 128;
		int height = 128;
		double[][] arr1 = new double[width][height];
		double[][] arr2 = new double[width][height];
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				arr1[x][y] = this.random.nextDouble();
			}
		}
		
		ArrayUtils.copy2DArray(arr1, 0, 0, arr2, 0, 0, width, height);
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				assertEquals(arr1[x][y], arr2[x][y]);
			}
		}
	}
	
	@Test
	public void testCopy2DArray002() {
		int width = 128;
		int height = 128;
		int offsetX = 24;
		int offsetY = 48;
		
		double[][] arr1 = new double[width][height];
		double[][] arr2 = new double[width + offsetX][height + offsetY];
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				arr1[x][y] = this.random.nextDouble();
			}
		}
		
		ArrayUtils.copy2DArray(arr1, 0, 0, arr2, offsetX, offsetY, width, height);
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				assertEquals(arr1[x][y], arr2[x + offsetX][y + offsetY]);
			}
		}
	}

	@Test
	public void testCopy2DArray003() {
		int width = 64;
		int height = 64;
		int offsetX = 24;
		int offsetY = 48;
		
		double[][] arr1 = new double[width + offsetX][height + offsetY];
		double[][] arr2 = new double[width][height];
		
		for (int x = 0; x < width + offsetX; x++) {
			for (int y = 0; y < height + offsetY; y++) {
				arr1[x][y] = this.random.nextDouble();
			}
		}
		
		ArrayUtils.copy2DArray(arr1, offsetX, offsetY, arr2, 0, 0, width, height);
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				assertEquals(arr1[x + offsetX][y + offsetY], arr2[x][y]);
			}
		}
	}
}
