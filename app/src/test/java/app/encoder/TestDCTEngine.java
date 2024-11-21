package app.encoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

import app.dct.DCTEngine;
import app.rendering.ColorManager;

public class TestDCTEngine {
	private Random random = new Random();
	private double DELTA = 5.0;
	
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
		double[][][] matrix_8x8 = new double[3][][];
		matrix_8x8[ColorManager.Y_INDEX] = new double[8][8];
		matrix_8x8[ColorManager.U_INDEX] = new double[4][4];
		matrix_8x8[ColorManager.V_INDEX] = new double[4][4];
		
		double[][][] matrix_128x128 = new double[3][][];
		matrix_128x128[ColorManager.Y_INDEX] = new double[128][128];
		matrix_128x128[ColorManager.U_INDEX] = new double[64][64];
		matrix_128x128[ColorManager.V_INDEX] = new double[64][64];
		
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
			matrix = new double[3][][];
			matrix[ColorManager.Y_INDEX] = new double[size][size];
			matrix[ColorManager.U_INDEX] = new double[size / 2][size / 2];
			matrix[ColorManager.V_INDEX] = new double[size / 2][size / 2];
		}
		
		if (verbose) {
			System.out.println(" > Filling out matrix");
		}
		
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
		
		if (verbose) {
			System.out.println(" > Getting DCT-Coeffs");
		}
		
		ArrayList<double[][][]> coeffs = dE.computeDCTOfVectorColorDifference(matrix, size);
		
		if (verbose) {
			System.out.println(" > Getting IDCT-Coeffs");
		}
		
		double[][][] resultingMatrix = dE.computeIDCTOfVectorColorDifference(coeffs, size);
		
		if (verbose) {
			System.out.println(" > Check values");
		}
		
		for (int x = 0; x < resultingMatrix[ColorManager.Y_INDEX].length; x++) {
			for (int y = 0; y < resultingMatrix[ColorManager.Y_INDEX][x].length; y++) {
				double rC = resultingMatrix[ColorManager.Y_INDEX][x][y];
				double oC = matrix[ColorManager.Y_INDEX][x][y];
				assertEquals(oC, rC, DELTA);
			}
		}
		
		for (int x = 0; x < resultingMatrix[ColorManager.U_INDEX].length; x++) {
			for (int y = 0; y < resultingMatrix[ColorManager.U_INDEX][x].length; y++) {
				double rCU = resultingMatrix[ColorManager.U_INDEX][x][y];
				double rCV = resultingMatrix[ColorManager.V_INDEX][x][y];
				double oCU = matrix[ColorManager.U_INDEX][x][y];
				double oCV = matrix[ColorManager.V_INDEX][x][y];
				assertEquals(oCU, rCU, DELTA);
				assertEquals(oCV, rCV, DELTA);
			}
		}
	}
}
