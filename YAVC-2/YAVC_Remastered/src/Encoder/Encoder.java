package Encoder;

import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import Main.config;
import Utils.MacroBlock;
import Utils.PixelRaster;
import Utils.QueueObject;
import Utils.Vector;

public class Encoder {
	public static DCTEngine DCT_ENGINE = null;
	private static QuadtreeEngine QUADTREE_ENGINE = new QuadtreeEngine();
	private static DifferenceEngine DIFFERENCE_ENGINE = new DifferenceEngine();
	private static VectorEngine VECTOR_ENGINE = new VectorEngine();
	
	public void encode(File input, File output) {
		DCT_ENGINE = new DCTEngine();
		DCT_ENGINE.initDCTCoefficinets();
		OutputStream outStream = new OutputStream(new File(input.getParent()));
		ArrayList<PixelRaster> references = new ArrayList<PixelRaster>(config.MAX_REFERENCES);
		PixelRaster futFrame = null;
		PixelRaster curFrame = null;
		PixelRaster prevFrame = null;
		
		int files = input.listFiles().length;
		long sumOfMilliSeconds = 0;
		
		try {
			outStream.activate();
			
			for (int i = 0; i < files; i++) {
				long start = System.currentTimeMillis();
				File frameFile = getAwaitedFile(input, i, ".bmp");
				
				if (!frameFile.exists()) {
					System.out.println("Skip: " + i);
					continue;
				}
				
				if (prevFrame == null) {
					prevFrame = new PixelRaster(ImageIO.read(frameFile));
//					futureFrame = new PixelRaster(ImageIO.read(getAwaitedFile(input, i + 1, ".bmp")));
					outStream.writeStartFrame(output, prevFrame);
					references.add(prevFrame);
					continue;
				}
				
				curFrame = new PixelRaster(ImageIO.read(frameFile));
//				futureFrame = new PixelRaster(ImageIO.read(getAwaitedFile(input, i + 1, ".bmp")));
				
				DCT_ENGINE.applyDCTOnPixelRaster(curFrame);
				ArrayList<MacroBlock> quadtreeRoots = QUADTREE_ENGINE.constructQuadtree(curFrame);
				ArrayList<MacroBlock> leaveNodes = QUADTREE_ENGINE.getLeaveNodes(quadtreeRoots);
				
//				BufferedImage[] part = QUADTREE_ENGINE.drawMacroBlocks(leaveNodes, curFrame.getDimension());
//				leaveNodes = DIFFERENCE_ENGINE.computeDifferences(curFrame.getColorSpectrum(), prevFrame, leaveNodes);
				ArrayList<Vector> movementVectors = VECTOR_ENGINE.computeMovementVectors(leaveNodes, references, curFrame.getColorSpectrum(), futFrame);
				
//				BufferedImage vectors = VECTOR_ENGINE.drawVectors(movementVectors, curFrame.getDimension());
				PixelRaster composit = outStream.renderResult(movementVectors, references, leaveNodes, prevFrame);
				
//				ImageIO.write(part[0], "png", new File(output.getAbsolutePath() + "/MB_" + i + ".png"));
//				ImageIO.write(part[1], "png", new File(output.getAbsolutePath() + "/MBA_" + i + ".png"));
//				ImageIO.write(vectors, "png", new File(output.getAbsolutePath() + "/V_" + i + ".png"));
				ImageIO.write(composit.toBufferedImage(), "png", new File(output.getAbsolutePath() + "/VR_" + i + ".png"));
				
				outStream.addObjectToOutputQueue(new QueueObject(movementVectors, leaveNodes));
				
				long end = System.currentTimeMillis();
				long time = end - start;
				sumOfMilliSeconds += time;
				printStatistics(time, sumOfMilliSeconds, i, movementVectors, leaveNodes, curFrame.getColorSpectrum());

				prevFrame = composit.copy();
				references.add(prevFrame);
				manageReferences(references);
			}
			
			outStream.shutdown();
			references.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static double TOTAL_MSE = 0;
	private static int TOTAL_MSE_ADDITION_COUNT = 0;
	
	private void printStatistics(long time, long fullTime, int index, ArrayList<Vector> vecs, ArrayList<MacroBlock> diffs, int colsCount) {
		System.out.println("");
		System.out.println("Frame " + index + " [Colors: " + colsCount + "]" + ":");
		System.out.println("Time: " + time + "ms | Avg. time: " + (fullTime / index) + "ms");

		if (vecs != null) {
			int vecArea = 0;
			double averageMSE = (VECTOR_ENGINE.getVectorMSE() / vecs.size());
			TOTAL_MSE += averageMSE;
			TOTAL_MSE_ADDITION_COUNT++;
			for (Vector v : vecs) vecArea += v.getSize() * v.getSize();
			System.out.println("Vectors: " + vecs.size() + " | Covered area: " + vecArea + "px | Avg. MSE: " + averageMSE);
		}
		
		if (diffs != null) {
			int diffArea = 0;
			for (MacroBlock b : diffs) diffArea += b.getSize() * b.getSize();
			System.out.println("Non-Coded blocks: " + diffs.size() + " | Covered area: " + diffArea + "px");
		}
		
		System.out.println("Total Avg. MSE of inter prediction: " + (TOTAL_MSE / TOTAL_MSE_ADDITION_COUNT));
	}
	
	private void manageReferences(ArrayList<?> references) {
		if (references == null) {
			return;
		}
		
		if (references.size() < config.MAX_REFERENCES) {
			return;
		}
		
		references.remove(0);
	}
	
	private File getAwaitedFile(File parent, int index, String format) {
		StringBuilder name = new StringBuilder(32);
		name.append(parent.getAbsolutePath() + "/");
		
		if (index < 10) {
			name.append("000");
		} else if (index < 100) {
			name.append("00");
		} else if (index < 1000) {
			name.append("0");
		}
		
		name.append(index);
		name.append(format);
		return new File(name.toString());
	}
}
