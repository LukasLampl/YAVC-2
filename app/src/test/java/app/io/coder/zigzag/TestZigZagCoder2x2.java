package app.io.coder.zigzag;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import app.encoder.MatrixOperations;
import app.exceptions.DCTCoefficientOutOfBoundsException;
import app.io.coder.zigzag.ZigZagCoder;
import app.io.coder.zigzag.ZigZagCoder2x2;

public class TestZigZagCoder2x2 {
	private ZigZagCoder coder = new ZigZagCoder2x2();
	
	@Test
	public void testCode_001() throws DCTCoefficientOutOfBoundsException {
		double[][] mat = MatrixOperations.generateRoundedRandom2DMatrix(2, 127);
		byte[] stream = this.coder.code(mat, 0, 0);
		double[][] decode = this.coder.decode(stream, 0);
		assertArrayEquals(mat, decode);
	}
	
	@Test
	public void testCode_002() throws DCTCoefficientOutOfBoundsException {
		for (int i = 0; i < 1024; i++) {
			double[][] mat = MatrixOperations.generateRoundedRandom2DMatrix(2, 127);
			byte[] stream = this.coder.code(mat, 0, 0);
			double[][] decode = this.coder.decode(stream, 0);
			assertArrayEquals(mat, decode);
		}
	}
}
