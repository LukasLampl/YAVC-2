package app.io.coder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import app.encoder.MatrixOperations;

public class TestZigZagCoder4x4 {
	private ZigZagCoder coder = new ZigZagCoder4x4();
	
	@Test
	public void testCode_001() {
		double[][] mat = MatrixOperations.generateRandom2DMatrix(4, 255);
		double[] stream = this.coder.code(mat, 0, 0);
		double[][] decode = this.coder.decode(stream, 0);
		assertArrayEquals(mat, decode);
	}
	
	@Test
	public void testCode_002() {
		for (int i = 0; i < 1024; i++) {
			double[][] mat = MatrixOperations.generateRandom2DMatrix(4, 255);
			double[] stream = this.coder.code(mat, 0, 0);
			double[][] decode = this.coder.decode(stream, 0);
			assertArrayEquals(mat, decode);
		}
	}
}
