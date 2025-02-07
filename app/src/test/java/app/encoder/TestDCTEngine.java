package app.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import app.engines.dct.DCTEngine;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;

public class TestDCTEngine {
	private DCTEngine dE = new DCTEngine();
	private Random random = new Random();
	private double DELTA = 0.001;
	
	@Test
	public void testDCTCoefficients() {
		int[] sizes = {4, 8, 16, 32, 64, 128};
		
		for (int size : sizes) {
			System.out.println("Current size: " + size + "x" + size);
			checkMatrixCoeffs(size, true, null);
		}
	}
	
	@Test
	public void testDCTCoefficientsTime() {
		double[][][] matrix_8x8 = ArrayUtils.get3DArray(8, true);
		double[][][] matrix_128x128 = ArrayUtils.get3DArray(128, true);
		
		long start_t = System.currentTimeMillis();
		
		for (int i = 0; i < 256; i++) {
			checkMatrixCoeffs(8, false, matrix_8x8);
		}
		
		long end_t = System.currentTimeMillis();
		long start_t1 = System.currentTimeMillis();
		checkMatrixCoeffs(128, false, matrix_128x128);
		long end_t1 = System.currentTimeMillis();
		System.out.println("Time comparison:");
		System.out.println("   256 - 8x8 blocks: " + (end_t - start_t) + "ms");
		System.out.println("   1 - 128x128 block: " + (end_t1 - start_t1) + "ms");
	}
	
	private void checkMatrixCoeffs(int size, boolean verbose, double[][][] matrix) {
		if (matrix == null) {
			matrix = ArrayUtils.get3DArray(size, true);
		}
		
		if (verbose) {
			System.out.println(" > Filling out matrix");
		}
		
		fillMatrix(matrix);
		double[][][] comparable = ArrayUtils.get3DArray(size, true);
		ArrayUtils.copy3DArray(matrix, 0, 0, 0, comparable, 0, 0, 0, size, size, ColorManager.CHANNELS, true);
		
		if (verbose) {
			System.out.println(" > Getting DCT-Coeffs");
		}
		
		dE.computeDCTOfDeltas(matrix, size, false);
		
		if (verbose) {
			System.out.println(" > Getting IDCT-Coeffs");
		}
		
		dE.computeIDCTOfDeltas(matrix, size, false);
		
		if (verbose) {
			System.out.println(" > Check values");
		}
		
		for (int n = 0; n < matrix.length; n++) {
			for (int i = 0; i < matrix[n].length; i++) {
				assertArrayEquals(matrix[n][i], comparable[n][i], DELTA);
			}
		}
	}
	
	@Test
	public void testMatrixTranslation() {
		testMatrixTranslationTask(4);
		testMatrixTranslationTask(8);
		testMatrixTranslationTask(16);
		testMatrixTranslationTask(32);
		testMatrixTranslationTask(64);
		testMatrixTranslationTask(128);
	}
	
	private void testMatrixTranslationTask(int size) {
		double[][][] matrix = ArrayUtils.get3DArray(size, true);
		fillMatrix(matrix);
		double[][][] comparable = ArrayUtils.get3DArray(size, true);
		ArrayUtils.copy3DArray(matrix, 0, 0, 0, comparable, 0, 0, 0, size, size, ColorManager.CHANNELS, true);
		
		dE.computeDCTOfDeltas(matrix, size, false);
		assertEquals(comparable.length, matrix.length);
		
		for (int i = 0; i < matrix.length; i++) {
			assertEquals(comparable[i].length, matrix[i].length);
			
			for (int n = 0; n < matrix[i].length; n++) {
				assertEquals(comparable[i][n].length, matrix[i][n].length);
			}
		}
		
		dE.computeIDCTOfDeltas(matrix, size, false);
		
		for (int n = 0; n < matrix.length; n++) {
			for (int i = 0; i < matrix[n].length; i++) {
				assertArrayEquals(comparable[n][i], matrix[n][i], DELTA);
			}
		}
	}
	
	private void fillMatrix(double[][][] matrix) {
		for (int x = 0; x < matrix[ColorManager.Y_INDEX].length; x++) {
			for (int y = 0; y < matrix[ColorManager.Y_INDEX][x].length; y++) {
				matrix[ColorManager.Y_INDEX][x][y] = this.random.nextDouble() * 255;
			}
		}
		
		for (int x = 0; x < matrix[ColorManager.U_INDEX].length; x++) {
			for (int y = 0; y < matrix[ColorManager.U_INDEX][x].length; y++) {
				matrix[ColorManager.U_INDEX][x][y] = this.random.nextDouble() * 255;
				matrix[ColorManager.V_INDEX][x][y] = this.random.nextDouble() * 255;
			}
		}
	}
}
