package app.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import app.engines.dct.DCTEngine;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;

public class TestDCTEngine {
	private Random random = new Random();
	private double DELTA = 0.05;
	
	@Test
	public void testStep() {
		class MockDCTEngine extends DCTEngine {
			private int[] x = {0, 0, 0, 0, 12, 23, 4, 6, 6, 3, 8, 1, 42, 67, 8, 0};
			private int[] m = {2, 4, 32, 16, 64, 64, 64, 2, 4, 128, 4, 4, 8, 16, 32, 32};
			
			public void run() {
				for (int i = 0; i < x.length; i++) {
					double expected = x[i] == 0 ? 1.0 / Math.sqrt(m[i]) : Math.sqrt(2.0 / (double)m[i]);
					assertEquals(super.step(x[i], m[i]), expected, 0.001);
				}
			}
		}
		
		MockDCTEngine e = new MockDCTEngine();
		e.run();
	}
	
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
	
	private void checkMatrixCoeffs(int size, boolean verbose, double[][][] cache) {
		DCTEngine dE = new DCTEngine();
		double[][][] matrix = cache;
		
		if (matrix == null) {
			matrix = ArrayUtils.get3DArray(size, true);
		}
		
		if (verbose) {
			System.out.println(" > Filling out matrix");
		}
		
		fillMatrix(matrix);
		
		if (verbose) {
			System.out.println(" > Getting DCT-Coeffs");
		}
		
		double[][][] coeffs = dE.computeDCTOfVectorColorDifference(matrix, size, false);
		
		if (verbose) {
			System.out.println(" > Getting IDCT-Coeffs");
		}
		
		double[][][] resultingMatrix = dE.computeIDCTOfVectorColorDifference(coeffs, size, false);
		
		if (verbose) {
			System.out.println(" > Check values");
		}
		
		for (int n = 0; n < matrix.length; n++) {
			for (int i = 0; i < matrix[n].length; i++) {
				assertArrayEquals(matrix[n][i], resultingMatrix[n][i], DELTA);
			}
		}
	}
	
	@Test
	public void testMatrixTranslation() {
		int[] sizes = {4, 8, 16, 32, 64, 128};
		
		for (int size : sizes) {
			testMatrixTranslationTask(size);
		}
	}
	
	private void testMatrixTranslationTask(int size) {
		DCTEngine dE = new DCTEngine();
		double[][][] matrix = ArrayUtils.get3DArray(size, true);
		fillMatrix(matrix);
		
		double[][][] converted = dE.computeDCTOfVectorColorDifference(matrix, size, false);
		assertEquals(matrix.length, converted.length);
		
		for (int i = 0; i < matrix.length; i++) {
			assertEquals(matrix[i].length, converted[i].length);
			
			for (int n = 0; n < matrix[i].length; n++) {
				assertEquals(matrix[i][n].length, converted[i][n].length);
			}
		}
		
		double[][][] decoded = dE.computeIDCTOfVectorColorDifference(converted, size, false);
		
		for (int n = 0; n < matrix.length; n++) {
			for (int i = 0; i < matrix[n].length; i++) {
				assertArrayEquals(matrix[n][i], decoded[n][i], DELTA);
			}
		}
	}
	
	private void fillMatrix(double[][][] matrix) {
		for (int x = 0; x < matrix[ColorManager.Y_INDEX].length; x++) {
			for (int y = 0; y < matrix[ColorManager.Y_INDEX][x].length; y++) {
				matrix[ColorManager.Y_INDEX][x][y] = this.random.nextDouble();
			}
		}
		
		for (int x = 0; x < matrix[ColorManager.U_INDEX].length; x++) {
			for (int y = 0; y < matrix[ColorManager.U_INDEX][x].length; y++) {
				matrix[ColorManager.U_INDEX][x][y] = this.random.nextDouble();
				matrix[ColorManager.V_INDEX][x][y] = this.random.nextDouble();
			}
		}
	}
}
