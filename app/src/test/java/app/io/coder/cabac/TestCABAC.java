package app.io.coder.cabac;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import app.encoder.MatrixOperations;
import app.engines.prediction.interprediction.DecodingVector;
import app.engines.prediction.interprediction.EncodingVector;
import app.io.BitReader;
import app.io.BitWriter;
import app.io.Protocol;

public class TestCABAC {
	@Test
	public void testGeneral001() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
		BitReader dataReader = new BitReader(data);
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader, bin, model_enc);
		
		BitReader binIn = new BitReader(bin.toByteArray());
		BitWriter dec = new BitWriter();
		decoder.decode(data.length * Byte.SIZE, binIn, dec, model_dec);
		byte[] decoded = dec.toByteArray();
		
		assertEquals(data.length, decoded.length);
		assertArrayEquals(data, decoded);
	}
	
	@Test
	public void testGeneral002() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = new byte[] {
			0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
			0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
			0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
			0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
			0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
			0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
			0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F,
			0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F
		};
		
		BitReader dataReader = new BitReader(data);
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader, bin, model_enc);
		
		BitReader binIn = new BitReader(bin.toByteArray());
		BitWriter dec = new BitWriter();
		decoder.decode(data.length * Byte.SIZE, binIn, dec, model_dec);
		byte[] decoded = dec.toByteArray();
		
		assertEquals(data.length, decoded.length);
		assertArrayEquals(data, decoded);
	}
	
	@Test
	public void testGeneral003() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = new byte[] {3, -5, 37, 98, -92, -12, 127, -127, 32, -56, 89, 44};
		BitReader dataReader = new BitReader(data);
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader, bin, model_enc);
		
		BitReader binIn = new BitReader(bin.toByteArray());
		BitWriter dec = new BitWriter();
		decoder.decode(data.length * Byte.SIZE, binIn, dec, model_dec);
		byte[] decoded = dec.toByteArray();
		
		assertEquals(data.length, decoded.length);
		assertArrayEquals(data, decoded);
	}
	
	@Test
	public void testGeneral004() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = MatrixOperations.generateRandomByteMatrix(15);
		BitReader dataReader = new BitReader(data);
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader, bin, model_enc);
		
		BitReader binIn = new BitReader(bin.toByteArray());
		BitWriter dec = new BitWriter();
		decoder.decode(data.length * Byte.SIZE, binIn, dec, model_dec);
		byte[] decoded = dec.toByteArray();
		
		assertEquals(data.length, decoded.length);
		assertArrayEquals(data, decoded);
	}
	
	@Test
	@Disabled
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
