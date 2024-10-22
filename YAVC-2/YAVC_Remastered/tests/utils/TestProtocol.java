package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestProtocol {
	@Test
	public void testPositionBytes() {
		int[] positions = {0, 4, 8, 12, 16, 28, 32, 75, 4096, 65000, 733};
		
		for (int pos : positions) {
			byte[] posBytes = Protocol.getPositionBytes(pos);
			int reversePos = Protocol.getPosition(posBytes[0], posBytes[1]);
			assertEquals(pos, reversePos);
		}
	}
}
