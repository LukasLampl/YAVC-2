package app.encoder;

import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import app.interprediction.Vector;
import app.interprediction.VectorEngine;
import app.interprediction.VectorEngineResult;
import app.quadtree.QuadtreeEngine;
import app.utils.Deblocker;
import app.utils.MacroBlock;
import app.utils.PixelRaster;
import app.utils.QueueObject;
import app.utils.ReferenceFrameManager;
import app.utils.RenderEngine;

public class Encoder {
	public DCTEngine DCT_ENGINE = null;
	private static QuadtreeEngine QUADTREE_ENGINE = new QuadtreeEngine();
	private static DifferenceEngine DIFFERENCE_ENGINE = new DifferenceEngine();
	private static VectorEngine VECTOR_ENGINE = new VectorEngine();
	private ReferenceFrameManager referenceManager = new ReferenceFrameManager();
	
	public Encoder(DCTEngine dctEngine) {
		this.DCT_ENGINE = dctEngine;
	}
	
	public void encode(File input, File output) {
		int files = input.listFiles().length;
		OutputStream outStream = new OutputStream(output);
		Deblocker deblocker = new Deblocker();
		ImagePreReader imgReader = new ImagePreReader(files, input);
		
		PixelRaster curFrame = null;
		PixelRaster prevFrame = null;
		
		long sumOfMilliSeconds = 0;
		long startOfTime = System.currentTimeMillis();
		
		try {
			outStream.activate();
			
			for (int i = 0; i < files; i++) {
				System.out.println("");
				System.out.println("Frame " + i + ":");
				long start = System.currentTimeMillis();
				long start_img_read = System.currentTimeMillis();
				PixelRaster frame = imgReader.getNextImage();
				long end_img_read = System.currentTimeMillis();
				
				if (frame.invokedWithData == false) {
					System.out.println("Skip: " + i);
					continue;
				}
				
				if (prevFrame == null) {
					prevFrame = frame;
					outStream.writeMetadata(prevFrame.getDimension(), files - 1);
					outStream.writeStartFrame(prevFrame);
					this.referenceManager.add(prevFrame);
					continue;
				}
			
				curFrame = frame;
				
				long start_construct_quadtree = System.currentTimeMillis();
				ArrayList<MacroBlock> quadtreeRoots = QUADTREE_ENGINE.constructQuadtree(curFrame);
				long end_construct_quadtree = System.currentTimeMillis();
				long start_get_leave_nodes = System.currentTimeMillis();
				LoadDistributor<MacroBlock> leaveNodeManager = QUADTREE_ENGINE.getLeaveNodes(quadtreeRoots);
				leaveNodeManager.compute(frame.getWidth() * frame.getHeight());
				long end_get_leave_nodes = System.currentTimeMillis();
				
//				BufferedImage[] part = RenderEngine.renderQuadtree(leaveNodes, curFrame.getDimension());
				long start_difference = System.currentTimeMillis();
				LoadDistributor<MacroBlock> differenceManager = DIFFERENCE_ENGINE.computeDifferences(prevFrame, leaveNodeManager);
				long end_difference = System.currentTimeMillis();
//				BufferedImage[] part = RenderEngine.renderQuadtree(leaveNodes, curFrame.getDimension());
				
				long start_vector_movement = System.currentTimeMillis();
				VectorEngineResult vectorEngineResult = VECTOR_ENGINE.computeMovementVectors(differenceManager, this.referenceManager);
				LoadDistributor<Vector> movementVectors = vectorEngineResult.getVectors();
				differenceManager = vectorEngineResult.getRestBlocks();
				long end_vector_movement = System.currentTimeMillis();
				
//				BufferedImage vectors = RenderEngine.renderVectors(movementVectors, curFrame.getDimension());
				long start_render = System.currentTimeMillis();
				PixelRaster composite = RenderEngine.renderResult(movementVectors, this.referenceManager, differenceManager, false);
				outStream.addObjectToOutputQueue(new QueueObject(movementVectors, differenceManager));
				long end_render = System.currentTimeMillis();
				
				long start_deblock = System.currentTimeMillis();
				deblocker.deblock(movementVectors, composite);
				long end_deblock = System.currentTimeMillis();
				
//				ImageIO.write(part[0], "png", new File(output.getParent() + "/MB_" + i + ".png"));
//				ImageIO.write(part[1], "png", new File(output.getParent() + "/MBA_" + i + ".png"));
//				ImageIO.write(vectors, "png", new File(output.getParent() + "/V_" + i + ".png"));
//				ImageIO.write(composite.toBufferedImage(), "png", new File(output.getParent() + "/VR_" + i + ".png"));
				
				long end = System.currentTimeMillis();
				long time = end - start;
				long imgReadTime = (end_img_read - start_img_read);
				long quadtreeConstructionTime = (end_construct_quadtree - start_construct_quadtree);
				long leaveNodesTime = (end_get_leave_nodes - start_get_leave_nodes);
				long differenceTime = (end_difference - start_difference);
				long vectorTime = (end_vector_movement - start_vector_movement);
				long renderTime = (end_render - start_render);
				long deblockTime = (end_deblock - start_deblock);
				sumOfMilliSeconds += time;
				printStatistics(time, sumOfMilliSeconds, i, movementVectors, differenceManager, imgReadTime, quadtreeConstructionTime, leaveNodesTime, differenceTime, vectorTime, renderTime, deblockTime);
				
				this.referenceManager.add(composite.copy());
				prevFrame = composite.copy();
			}
			
			long endOfTime = System.currentTimeMillis();
			System.out.println("Time used: " + (endOfTime - startOfTime) + "ms");
			
			outStream.finishQueue();
		} catch (Exception e) {
			outStream.shutdown();
			e.printStackTrace();
		}
	}
	
	private static double TOTAL_MSE = 0;
	private static int TOTAL_MSE_ADDITION_COUNT = 0;
	
	private void printStatistics(long time, long fullTime, int index, LoadDistributor<Vector> vecs, LoadDistributor<MacroBlock> diffs,
			long imgReadTime, long quadtreeConstructionTime, long leaveNodeTime, long differenceTime, long vectorTime, long renderTime, long deblockTime) {
		long startOutput = System.currentTimeMillis();
//		System.out.println("");
//		System.out.println("Frame " + index + ":");
		System.out.println("- Time: " + time + "ms | Avg. time: " + (fullTime / index) + "ms");
		System.out.println("   > Image read time: " + imgReadTime + "ms");
		System.out.println("   > Quadtree construction time: " + quadtreeConstructionTime + "ms");
		System.out.println("   > Leave node time: " + leaveNodeTime + "ms");
		System.out.println("   > Difference analysis time: " + differenceTime + "ms");
		System.out.println("   > Vector calculation time: " + vectorTime + "ms");
		System.out.println("   > Rendering time of frame (with coding errors): " + renderTime + "ms");
		System.out.println("   > Deblocking filter time: " + deblockTime + "ms");
		System.out.println("   > Sum (process only, no output): " + (quadtreeConstructionTime + leaveNodeTime + differenceTime + vectorTime + renderTime + deblockTime) + "ms");
		
		if (vecs != null) {
			int vecArea = 0;
			double averageMSE = (VECTOR_ENGINE.getVectorMSE() / vecs.getNumberOfObjects());
			TOTAL_MSE += averageMSE;
			TOTAL_MSE_ADDITION_COUNT++;
			
			for (Vector v : vecs.getRawData()) {
				vecArea += v.getAppendedBlock().getSquaredSize();
			}
			
			System.out.println("- Vectors: " + vecs.getNumberOfObjects() + " | Covered area: " + vecArea + "px | Avg. MSE: " + averageMSE);
		}
		
		if (diffs != null) {
			int diffArea = 0;
			
			for (MacroBlock b : diffs.getRawData()) {
				diffArea += b.getSquaredSize();
			}
			
			System.out.println("- Non-Coded blocks: " + diffs.getNumberOfObjects() + " | Covered area: " + diffArea + "px");
		}
		
		System.out.println("- Total Avg. MSE of inter prediction: " + (TOTAL_MSE / TOTAL_MSE_ADDITION_COUNT));
		
		int usedMemory = (int)Runtime.getRuntime().totalMemory();
		int memory = usedMemory / 1000000;
		System.out.println("- Memory usage: " + memory + "MB");
		long endOutput = System.currentTimeMillis();
		System.out.println("- Total time used for writing statistics: " + (endOutput - startOutput) + "ms");
	}
}
