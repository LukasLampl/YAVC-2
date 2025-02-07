package app.engines.dct.fct;

import org.junit.jupiter.api.Test;

import app.engines.dct.DCTEngine;

public class Test8x8FCT extends TestFCTBase {
	private static final int N = 8;
	private static FCT FCT8x8 = new FCT8x8();
	
	@Test
	public void test001() {
		double[][] arr = super.generateFilledMatrix(N, 0);
		double[][] copy = super.getCopy(arr);
		FCT8x8.fct2D(arr, 0, 0);
		FCT8x8.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test002() {
		double[][] arr = super.generateFilledMatrix(N, 128);
		double[][] copy = super.getCopy(arr);
		FCT8x8.fct2D(arr, 0, 0);
		FCT8x8.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test003() {
		double[][] arr = super.generateFilledMatrix(N, 255);
		double[][] copy = super.getCopy(arr);
		FCT8x8.fct2D(arr, 0, 0);
		FCT8x8.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test004() {
		double[][] arr = super.generateXResonatingMatrix(N, 0, 255);
		double[][] copy = super.getCopy(arr);
		FCT8x8.fct2D(arr, 0, 0);
		FCT8x8.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test005() {
		double[][] arr = super.generateYResonatingMatrix(N, 0, 255);
		double[][] copy = super.getCopy(arr);
		FCT8x8.fct2D(arr, 0, 0);
		FCT8x8.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test006() {
		double[][] arr = super.generateCheckerboardMatrix(N, 0, 255);
		double[][] copy = super.getCopy(arr);
		FCT8x8.fct2D(arr, 0, 0);
		FCT8x8.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test008() {
		DCTEngine e = new DCTEngine();
		
		int steps = 1;
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		double max_q = Double.MIN_VALUE;
		double min_q = Double.MAX_VALUE;
		
		for (int i = 0; i < steps; i++) {
			double[][] arr = super.generateRandomMatrix(N, 5);
			double[][] copy = super.getCopy(arr);
			FCT8x8.fct2D(arr, 0, 0);
			
			for (int x = 0; x < N; x++) {
				for (int y = 0; y < N; y++) {
					if (arr[x][y] < min) {
						min = arr[x][y];
					} else if (arr[x][y] > max) {
						max = arr[x][y];
					}
				}
			}
			
			double[][] intermed = super.getCopy(arr);
			e.quantizeLumaDCTCoefficients(intermed, N, 0, 0);
			
			for (int x = 0; x < N; x++) {
				for (int y = 0; y < N; y++) {
					if (intermed[x][y] < min_q) {
						min_q = intermed[x][y];
					} else if (intermed[x][y] > max_q) {
						max_q = intermed[x][y];
					}
				}
			}
			
			FCT8x8.ifct2D(arr, 0, 0);
			super.assertArray(copy, arr);
		}
		
		System.out.println("Max val: " + max + "; Min val: " + min);
		System.out.println("Max q val: " + max_q + "; Min q val: " + min_q);
	}
}
