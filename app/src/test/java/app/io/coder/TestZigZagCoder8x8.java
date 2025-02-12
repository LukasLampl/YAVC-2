package app.io.coder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import app.encoder.MatrixOperations;

public class TestZigZagCoder8x8 {
	private ZigZagCoder coder = new ZigZagCoder8x8();
	
	@Test
	public void testCode_001() {
		double[][] mat = MatrixOperations.generateRandom2DMatrix(8, 255);
		double[] stream = this.coder.code(mat);
		double[][] decode = this.coder.decode(stream);
		assertArrayEquals(mat, decode);
	}
	
	@Test
	public void testCode_002() {
		for (int i = 0; i < 1024; i++) {
			double[][] mat = MatrixOperations.generateRandom2DMatrix(8, 255);
			double[] stream = this.coder.code(mat);
			double[][] decode = this.coder.decode(stream);
			assertArrayEquals(mat, decode);
		}
	}
}
