package app.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import app.io.Protocol;
import app.io.ProtocolBase;
import app.rendering.ColorManager;

public class TestProtocol {
	@Test
	public void testPositionBytes_001() {
		int[] positions = {0, 4, 8, 12, 16, 28, 32, 75, 4096, 65000, 733};
		
		for (int pos : positions) {
			byte[] posBytes = ProtocolBase.getPositionBytes(pos);
			int reversePos = ProtocolBase.getPosition(posBytes[0], posBytes[1]);
			assertEquals(pos, reversePos);
		}
	}
	
	@Test
	public void testPositionBytes_002() {
		int[] positions = {-1, -345, 65537, 634897345};
		
		for (int pos : positions) {
			assertThrows(IllegalArgumentException.class, () -> ProtocolBase.getPositionBytes(pos));
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
		double[] coeffs = {-51.0, 0, 37.0, -127.0, 127.0, 58.0, 65.0, 87.0, -32.0, -1.0, 1.0};
		
		for (double coeff : coeffs) {
			byte b_c = ProtocolBase.getDCTCoeffByte(coeff);
			double convertedByte = ProtocolBase.getDCTCoeff(b_c);
			assertEquals(coeff, convertedByte);
		}
	}
	
	@Test
	public void test_border_color_bytes_001() {
		double[][] vertical = {
				ColorManager.convertRGBToYUV(Color.RED),
				ColorManager.convertRGBToYUV(Color.BLUE),
				ColorManager.convertRGBToYUV(Color.GREEN),
				ColorManager.convertRGBToYUV(Color.YELLOW),
				ColorManager.convertRGBToYUV(Color.MAGENTA)
		};
		
		double[][] horizontal = {
				ColorManager.convertRGBToYUV(Color.MAGENTA),
				ColorManager.convertRGBToYUV(Color.YELLOW),
				ColorManager.convertRGBToYUV(Color.GREEN),
				ColorManager.convertRGBToYUV(Color.BLUE),
				ColorManager.convertRGBToYUV(Color.RED)
		};
		
		final int blockSize = 5; // Because of 5 colors
		byte[] data = Protocol.getBorderColorBytes(vertical, horizontal, blockSize);
		double[][][] decoded = Protocol.getBorderColors(data, blockSize, 0);

		assertArrayEquals(vertical, decoded[1]);
		assertArrayEquals(horizontal, decoded[0]);
	}
	
	@Test
	public void test_size_and_angle_001() {
		int[] sizes = {4, 16, 64, 128, 128, 64, 64, 4, 4, 8, 16, 4};
		int[] angles = {0, 5, 45, 65, 90, 125, 90, 35, 145, 75, 80, 100};
		
		for (int i = 0; i < sizes.length; i++) {
			final int size = sizes[i];
			final int angle = angles[i];
			final byte[] data = Protocol.getSizeAndAngleByte(size, angle);
			final int[] sizeAndAngle = Protocol.getSizeAndAngle(data[0], data[1]);
			assertEquals(size, sizeAndAngle[0]);
			assertEquals(angle, sizeAndAngle[1]);
		}
	}
}
