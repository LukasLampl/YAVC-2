package app.engines.dct.fct;

import org.junit.jupiter.api.Test;

public class Test4x4FCT extends TestFCTBase {
	private static final int N = 4;
	private static FCT FCT4x4 = new FCT4x4();
	
	@Test
	public void test001() {
		double[][] arr = super.generateFilledMatrix(N, 0);
		double[][] copy = super.getCopy(arr);
		FCT4x4.fct2D(arr, 0, 0);
		FCT4x4.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test002() {
		double[][] arr = super.generateFilledMatrix(N, 128);
		double[][] copy = super.getCopy(arr);
		FCT4x4.fct2D(arr, 0, 0);
		FCT4x4.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test003() {
		double[][] arr = super.generateFilledMatrix(N, 255);
		double[][] copy = super.getCopy(arr);
		FCT4x4.fct2D(arr, 0, 0);
		FCT4x4.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test004() {
		double[][] arr = super.generateFilledMatrix(N, 256);
		double[][] copy = super.getCopy(arr);
		FCT4x4.fct2D(arr, 0, 0);
		FCT4x4.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test005() {
		double[][] arr = super.generateXResonatingMatrix(N, 0, 255);
		double[][] copy = super.getCopy(arr);
		FCT4x4.fct2D(arr, 0, 0);
		FCT4x4.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test006() {
		double[][] arr = super.generateYResonatingMatrix(N, 0, 255);
		double[][] copy = super.getCopy(arr);
		FCT4x4.fct2D(arr, 0, 0);
		FCT4x4.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
	
	@Test
	public void test007() {
		double[][] arr = super.generateCheckerboardMatrix(N, 0, 255);
		double[][] copy = super.getCopy(arr);
		FCT4x4.fct2D(arr, 0, 0);
		FCT4x4.ifct2D(arr, 0, 0);
		super.assertArray(copy, arr);
	}
}
