/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2025  Lukas Nian En Lampl, Hans Lampl

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package app.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.util.Random;

import org.junit.jupiter.api.Test;

import app.encoder.MatrixOperations;
import app.exceptions.DCTCoefficientOutOfBoundsException;
import app.io.coder.cabac.CABAC;
import app.io.coder.cabac.ContextModelManager;
import app.io.coder.cabac.ContextModelManager.ResidualType;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.MathUtils;

public class TestProtocol {
	private Random random = new Random();
	
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
	public void testDCTCoefficientBytes_001() {
		double[] coeffs = {-51.0, 0, 37.0, -127.0, 127.0, 58.0, 65.0, 87.0, -32.0, -1.0, 1.0};
		
		for (double coeff : coeffs) {
			byte b_c = ProtocolBase.getDCTCoeffByte(coeff);
			double convertedByte = ProtocolBase.getDCTCoeff(b_c);
			assertEquals(coeff, convertedByte);
		}
	}
	
	@Test
	public void testBorderColorBytes_001() {
		double[][] vertical = {
				ColorManager.convertRGBToYUV(Color.RED),
				ColorManager.convertRGBToYUV(Color.BLUE),
				ColorManager.convertRGBToYUV(Color.GREEN),
				ColorManager.convertRGBToYUV(Color.YELLOW),
				ColorManager.convertRGBToYUV(Color.MAGENTA)
		};
		
		double[][] horizontal = {
				ColorManager.convertRGBToYUV(Color.BLACK),
				ColorManager.convertRGBToYUV(Color.PINK),
				ColorManager.convertRGBToYUV(Color.ORANGE),
				ColorManager.convertRGBToYUV(Color.CYAN),
				ColorManager.convertRGBToYUV(Color.GRAY)
		};
		
		final int blockSize = 5; // Because of 5 colors
		byte[] data = Protocol.getBorderColorBytes(vertical, horizontal, blockSize);
		double[][][] decoded = Protocol.getBorderColors(data, blockSize, 0);

		assertArrayEquals(vertical, decoded[0]);
		assertArrayEquals(horizontal, decoded[1]);
	}
	
	@Test
	public void testBorderColorBytes_002() {
		int[] sizes = {4, 8, 16, 32, 64, 128};
		
		for (final int size : sizes) {
			for (int i = 0; i < 256; i++) {
				double[][] vertical = generateRandomArray(size, 255);
				double[][] horizontal = generateRandomArray(size, 255);
				
				byte[] data = Protocol.getBorderColorBytes(vertical, horizontal, size);
				double[][][] decoded = Protocol.getBorderColors(data, size, 0);
				assertArrayEquals(vertical, decoded[0]);
				assertArrayEquals(horizontal, decoded[1]);
			}
		}
	}
	
	private double[][] generateRandomArray(final int size, final double scale) {
		double[][] arr = new double[size][ColorManager.CHANNELS];
		
		for (int i = 0; i < size; i++) {
			int r = MathUtils.round(Math.random() * scale);
			int g = MathUtils.round(Math.random() * scale);
			int b = MathUtils.round(Math.random() * scale);
			arr[i] = ColorManager.convertRGBToYUV(new Color(r, b, g));
		}
		
		return arr;
	}

	private double[][][] generateRandom3DArray(final int size, final int min, final int max) {
		double[][][] arr = ArrayUtils.get3DArray(size, true);
		
		for (int i = 0; i < arr.length; i++) {
			for (int x = 0; x < arr[i].length; x++) {
				for (int y = 0; y < arr[i][x].length; y++) {
					boolean inv = this.random.nextBoolean();
					arr[i][x][y] = MathUtils.round(inv ? this.random.nextDouble() * min
							: this.random.nextDouble() * max);
				}
			}
		}
		
		return arr;
	}
	
	@Test
	public void testSizeAndAngle_001() {
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
	
	@Test
	public void testDeltaMatrix_001() throws DCTCoefficientOutOfBoundsException {
		double[][][] deltas = generateRandom3DArray(8, -127, 127);
		
		byte[][] data = ProtocolBase.getDeltaMatrixBytes(deltas, 8);
		byte[] stream = new byte[data[0].length + data[1].length + data[2].length];
		ArrayUtils.copyArray(data[0], 0, stream, 0, data[0].length);
		ArrayUtils.copyArray(data[1], 0, stream, data[0].length, data[1].length);
		ArrayUtils.copyArray(data[2], 0, stream, data[0].length + data[1].length, data[2].length);
		double[][][] decoded = ProtocolBase.getDeltaCoefficientsFromDatastream(stream, 0, 8);
		assertArrayEquals(deltas, decoded);
	}
	
	@Test
	public void testDeltaMatrix_002() throws DCTCoefficientOutOfBoundsException {
		int[] sizes = {4, 8, 16, 64, 128};
		
		for (final int size : sizes) {
			for (int i = 0; i < 1024; i++) {
				double[][][] deltas = generateRandom3DArray(size, -127, 127);
				
				byte[][] data = ProtocolBase.getDeltaMatrixBytes(deltas, size);
				byte[] stream = new byte[data[0].length + data[1].length + data[2].length];
				ArrayUtils.copyArray(data[0], 0, stream, 0, data[0].length);
				ArrayUtils.copyArray(data[1], 0, stream, data[0].length, data[1].length);
				ArrayUtils.copyArray(data[2], 0, stream, data[0].length + data[1].length, data[2].length);
				double[][][] decoded = ProtocolBase.getDeltaCoefficientsFromDatastream(stream, 0, size);
				assertArrayEquals(deltas, decoded);
			}
		}
	}
	
	@Test
	public void testBinarizeDeltaMatrix_001() {
		final int N = 16;
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		byte[] deltaBytes = MatrixOperations.generateRandomByteMatrix(N * N);
		BitWriter output = new BitWriter();
		BitWriter dec = new BitWriter();
		
		Protocol.binarizeResiduals(deltaBytes, ResidualType.RESIDUAL_Y, output, encoder, manager_enc, N);
		Protocol.debinarizeResiduals(new BitReader(output.toByteArray(), output.getTotalBits()), ResidualType.RESIDUAL_Y, dec, decoder, manager_dec, N);
		byte[] decoded = dec.toByteArray();
		
		assertEquals(deltaBytes.length, decoded.length);
		assertArrayEquals(deltaBytes, decoded);
	}
	
	@Test
	public void testBinarizeDeltaMatrix_002() {
		int[] sizes = {4, 8, 16, 64, 128};
		
		for (final int size : sizes) {
			for (int i = 0; i < 1024; i++) {
				CABAC encoder = new CABAC();
				CABAC decoder = new CABAC();
				
				ContextModelManager manager_enc = new ContextModelManager();
				ContextModelManager manager_dec = new ContextModelManager();
				
				byte[] deltaBytes = MatrixOperations.generateRandomByteMatrix(size * size);
				BitWriter output = new BitWriter();
				BitWriter dec = new BitWriter();
				
				Protocol.binarizeResiduals(deltaBytes, ResidualType.RESIDUAL_Y, output, encoder, manager_enc, size);
				Protocol.debinarizeResiduals(new BitReader(output.toByteArray(), output.getTotalBits()), ResidualType.RESIDUAL_Y, dec, decoder, manager_dec, size);
				byte[] decoded = dec.toByteArray();
				
				assertEquals(deltaBytes.length, decoded.length);
				assertArrayEquals(deltaBytes, decoded);
			}
		}
	}
}
