package app.encoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestDCTEngine {
	@Test
	public void testStep() {
		class MockDCTEngine extends DCTEngine {
			private int[] x = {0, 0, 0, 0, 12, 23, 4, 6, 6, 3, 8, 1, 42, 67, 8, 0};
			private int[] m = {2, 4, 32, 16, 64, 64, 64, 2, 4, 128, 4, 4, 8, 16, 32, 32};
			
			public void run() {
				for (int i = 0; i < x.length; i++) {
					double expected = x[i] == 0 ? 1.0 / Math.sqrt(m[i]) : Math.sqrt(2.0 / (double)m[i]);
					assertEquals(super.step(x[i], m[i]), expected, 0.001);
				}
			}
		}
		
		MockDCTEngine e = new MockDCTEngine();
		e.run();
	}
}
