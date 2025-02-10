package app.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
	
	@Test
	public void testTranspose001() {
		double[][] mat = {
				{1, 2, 3, 4},
				{5, 6, 7, 8},
				{9, 10, 11, 12},
				{13, 14, 15, 16}
		};
		
		double[][] awaited = {
				{1, 5, 9, 13},
				{2, 6, 10, 14},
				{3, 7, 11, 15},
				{4, 8, 12, 16}
		};
		
		ArrayUtils.transpose(mat, 4, 0, 0);
		assertArrayEquals(awaited, mat);
	}
	
	@Test
	public void testTranspose002() {
		double[][] mat = {
				{ 1,  2,  3,  4, 	 5,  6,  7,  8},
				{ 9, 10, 11, 12, 	13, 14, 15, 16},
				{17, 18, 19, 20, 	21, 22, 23, 24},
				{25, 26, 27, 28, 	29, 30, 31, 32},
				
				{33, 34, 35, 36, 	37, 38, 39, 40},
				{41, 42, 43, 44, 	45, 46, 47, 48},
				{49, 50, 51, 52, 	53, 54, 55, 56},
				{57, 58, 59, 60, 	61, 62, 63, 64}
		};
		
		double[][] awaited = {
				{ 1,  2,  3,  4, 	 5,  6,  7,  8},
				{ 9, 10, 11, 12, 	13, 14, 15, 16},
				{17, 18, 19, 20, 	21, 22, 23, 24},
				{25, 26, 27, 28, 	29, 30, 31, 32},
				
				{33, 41, 49, 57, 	37, 38, 39, 40},
				{34, 42, 50, 58, 	45, 46, 47, 48},
				{35, 43, 51, 59, 	53, 54, 55, 56},
				{36, 44, 52, 60, 	61, 62, 63, 64}
		};
		
		ArrayUtils.transpose(mat, 4, 4, 0);
		assertArrayEquals(awaited, mat);
	}
	
	@Test
	public void testTranspose003() {
		double[][] mat = {
				{ 1,  2,  3,  4, 	 5,  6,  7,  8},
				{ 9, 10, 11, 12, 	13, 14, 15, 16},
				{17, 18, 19, 20, 	21, 22, 23, 24},
				{25, 26, 27, 28, 	29, 30, 31, 32},
				
				{33, 34, 35, 36, 	37, 38, 39, 40},
				{41, 42, 43, 44, 	45, 46, 47, 48},
				{49, 50, 51, 52, 	53, 54, 55, 56},
				{57, 58, 59, 60, 	61, 62, 63, 64}
		};
		
		double[][] awaited = {
				{ 1,  2,  3,  4, 	 5,  6,  7,  8},
				{ 9, 10, 11, 12, 	13, 14, 15, 16},
				{17, 18, 19, 20, 	21, 22, 23, 24},
				{25, 26, 27, 28, 	29, 30, 31, 32},
				
				{33, 34, 35, 36, 	37, 45, 53, 61},
				{41, 42, 43, 44, 	38, 46, 54, 62},
				{49, 50, 51, 52, 	39, 47, 55, 63},
				{57, 58, 59, 60, 	40, 48, 56, 64}
		};
		
		ArrayUtils.transpose(mat, 4, 4, 4);
		assertArrayEquals(awaited, mat);
	}
	
	@Test
	public void testTranspose004() {
		double[][] mat = {
			{  1,   2,   3,   4,	   5,   6,   7,   8,	   9,  10,  11,  12,	  13,  14,  15,  16},
			{ 17,  18,  19,  20,	  21,  22,  23,  24,	  25,  26,  27,  28,	  29,  30,  31,  32},
			{ 33,  34,  35,  36,	  37,  38,  39,  40,	  41,  42,  43,  44,	  45,  46,  47,  48},
			{ 49,  50,  51,  52,	  53,  54,  55,  56,	  57,  58,  59,  60,	  61,  62,  63,  64},
			
			{ 65,  66,  67,  68,	  69,  70,  71,  72,	  73,  74,  75,  76,	  77,  78,  79,  80},
			{ 81,  82,  83,  84,	  85,  86,  87,  88,	  89,  90,  91,  92,	  93,  94,  95,  96},
			{ 97,  98,  99, 100,	 101, 102, 103, 104,	 105, 106, 107, 108,	 109, 110, 111, 112},
			{113, 114, 115, 116,	 117, 118, 119, 120,	 121, 122, 123, 124,	 125, 126, 127, 128},
			
			{129, 130, 131, 132,	 133, 134, 135, 136,	 137, 138, 139, 140,	 141, 142, 143, 144},
			{145, 146, 147, 148,	 149, 150, 151, 152,	 153, 154, 155, 156,	 157, 158, 159, 160},
			{161, 162, 163, 164,	 165, 166, 167, 168,	 169, 170, 171, 172,	 173, 174, 175, 176},
			{177, 178, 179, 180,	 181, 182, 183, 184,	 185, 186, 187, 188,	 189, 190, 191, 192},
			
			{193, 194, 195, 196,	 197, 198, 199, 200,	 201, 202, 203, 204,	 205, 206, 207, 208},
			{209, 210, 211, 212,	 213, 214, 215, 216,	 217, 218, 219, 220,	 221, 222, 223, 224},
			{225, 226, 227, 228,	 229, 230, 231, 232,	 233, 234, 235, 236,	 237, 238, 239, 240},
			{241, 242, 243, 244,	 245, 246, 247, 248,	 249, 250, 251, 252,	 253, 254, 255, 256}
		};

		
		double[][] awaited = {
			{  1,   2,   3,   4,	   5,   6,   7,   8,	   9,  10,  11,  12,	  13,  14,  15,  16},
			{ 17,  18,  19,  20,	  21,  22,  23,  24,	  25,  26,  27,  28,	  29,  30,  31,  32},
			{ 33,  34,  35,  36,	  37,  38,  39,  40,	  41,  42,  43,  44,	  45,  46,  47,  48},
			{ 49,  50,  51,  52,	  53,  54,  55,  56,	  57,  58,  59,  60,	  61,  62,  63,  64},
			
			{ 65,  66,  67,  68,	  69,  70,  71,  72,	  73,  74,  75,  76,	  77,  78,  79,  80},
			{ 81,  82,  83,  84,	  85,  86,  87,  88,	  89,  90,  91,  92,	  93,  94,  95,  96},
			{ 97,  98,  99, 100,	 101, 102, 103, 104,	 105, 106, 107, 108,	 109, 110, 111, 112},
			{113, 114, 115, 116,	 117, 118, 119, 120,	 121, 122, 123, 124,	 125, 126, 127, 128},
			
			{129, 130, 131, 132,	 133, 134, 135, 136,	 137, 138, 139, 140,	 141, 142, 143, 144},
			{145, 146, 147, 148,	 149, 150, 151, 152,	 153, 154, 155, 156,	 157, 158, 159, 160},
			{161, 162, 163, 164,	 165, 166, 167, 168,	 169, 170, 171, 172,	 173, 174, 175, 176},
			{177, 178, 179, 180,	 181, 182, 183, 184,	 185, 186, 187, 188,	 189, 190, 191, 192},
			
			{193, 194, 195, 196,	 197, 198, 199, 200,	 201, 217, 233, 249,	 205, 206, 207, 208},
			{209, 210, 211, 212,	 213, 214, 215, 216,	 202, 218, 234, 250,	 221, 222, 223, 224},
			{225, 226, 227, 228,	 229, 230, 231, 232,	 203, 219, 235, 251,	 237, 238, 239, 240},
			{241, 242, 243, 244,	 245, 246, 247, 248,	 204, 220, 236, 252,	 253, 254, 255, 256}
		};
		
		ArrayUtils.transpose(mat, 4, 12, 8);
		assertArrayEquals(awaited, mat);
	}
	
	@Test
	public void testTranspose005() {
		double[][] mat = {
				{1, 2, 3, 4},
				{5, 6, 7, 8},
				{9, 10, 11, 12},
				{13, 14, 15, 16}
		};
		
		double[][] copy = {
				{1, 2, 3, 4},
				{5, 6, 7, 8},
				{9, 10, 11, 12},
				{13, 14, 15, 16}
		};
		
		double[][] temp = new double[4][4];
		
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				temp[y][x] = copy[x][y];
			}
		}
		
		ArrayUtils.transpose(mat, 4, 0, 0);
		
		assertArrayEquals(temp, mat);
	}
}
