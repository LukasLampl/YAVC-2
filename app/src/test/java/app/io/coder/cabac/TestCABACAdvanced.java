package app.io.coder.cabac;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
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
import app.utils.components.MacroBlock;

public class TestCABACAdvanced {
	@Test
	public void test_vectorConversion_001() {
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
		EncodingVector vector = new EncodingVector(43, 31, 8);
		vector.setReference(4);
		vector.setYUVDelta(MatrixOperations.generateRandom3DMatrix(8, 255));
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeVector(vector, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingVector decoded = Protocol.debinarizeVector(decoder, manager_dec, binStream, 8);
		
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
			
			EncodingVector vector = new EncodingVector(spanX, spanY, size);
			vector.setReference(reference);
			vector.setYUVDelta(MatrixOperations.generateRandom3DMatrix(size, 127));
			
			BitWriter bin = new BitWriter();
			Protocol.binarizeVector(vector, encoder, manager_enc, bin);
			
			BitReader binStream = new BitReader(bin.toByteArray());
			DecodingVector decoded = Protocol.debinarizeVector(decoder, manager_dec, binStream, size);
			
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
		DecodingVector decoded_1 = Protocol.debinarizeVector(decoder, manager_dec, binStream, 8);
		DecodingVector decoded_2 = Protocol.debinarizeVector(decoder, manager_dec, binStream, 8);
		
		assertEquals(vector_1.getSpanX(), decoded_1.getSpanX());
		assertEquals(vector_1.getSpanY(), decoded_1.getSpanY());
		assertEquals(vector_1.getReference(), decoded_1.getReference());
		
		assertEquals(vector_2.getSpanX(), decoded_2.getSpanX());
		assertEquals(vector_2.getSpanY(), decoded_2.getSpanY());
		assertEquals(vector_2.getReference(), decoded_2.getReference());
	}
	
	@Test
	public void test_intraConversion_001() {
		ContextModelManager manager_enc = new ContextModelManager();
		CABAC encoder = new CABAC();
		
		ContextModelManager manager_dec = new ContextModelManager();
		CABAC decoder = new CABAC();
		
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
		DecodingIntraPredictionBlock decoded = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, 8);
		
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
		
		EncodingIntraPredictionBlock intraBlock = new EncodingIntraPredictionBlock(43, 31, 30, 8);
		intraBlock.setYUVDelta(MatrixOperations.generateRandom3DMatrix(8, 255));
		intraBlock.setHorizontal(MatrixOperations.generateColorBased2DMatrix(8));
		intraBlock.setVertical(MatrixOperations.generateColorBased2DMatrix(8));
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeIntraPredictionBlock(intraBlock, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingIntraPredictionBlock decoded = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, 8);
		
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
			DecodingIntraPredictionBlock decoded = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, 8);
			
			assertEquals(intraBlock.getAngle(), decoded.getAngle());
			assertArrayEquals(intraBlock.getHorizontal(), decoded.getHorizontal());
			assertArrayEquals(intraBlock.getVertical(), decoded.getVertical());
		}
	}
	
	@Test
	public void testQuadtreeConversion() throws IOException {
		Dimension bounds = new Dimension(QuadtreeBase.MAX_SIZE, QuadtreeBase.MAX_SIZE);
		List<MacroBlock> roots = MockQuadtreeEngine.generateQuadtrees(1, bounds, false);
		MockQuadtreeEngine.assignLinks(roots);
		BitWriter data = Protocol.binarizeQuadtrees(roots);
		BitReader input = new BitReader(data.toByteArray(), data.getTotalBits());
		List<MacroBlock> decoded = Protocol.debinarizeQuadtrees(input, bounds);
		
		
//		System.out.println("Final len: " + data.getTotalBits() + " Bits");
//		System.out.println(" - " + (data.getTotalBits() / Byte.SIZE) + " Byte");
//		System.out.println(" - Around " + ((((double)data.getTotalBits() / (double)Byte.SIZE) / 24000) * 100) + "% of 24kB");
	}
}
