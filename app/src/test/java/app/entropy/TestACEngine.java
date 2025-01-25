package app.entropy;

import org.junit.jupiter.api.Test;

public class TestACEngine {
			
		@Test
		public void test() {
			byte [] byteArray = {0x7f, 0x79, 0x05, 0x21, 0x48, 0x61, 0x44, 0x01};
			double d = ACEngine.encode(byteArray);
			System.out.println("Double d: " + d);
			byte [] decoded = ACEngine.decode(d, byteArray.length);
			for (byte b : decoded) {
				System.out.println(String.format("%02x", b));
			}
			
			System.out.println(Integer.toBinaryString(byteArray[0]));
			System.out.println(Integer.toBinaryString(decoded[0]));
		}
}
