package app.io.coder.cabac;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.config;
import app.encoder.MatrixOperations;
import app.engines.prediction.interprediction.DecodingVector;
import app.engines.prediction.interprediction.EncodingVector;
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
}
