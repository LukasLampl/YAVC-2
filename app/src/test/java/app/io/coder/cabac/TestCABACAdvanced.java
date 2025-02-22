package app.io.coder.cabac;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import app.config;
import app.encoder.MatrixOperations;
import app.engines.prediction.interprediction.DecodingVector;
import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.intraprediction.DecodingIntraPredictionBlock;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.io.BitReader;
import app.io.BitWriter;
import app.io.Protocol;
import app.utils.MathUtils;

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
	@Disabled
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
		intraBlock.setHorizontal(MatrixOperations.generateRGBColor(8));
		intraBlock.setVertical(MatrixOperations.generateRGBColor(8));
		
		BitWriter bin = new BitWriter();
		Protocol.binarizeIntraPredictionBlock(intraBlock, encoder, manager_enc, bin);
		
		BitReader binStream = new BitReader(bin.toByteArray());
		DecodingIntraPredictionBlock decoded = Protocol.debinarizeIntraPredictionBlock(decoder, manager_dec, binStream, 8);
		
		assertEquals(intraBlock.getAngle(), decoded.getAngle());
		assertEquals(intraBlock.getHorizontal(), decoded.getHorizontal());
		assertEquals(intraBlock.getVertical(), decoded.getVertical());
	}
}
