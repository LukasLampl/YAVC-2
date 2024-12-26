package app.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import app.io.Protocol;

public class TestProtocol {
	@Test
	public void testPositionBytes_001() {
		int[] positions = {0, 4, 8, 12, 16, 28, 32, 75, 4096, 65000, 733};
		
		for (int pos : positions) {
			byte[] posBytes = Protocol.getPositionBytes(pos);
			int reversePos = Protocol.getPosition(posBytes[0], posBytes[1]);
			assertEquals(pos, reversePos);
		}
	}
	
	@Test
	public void testPositionBytes_002() {
		int[] positions = {-1, -345, 65537, 634897345};
		
		for (int pos : positions) {
			assertThrows(IllegalArgumentException.class, () -> Protocol.getPositionBytes(pos));
		}
	}
	
	@Test
	public void testSpanBytes_001() {
		int[][] spans = {{1, 2}, {0, 17}, {43, 65}, {123, 127}, {-2, -75}, {-127, -34}, {35, -33}};
		
		for (int[] span : spans) {
			byte[] spanBytes = Protocol.getVectorSpanBytes(span[0], span[1]);
			int spanX = Protocol.getVectorSpanInt(spanBytes[0]);
			int spanY = Protocol.getVectorSpanInt(spanBytes[1]);
			assertEquals(span[0], spanX);
			assertEquals(span[1], spanY);
		}
	}
	
	@Test
	public void testSpanBytes_002() {
		int[][] spans = {{-128, 128}, {128, -128}};
		
		for (int[] span : spans) {
			assertThrows(IllegalArgumentException.class, () -> Protocol.getVectorSpanBytes(span[0], span[1]));
		}
	}
	
	@Test
	public void testReferenceAndSizeBytes_001() {
		int[][] refsAndSizes = {{1, 4}, {3, 64}, {2, 128}, {4, 32}};
		
		for (int[] refAndSize : refsAndSizes) {
			byte refAndSizeByte = Protocol.getReferenceAndSizeByte(refAndSize[0], refAndSize[1]);
			int[] resultingRefAndSize = Protocol.getReferenceAndSizeInt(refAndSizeByte);
			assertEquals(refAndSize[0], resultingRefAndSize[0]);
			assertEquals(refAndSize[1], resultingRefAndSize[1]);
		}
	}
	
	@Test
	public void testReferenceAndSizeBytes_002() {
		int[][] refsAndSizes = {{345, 8}, {3, 3}};
		
		for (int[] refAndSize : refsAndSizes) {
			assertThrows(IllegalArgumentException.class, () -> Protocol.getReferenceAndSizeByte(refAndSize[0], refAndSize[1]));
		}
	}
	
	@Test
	public void test_DCT_coefficient_bytes_001() {
		double[] coeffs = {-32767.0, 32767.0, 16799.0, -8354.0, -4096.0, 2040.0, 993.0,
				-51.0, 0, 37.0, -127.0, 127.0, 58.0, 65.0, 87.0, -32.0, -1.0, 1.0,
				-512.0, 239.0, 100.0, 68.0};
		
		for (double coeff : coeffs) {
			byte[] b_c = Protocol.getDCTCoeffByte(coeff);
			double convertedByte = Protocol.getDCTCoeff(b_c[0], b_c[1]);
			assertEquals(coeff, convertedByte);
		}
	}
}
