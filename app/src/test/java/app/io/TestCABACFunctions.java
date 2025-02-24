package app.io;

import app.io.coder.cabac.BinaryContextModel;
import app.utils.MathUtils;

public abstract class TestCABACFunctions {
	public static void calculateEfficency(final byte[] arr, final int actualBitsUsed) {
		BitReader reader = new BitReader(arr);
		BinaryContextModel model = new BinaryContextModel();
		
		while (reader.hasRemainingBits()) {
			model.incrementSymbolFrequency(reader.read());
		}
		
		final double freq_0 = model.getSymbolFrequency(0x00);
		final double freq_1 = model.getSymbolFrequency(0x01);
		final double total = freq_0 + freq_1;
		final double p0 = freq_0 / total;
		final double p1 = freq_1 / total;
		
		final double H = -(p0 * (Math.log(p0) / Math.log(2))
				+ p1 * (Math.log(p1) / Math.log(2)));
		
		final double minimumRequiredBits = MathUtils.round(H * arr.length);
		
		System.out.println("Shannon entropy: H(x)=" + H);
		System.out.println("Minimum required bits: " + minimumRequiredBits);
		System.out.println(" > Actually used: " + actualBitsUsed);
		System.out.println("Efficency: " + ((minimumRequiredBits / actualBitsUsed) * 100) + "%");
	}
}
