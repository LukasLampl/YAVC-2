package app;

import java.util.ArrayList;

import app.encoder.Encoder;
import app.interprediction.Vector;
import app.utils.MacroBlock;

public class StatsWriter {
	private static double TOTAL_MSE = 0;
	private static int TOTAL_MSE_ADDITION_COUNT = 0;
	
	public static void write(long time, long fullTime, int index, ArrayList<Vector> vecs, ArrayList<MacroBlock> diffs) {
		System.out.println();
		System.out.println("Frame " + index + ":");
		System.out.println("- Time: " + time + "ms | Avg. time: " + (fullTime / index) + "ms");

		printVectorSize(vecs);
		printNonCodedBlockSize(diffs);
		System.out.println("- Total Avg. MSE of inter prediction: " + (TOTAL_MSE / TOTAL_MSE_ADDITION_COUNT));
		printMemoryUsage();
	}
	
	private static void printVectorSize(ArrayList<Vector> vecs) {
		if (vecs != null) {
			int vecArea = 0;
			double averageMSE = (Encoder.VECTOR_ENGINE.getVectorMSE() / vecs.size());
			TOTAL_MSE += averageMSE;
			TOTAL_MSE_ADDITION_COUNT++;
			
			for (Vector v : vecs) {
				vecArea += v.getAppendedBlock().getSquaredSize();
			}
			
			System.out.println("- Vectors: " + vecs.size() + " | Covered area: " + vecArea + "px | Avg. MSE: " + averageMSE);
			
			if (config.PRINT_EXACT_STATISTICS) {
				writeExactMacroBlockDistribution(vecs);
			}
		}
	}
	
	private static void printNonCodedBlockSize(ArrayList<MacroBlock> diffs) { 
		if (diffs != null) {
			int diffArea = 0;
			
			for (MacroBlock b : diffs) {
				diffArea += b.getSquaredSize();
			}
			
			System.out.println("- Non-Coded blocks: " + diffs.size() + " | Covered area: " + diffArea + "px");
		}
	}
	
	private static String getPercentage(int val1, int val2) {
		return String.format("%.02f%%", (((double)val1 / (double)val2) * 100));
	}
	
	private static void printMemoryUsage() {
		int usedMemory = (int)Runtime.getRuntime().totalMemory();
		int memory = usedMemory / 1000000;
		System.out.println("- Memory usage: " + memory + "MB");
	}
	
	private static void writeExactMacroBlockDistribution(ArrayList<Vector> vecs) {
		int[] areaDistribution = new int[6];
		
		for (Vector v : vecs) {
			switch (v.getSize()) {
			case 4:
				areaDistribution[0]++; break;
			case 8:
				areaDistribution[1]++; break;
			case 16:
				areaDistribution[2]++; break;
			case 32:
				areaDistribution[3]++; break;
			case 64:
				areaDistribution[4]++; break;
			case 128:
				areaDistribution[5]++; break;
			}
		}
		
		System.out.println("   > Coverage of blocks: 4x4 = " + getPercentage(areaDistribution[0], vecs.size()) +
								" | 8x8 = " + getPercentage(areaDistribution[1], vecs.size()) +
								" | 16x16 = " + getPercentage(areaDistribution[2], vecs.size()) +
								" | 32x32 = " + getPercentage(areaDistribution[3], vecs.size()) +
								" | 64x64 = " + getPercentage(areaDistribution[4], vecs.size()) +
								" | 128x128 = " + getPercentage(areaDistribution[5], vecs.size()));
	}
}
