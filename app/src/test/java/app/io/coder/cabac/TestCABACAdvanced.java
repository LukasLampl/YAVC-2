package app.io.coder.cabac;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.encoder.MatrixOperations;
import app.engines.prediction.interprediction.DecodingVector;
import app.engines.prediction.interprediction.EncodingVector;
import app.io.BitReader;
import app.io.BitWriter;
import app.io.Protocol;

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
}
