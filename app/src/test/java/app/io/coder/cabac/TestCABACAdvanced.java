package app.io.coder.cabac;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.config;
import app.encoder.MatrixOperations;
import app.encoder.MockQuadtreeEngine;
import app.engines.prediction.interprediction.DecodingVector;
import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.intraprediction.DecodingIntraPredictionBlock;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.engines.quadtree.QuadtreeBase;
import app.io.BitReader;
import app.io.BitWriter;
import app.io.Protocol;
import app.rendering.ColorManager;
import app.utils.MathUtils;
import app.utils.components.Component2D;
import app.utils.components.MacroBlock;

public class TestCABACAdvanced {
	@Test
	public void test_vectorConversion_001() {
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
		MacroBlock block = new MacroBlock(43, 41, 8, true);
		
		EncodingVector vector = new EncodingVector(43, 31, 8);
		vector.setReference(4);
		vector.setYUVDelta(MatrixOperations.generateRandom3DMatrix(8, 255));
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeVector(vector, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingVector decoded = Protocol.debinarizeVector(decoder, manager_dec, binStream, block);
		
		assertEquals(vector.getSpanX(), decoded.getSpanX());
		assertEquals(vector.getSpanY(), decoded.getSpanY());
		assertEquals(vector.getReference(), decoded.getReference());
	}
	
	@Test
	public void test_vectorConversion_002() {
		final int steps = 0xFFFF;
		final int ten_percent = steps / 10;
		
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();

		for (int i = 0; i < steps; i++) {
			if (i % ten_percent == 0) {
				System.out.println("Vector conversion test progress: " + (i * 100 / steps) + "%");
			}
			
			manager_enc.resetModels();
			manager_dec.resetModels();
			encoder.reset();
			decoder.reset();
			
			final int spanX = MathUtils.round(Math.random() * 127 * (Math.random() < 0.5 ? -1 : 1));
			final int spanY = MathUtils.round(Math.random() * 127 * (Math.random() < 0.5 ? -1 : 1));
			final int size = 8;
			final int reference = MathUtils.round(Math.random() * config.MAX_REFERENCES);
			
			MacroBlock block = new MacroBlock(0, 0, size, true);
			
			EncodingVector vector = new EncodingVector(spanX, spanY, size);
			vector.setReference(reference);
			vector.setYUVDelta(MatrixOperations.generateRandom3DMatrix(size, 127));
			
			BitWriter bin = new BitWriter();
			Protocol.binarizeVector(vector, encoder, manager_enc, bin);
			
			BitReader binStream = new BitReader(bin.toByteArray());
			DecodingVector decoded = Protocol.debinarizeVector(decoder, manager_dec, binStream, block);
			
			assertEquals(vector.getSpanX(), decoded.getSpanX());
			assertEquals(vector.getSpanY(), decoded.getSpanY());
			assertEquals(vector.getReference(), decoded.getReference());
		}
	}
	
	@Test
	public void test_vectorConversion_003() {
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
		MacroBlock block = new MacroBlock(0, 0, 8, true);
		
		EncodingVector vector_1 = new EncodingVector(43, 31, 8);
		vector_1.setReference(4);
		vector_1.setYUVDelta(MatrixOperations.generateRandom3DMatrix(8, 255));
		
		EncodingVector vector_2 = new EncodingVector(1, 17, 8);
		vector_2.setReference(2);
		vector_2.setYUVDelta(MatrixOperations.generateRandom3DMatrix(8, 255));
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeVector(vector_1, encoder, manager_enc, bin);
		Protocol.binarizeVector(vector_2, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingVector decoded_1 = Protocol.debinarizeVector(decoder, manager_dec, binStream, block);
		DecodingVector decoded_2 = Protocol.debinarizeVector(decoder, manager_dec, binStream, block);
		
		assertEquals(vector_1.getSpanX(), decoded_1.getSpanX());
		assertEquals(vector_1.getSpanY(), decoded_1.getSpanY());
		assertEquals(vector_1.getReference(), decoded_1.getReference());
		
		assertEquals(vector_2.getSpanX(), decoded_2.getSpanX());
		assertEquals(vector_2.getSpanY(), decoded_2.getSpanY());
		assertEquals(vector_2.getReference(), decoded_2.getReference());
	}
	
	@Test
	public void test_vectorConversion_004() {
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
		MacroBlock block = new MacroBlock(4, 16, 4, true);
		
		EncodingVector vector = new EncodingVector(4, 16, 4);
		vector.setReference(3);
		vector.setYUVDelta(MatrixOperations.generateRandom3DMatrix(4, 255));
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeVector(vector, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingVector decoded = Protocol.debinarizeVector(decoder, manager_dec, binStream, block);
		
		assertEquals(vector.getSpanX(), decoded.getSpanX());
		assertEquals(vector.getSpanY(), decoded.getSpanY());
		assertEquals(vector.getReference(), decoded.getReference());
	}
	
	@Test
	public void test_intraConversion_001() {
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
		MacroBlock block = new MacroBlock(43, 31, 8, true);
		
		EncodingIntraPredictionBlock intraBlock = new EncodingIntraPredictionBlock(43, 31, 30, 8);
		intraBlock.setYUVDelta(MatrixOperations.generateRandom3DMatrix(8, 255));
		intraBlock.setHorizontal(new double[][] {
			ColorManager.convertRGBToYUV(Color.RED),
			ColorManager.convertRGBToYUV(Color.BLUE),
			ColorManager.convertRGBToYUV(Color.YELLOW),
			ColorManager.convertRGBToYUV(Color.GREEN),
			ColorManager.convertRGBToYUV(Color.MAGENTA),
			ColorManager.convertRGBToYUV(Color.ORANGE),
			ColorManager.convertRGBToYUV(Color.WHITE),
			ColorManager.convertRGBToYUV(Color.BLACK)
		});
		
		intraBlock.setVertical(new double[][] {
			ColorManager.convertRGBToYUV(Color.BLACK),
			ColorManager.convertRGBToYUV(Color.RED),
			ColorManager.convertRGBToYUV(Color.ORANGE),
			ColorManager.convertRGBToYUV(Color.WHITE),
			ColorManager.convertRGBToYUV(Color.MAGENTA),
			ColorManager.convertRGBToYUV(Color.GREEN),
			ColorManager.convertRGBToYUV(Color.BLUE),
			ColorManager.convertRGBToYUV(Color.YELLOW)
		});
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeIntraPredictionBlock(intraBlock, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingIntraPredictionBlock decoded = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, block);
		
		assertEquals(intraBlock.getAngle(), decoded.getAngle());
		assertArrayEquals(intraBlock.getHorizontal(), decoded.getHorizontal());
		assertArrayEquals(intraBlock.getVertical(), decoded.getVertical());
	}
	
	@Test
	public void test_intraConversion_002() {
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
		MacroBlock block = new MacroBlock(43, 31, 8, true);
		
		EncodingIntraPredictionBlock intraBlock = new EncodingIntraPredictionBlock(43, 31, 30, 8);
		intraBlock.setYUVDelta(MatrixOperations.generateRandom3DMatrix(8, 255));
		intraBlock.setHorizontal(MatrixOperations.generateColorBased2DMatrix(8));
		intraBlock.setVertical(MatrixOperations.generateColorBased2DMatrix(8));
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeIntraPredictionBlock(intraBlock, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingIntraPredictionBlock decoded = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, block);
		
		assertEquals(intraBlock.getAngle(), decoded.getAngle());
		assertArrayEquals(intraBlock.getHorizontal(), decoded.getHorizontal());
		assertArrayEquals(intraBlock.getVertical(), decoded.getVertical());
	}
	
	@Test
	public void test_intraConversion_003() {
		final int steps = 0xFFFF;
		final int ten_percent = steps / 10;
		
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
		MacroBlock block = new MacroBlock(43, 31, 8, true);
		
		for (int i = 0; i < steps; i++) {
			if (i % ten_percent == 0) {
				System.out.println("Intra conversion test progress: " + (i * 100 / steps) + "%");
			}
			
			manager_enc.resetModels();
			manager_dec.resetModels();
			encoder.reset();
			decoder.reset();
			
			EncodingIntraPredictionBlock intraBlock = new EncodingIntraPredictionBlock(43, 31, 30, 8);
			intraBlock.setYUVDelta(MatrixOperations.generateRandom3DMatrix(8, 255));
			intraBlock.setHorizontal(MatrixOperations.generateColorBased2DMatrix(8));
			intraBlock.setVertical(MatrixOperations.generateColorBased2DMatrix(8));
			
			BitWriter bin = new BitWriter();
			Protocol.binarizeIntraPredictionBlock(intraBlock, encoder, manager_enc, bin);
			
			BitReader binStream = new BitReader(bin.toByteArray());
			DecodingIntraPredictionBlock decoded = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, block);
			
			assertEquals(intraBlock.getAngle(), decoded.getAngle());
			assertArrayEquals(intraBlock.getHorizontal(), decoded.getHorizontal());
			assertArrayEquals(intraBlock.getVertical(), decoded.getVertical());
		}
	}
	
	@Test
	public void test_intraConversion_004() {
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
		MacroBlock block = new MacroBlock(16, 4, 4, true);
		
		EncodingIntraPredictionBlock intraBlock = new EncodingIntraPredictionBlock(16, 4, 30, 4);
		intraBlock.setYUVDelta(MatrixOperations.generateRandom3DMatrix(4, 255));
		intraBlock.setHorizontal(MatrixOperations.generateColorBased2DMatrix(4));
		intraBlock.setVertical(MatrixOperations.generateColorBased2DMatrix(4));
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeIntraPredictionBlock(intraBlock, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingIntraPredictionBlock decoded = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, block);
		
		assertEquals(intraBlock.getAngle(), decoded.getAngle());
		assertArrayEquals(intraBlock.getHorizontal(), decoded.getHorizontal());
		assertArrayEquals(intraBlock.getVertical(), decoded.getVertical());
	}
	
	@Test
	public void test_intraConversion_005() {
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
		MacroBlock block = new MacroBlock(16, 4, 4, true);
		
		EncodingIntraPredictionBlock intraBlock_1 = new EncodingIntraPredictionBlock(16, 4, 30, 4);
		intraBlock_1.setYUVDelta(MatrixOperations.generateRandom3DMatrix(4, 255));
		intraBlock_1.setHorizontal(MatrixOperations.generateColorBased2DMatrix(4));
		intraBlock_1.setVertical(MatrixOperations.generateColorBased2DMatrix(4));
		
		EncodingIntraPredictionBlock intraBlock_2 = new EncodingIntraPredictionBlock(0, 28, 5, 4);
		intraBlock_2.setYUVDelta(MatrixOperations.generateRandom3DMatrix(4, 255));
		intraBlock_2.setHorizontal(MatrixOperations.generateColorBased2DMatrix(4));
		intraBlock_2.setVertical(MatrixOperations.generateColorBased2DMatrix(4));
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeIntraPredictionBlock(intraBlock_1, encoder, manager_enc, bin);
		Protocol.binarizeIntraPredictionBlock(intraBlock_2, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingIntraPredictionBlock decoded_1 = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, block);
		DecodingIntraPredictionBlock decoded_2 = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, block);
		
		assertEquals(intraBlock_1.getAngle(), decoded_1.getAngle());
		assertArrayEquals(intraBlock_1.getHorizontal(), decoded_1.getHorizontal());
		assertArrayEquals(intraBlock_1.getVertical(), decoded_1.getVertical());
		
		assertEquals(intraBlock_2.getAngle(), decoded_2.getAngle());
		assertArrayEquals(intraBlock_2.getHorizontal(), decoded_2.getHorizontal());
		assertArrayEquals(intraBlock_2.getVertical(), decoded_2.getVertical());
	}
	
	@Test
	public void testQuadtreeConversion() throws IOException {
		Dimension bounds = new Dimension(1920, 1080);
		final int numOfRoots = (1920 * 1080) / (QuadtreeBase.MAX_SIZE * QuadtreeBase.MAX_SIZE) + 1;
		
		List<MacroBlock> roots = MockQuadtreeEngine.generateQuadtrees(1, bounds, true);
		MockQuadtreeEngine.assignLinks(roots);
		BitWriter data = Protocol.binarizeQuadtrees(roots);
		
		System.out.println(numOfRoots + " quadtrees stored in " + data.getTotalBits() + " Bits or " + (data.getTotalBits() / Byte.SIZE) + " Bytes.");
		Files.write(Path.of("C:\\Users\\Lukas Lampl\\Documents\\EncoderOut\\cabac.bin"), data.toByteArray());
		
		BitReader input = new BitReader(data.toByteArray(), data.getTotalBits());
		List<MacroBlock> decoded = Protocol.debinarizeQuadtrees(input, bounds);
		
		assertEquals(roots.size(), decoded.size());
		
		for (int i = 0; i < roots.size(); i++) {
			assertSingleQuadtree(roots.get(i), decoded.get(i));
		}
		
//		Files.write(Path.of("C:\\Users\\Lukas Lampl\\Documents\\EncoderOut\\cabac.bin"), data.toByteArray());
	}
	
	private void assertSingleQuadtree(final MacroBlock expected, final MacroBlock got) {
		assertEquals(expected.isSubdivided(), got.isSubdivided());
		assertEquals(expected.getPositionX(), got.getPositionX());
		assertEquals(expected.getPositionY(), got.getPositionY());
		assertEquals(expected.getSize(), got.getSize());
		
		if (expected.isSubdivided()) {
			assertEquals(expected.getNodes().length, got.getNodes().length);
			
			for (int i = 0; i < expected.getNodes().length; i++) {
				if (expected.getNodes()[i] == null) {
					continue;
				}
				
				assertSingleQuadtree(expected.getNodes()[i], got.getNodes()[i]);
			}
		} else {
			assertLink(expected, got);
		}
	}
	
	private void assertLink(final MacroBlock expected, final MacroBlock got) {
		final Component2D exp_link = expected.getLink();
		final Component2D got_link = got.getLink();
		
		if (exp_link instanceof EncodingVector) {
			assertEquals(got_link.getClass(), DecodingVector.class);
			
			EncodingVector v_enc = (EncodingVector)exp_link;
			DecodingVector v_dec = (DecodingVector)got_link;
			
			assertEquals(v_enc.getSpanX(), v_dec.getSpanX());
			assertEquals(v_enc.getSpanY(), v_dec.getSpanY());
			assertEquals(v_enc.getReference(), v_dec.getReference());
		} else if (exp_link instanceof EncodingIntraPredictionBlock) {
			assertEquals(got_link.getClass(), DecodingIntraPredictionBlock.class);
			
			EncodingIntraPredictionBlock intra_enc = (EncodingIntraPredictionBlock)exp_link;
			DecodingIntraPredictionBlock intra_dec = (DecodingIntraPredictionBlock)got_link;
			
			assertEquals(intra_enc.getAngle(), intra_dec.getAngle());
			assertArrayEquals(intra_enc.getHorizontal(), intra_dec.getHorizontal());
			assertArrayEquals(intra_enc.getVertical(), intra_dec.getVertical());
		} else {
			throw new IllegalStateException("No such type: " + exp_link.getClass() + "!");
		}
		
		assertEquals(exp_link.getPositionX(), got_link.getPositionX());
		assertEquals(exp_link.getPositionY(), got_link.getPositionY());
		assertEquals(exp_link.getSize(), got_link.getSize());
	}
}
