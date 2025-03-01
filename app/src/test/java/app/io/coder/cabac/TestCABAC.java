package app.io.coder.cabac;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.encoder.MatrixOperations;
import app.io.BitReader;
import app.io.BitWriter;
import app.io.TestCABACFunctions;
import app.io.coder.cabac.ContextModelManager.CodingType;
import app.utils.MathUtils;

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
		
		byte[] data = new byte[] {1};
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
	public void testGeneral005() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = new byte[] {3};
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
	public void testGeneral006() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = new byte[] {7};
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
	public void testGeneral007() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = new byte[] {12};
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
	public void testGeneral008() {
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
	public void testGeneral009() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = MatrixOperations.generateRandomByteMatrix(8192);
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
	public void testGeneral010() {
		final int steps = 2048;
		final int ten_percent = steps / 10;
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		for (int i = 0; i < steps; i++) {
			if (i % ten_percent == 0) {
				System.out.println("CABAC test progress: " + (i * 100 / steps) + "%");
			}
			
			model_enc.reset();
			model_dec.reset();
			encoder.reset();
			decoder.reset();
			
			byte[] data = MatrixOperations.generateRandomByteMatrix(MathUtils.round(Math.random() * 8192));
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
	}
	
	@Test
	public void testGeneral011() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = new byte[] {127, -127};
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
	public void testMultiModal001() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = {0, 34, 65, 68, 0, -24, 56, 84, -45, 0, 0, 56};
		byte[] data_p2 = {78, 98, 0, 0, -1, -2, 56, -94, -125, 120, 30};
		
		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);
		
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
	}
	
	@Test
	public void testMultiModal002() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = {42, -103, 67, 18, -56, 89, -18, 101, -72, 11};
		byte[] data_p2 = {34, -21, 88, -45, 12, -60, 101, -3, 76, -90};
		
		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);
		
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
	}
	
	@Test
	public void testMultiModal003() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = {119, -103, -56, -78, 95, -127, -103, -88, -73, 116, -117, -16};
		byte[] data_p2 = {-79, 69, -51, -125, 65, -51, 100, 46, 98, -46, 110, -115, -107, -78, 10, -43};
		
		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);
		
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
	}
	
	@Test
	public void testMultiModal004() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = MatrixOperations.generateRandomByteMatrix(12);
		byte[] data_p2 = MatrixOperations.generateRandomByteMatrix(16);

		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);

		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
	}
	
	@Test
	public void testMultiModal005() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = {56, -104, 19, 72, -33, 85, -67, 42, -9, 120};
		byte[] data_p2 = {63, -48, 110, -21, 7, -92, 29, -118, 55, -30};
		byte[] data_p3 = {84, -56, 36, -112, 14, -78, 67, -4, 93, -25};
		
		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);
		BitReader dataReader_3 = new BitReader(data_p3);
		
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));
		encoder.encode(dataReader_3, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_Y));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		BitWriter dec_3 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		decoder.decode(data_p3.length * Byte.SIZE, binIn, dec_3, manager_dec.getModel(CodingType.VECTOR_SPAN_Y));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		byte[] decoded_p3 = dec_3.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertEquals(data_p3.length, decoded_p3.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
		assertArrayEquals(data_p3, decoded_p3);
	}
	
	@Test
	public void testMultiModal006() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = {127};
		byte[] data_p2 = {-127};
		
		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);
		
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
	}
	
	@Test
	public void testMultiModal007() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = {38};
		byte[] data_p2 = {21};
		
		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);
		
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
	}
	
	@Test
	public void testMultiModal008() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = {0};
		byte[] data_p2 = {0};
		
		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);
		
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
	}
	
	@Test
	public void testMultiModal009() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = {78};
		byte[] data_p2 = {93};
		
		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);
		
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
	}
	
	@Test
	public void testMultiModal010() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
			
		byte[] data_p1 = MatrixOperations.generateRandomByteMatrix(0xFF);
		byte[] data_p2 = MatrixOperations.generateRandomByteMatrix(0x40);
		
		BitReader dataReader_1 = new BitReader(data_p1);
		BitReader dataReader_2 = new BitReader(data_p2);
		
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader_1, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));
		encoder.encode(dataReader_2, bin, manager_enc.getModel(CodingType.VECTOR_SPAN_X));

		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(data_p1.length * Byte.SIZE, binIn, dec_1, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		decoder.decode(data_p2.length * Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.VECTOR_SPAN_X));
		
		byte[] decoded_p1 = dec_1.toByteArray();
		byte[] decoded_p2 = dec_2.toByteArray();
		
		assertEquals(data_p1.length, decoded_p1.length);
		assertEquals(data_p2.length, decoded_p2.length);
		assertArrayEquals(data_p1, decoded_p1);
		assertArrayEquals(data_p2, decoded_p2);
	}
	
	@Test
	public void testCompRatio001() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = MatrixOperations.generateSteadyByteMatrix(0xFFF, (byte)0x00);
		BitReader dataReader = new BitReader(data);
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader, bin, model_enc);
		
		System.out.println("Original size: " + (data.length * Byte.SIZE) + " Bits");
		System.out.println("> " + data.length + " Bytes");
		System.out.println("Encoded size: " + bin.getTotalBits() + " Bits");
		System.out.println("> " + (bin.getTotalBits() / Byte.SIZE) + " Bytes");
		TestCABACFunctions.calculateEfficency(data, bin.getTotalBits());
		
		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec = new BitWriter();
		
		decoder.decode(data.length * Byte.SIZE, binIn, dec, model_dec);
		byte[] decoded = dec.toByteArray();
		
		assertEquals(data.length, decoded.length);
		assertArrayEquals(data, decoded);
	}
	
	@Test
	public void testCompRatio002() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		System.out.println("Generating bytes");
		byte[] data = MatrixOperations.generateIncreasingByteMatrix(0xFFF, 0x00, 0xFF);
		System.out.println("Generated bytes");
		System.out.println("Starting encoding");
		BitReader dataReader = new BitReader(data);
		BitWriter bin = new BitWriter();
		
		encoder.encode(dataReader, bin, model_enc);
		
		System.out.println("Original size: " + (data.length * Byte.SIZE) + " Bits");
		System.out.println("> " + data.length + " Bytes");
		System.out.println("Encoded size: " + bin.getTotalBits() + " Bits");
		System.out.println("> " + (bin.getTotalBits() / Byte.SIZE) + " Bytes");
		TestCABACFunctions.calculateEfficency(data, bin.getTotalBits());
		
		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec = new BitWriter();
		
		decoder.decode(data.length * Byte.SIZE, binIn, dec, model_dec);
		byte[] decoded = dec.toByteArray();
		
		assertEquals(data.length, decoded.length);
		assertArrayEquals(data, decoded);
	}
	
	@Test
	public void testBitProcessing001() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte bit = 0x01;
		BitWriter bin = new BitWriter();
		encoder.encode(bit, bin, model_enc);
		
		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec = new BitWriter();
		
		decoder.decode(1, binIn, dec, model_dec);
		BitReader reader = new BitReader(dec.toByteArray());
		
		assertEquals(bit, reader.read());
	}
	
	@Test
	public void testBitProcessing002() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte bit = 0x00;
		BitWriter bin = new BitWriter();
		encoder.encode(bit, bin, model_enc);
		
		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec = new BitWriter();
		
		decoder.decode(1, binIn, dec, model_dec);
		byte[] decoded = dec.toByteArray();
		
		assertEquals(bit, decoded[0]);
	}
	
	@Test
	public void testBitProcessing003() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte bit_1 = 0x00;
		byte bit_2 = 0x01;
		BitWriter bin = new BitWriter();
		encoder.encode(bit_1, bin, model_enc);
		encoder.encode(bit_2, bin, model_enc);
		
		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec = new BitWriter();
		
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		BitReader reader = new BitReader(dec.toByteArray(), dec.getTotalBits());
		
		byte dec_1 = reader.read();
		byte dec_2 = reader.read();
		
		assertEquals(bit_1, dec_1);
		assertEquals(bit_2, dec_2);	
	}
	
	@Test
	public void testBitProcessing004() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte bit_1 = 0x00;
		byte bit_2 = 0x00;
		byte bit_3 = 0x01;
		byte bit_4 = 0x01;
		byte bit_5 = 0x01;
		byte bit_6 = 0x01;
		byte bit_7 = 0x01;
		byte bit_8 = 0x00;
		byte bit_9 = 0x00;
		byte bit_10 = 0x01;
		byte bit_11 = 0x00;
		
		BitWriter bin = new BitWriter();
		encoder.encode(bit_1, bin, model_enc);
		encoder.encode(bit_2, bin, model_enc);
		encoder.encode(bit_3, bin, model_enc);
		encoder.encode(bit_4, bin, model_enc);
		encoder.encode(bit_5, bin, model_enc);
		encoder.encode(bit_6, bin, model_enc);
		encoder.encode(bit_7, bin, model_enc);
		encoder.encode(bit_8, bin, model_enc);
		encoder.encode(bit_9, bin, model_enc);
		encoder.encode(bit_10, bin, model_enc);
		encoder.encode(bit_11, bin, model_enc);
		
		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec = new BitWriter();
		
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		decoder.decode(1, binIn, dec, model_dec);
		BitReader reader = new BitReader(dec.toByteArray(), dec.getTotalBits());
		
		assertEquals(bit_1, reader.read());
		assertEquals(bit_2, reader.read());
		assertEquals(bit_3, reader.read());
		assertEquals(bit_4, reader.read());
		assertEquals(bit_5, reader.read());
		assertEquals(bit_6, reader.read());
		assertEquals(bit_7, reader.read());
		assertEquals(bit_8, reader.read());
		assertEquals(bit_9, reader.read());
		assertEquals(bit_10, reader.read());
		assertEquals(bit_11, reader.read());
	}
	
	@Test
	public void testBitProcessing005() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte bit_1 = 0x00;
		byte[] data = {0x59};
		
		BitWriter bin = new BitWriter();
		encoder.encode(bit_1, bin, manager_enc.getModel(CodingType.INTRA_BORDER_HORIZONTAL));
		encoder.encode(new BitReader(data), bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		
		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		
		decoder.decode(1, binIn, dec_1, manager_dec.getModel(CodingType.INTRA_BORDER_HORIZONTAL));
		decoder.decode(Byte.SIZE, binIn, dec_2, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		BitReader reader_1 = new BitReader(dec_1.toByteArray(), dec_1.getTotalBits());
		
		assertEquals(bit_1, reader_1.read());
		assertEquals(data[0], dec_2.toByteArray()[0]);
	}
	
	@Test
	public void testBitProcessing006() {
		ContextModelManager manager_enc = new ContextModelManager();
		ContextModelManager manager_dec = new ContextModelManager();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte bit_1 = 0x00;
		byte bit_2 = 0x01;
		byte[] data = {0x59};
		
		BitWriter bin = new BitWriter();
		encoder.encode(bit_1, bin, manager_enc.getModel(CodingType.INTRA_BORDER_HORIZONTAL));
		encoder.encode(bit_2, bin, manager_enc.getModel(CodingType.INTRA_BORDER_HORIZONTAL));
		encoder.encode(new BitReader(data), bin, manager_enc.getModel(CodingType.PREDICTION_TYPE));
		
		BitReader binIn = new BitReader(bin.toByteArray(), bin.getTotalBits());
		BitWriter dec_1 = new BitWriter();
		BitWriter dec_2 = new BitWriter();
		BitWriter dec_3 = new BitWriter();
		
		decoder.decode(1, binIn, dec_1, manager_dec.getModel(CodingType.INTRA_BORDER_HORIZONTAL));
		decoder.decode(1, binIn, dec_2, manager_dec.getModel(CodingType.INTRA_BORDER_HORIZONTAL));
		decoder.decode(Byte.SIZE, binIn, dec_3, manager_dec.getModel(CodingType.PREDICTION_TYPE));
		BitReader reader_1 = new BitReader(dec_1.toByteArray(), dec_1.getTotalBits());
		BitReader reader_2 = new BitReader(dec_2.toByteArray(), dec_2.getTotalBits());
		
		assertEquals(bit_1, reader_1.read());
		assertEquals(bit_2, reader_2.read());
		assertEquals(data[0], dec_3.toByteArray()[0]);
	}
	
	@Test
	public void testLongCoding() {
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		
		byte[] data = MatrixOperations.generateRandomByteMatrix(0x7FFFFF);
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
	public void testThroughput() {
		final int steps = 16;
		final int dataLen = 0x7FFFFF;
		BinaryContextModel model_enc = new BinaryContextModel();
		BinaryContextModel model_dec = new BinaryContextModel();
		
		CABAC encoder = new CABAC();
		CABAC decoder = new CABAC();
		long encodingTime = 0;
		long decodingTime = 0;
		
		for (int i = 0; i < steps; i++) {
			encoder.reset();
			decoder.reset();
			model_enc.reset();
			model_dec.reset();
			
			System.out.println("CABAC Troughput test step: " + i + "/" + steps);
			byte[] data = MatrixOperations.generateRandomByteMatrix(dataLen);
			BitReader dataReader = new BitReader(data);
			BitWriter bin = new BitWriter();
			
			long start_enc = System.nanoTime();
			encoder.encode(dataReader, bin, model_enc);
			long end_enc = System.nanoTime();
			
			BitReader binIn = new BitReader(bin.toByteArray());
			BitWriter dec = new BitWriter();
			
			long start_dec = System.nanoTime();
			decoder.decode(data.length * Byte.SIZE, binIn, dec, model_dec);
			long end_dec = System.nanoTime();
			
			encodingTime += end_enc - start_enc;
			decodingTime += end_dec - start_dec;
			byte[] decoded = dec.toByteArray();
			
			assertEquals(data.length, decoded.length);
			assertArrayEquals(data, decoded);
		}
		
		encodingTime /= 1000000;
		decodingTime /= 1000000;
		
		System.out.println("\n=========================================================");
		System.out.println("THROUGHPUT TEST FOR " + (dataLen * steps) + " BYTES");
		System.out.println("=========================================================");
		System.out.println("Total time:");
		System.out.println(" > Encoding: " + encodingTime + " ms");
		System.out.println(" > Decoding: " + decodingTime + " ms" );
		System.out.println("Average time:");
		System.out.println(" > Encoding: " + (encodingTime / steps) + " ms");
		System.out.println(" > Decoding: " + (decodingTime / steps) + " ms" );
		System.out.println("Throughput:");
		System.out.println(" > Encoding: " + ((encodingTime * 1000000) / (steps * dataLen) + " ns/b"));
		System.out.println(" > Decoding: " + ((decodingTime * 1000000) / (steps * dataLen) + " ns/b"));
		System.out.println();
	}
}
