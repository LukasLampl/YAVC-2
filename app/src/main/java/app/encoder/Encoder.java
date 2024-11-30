/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package app.encoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import app.ArgumentProcessor;
import app.dct.DCTEngine;
import app.filter.Deblocker;
import app.interprediction.Vector;
import app.interprediction.VectorEngine;
import app.interprediction.VectorEngineResult;
import app.intraprediction.IntraEngine;
import app.io.ImagePreReader;
import app.io.OutputStream;
import app.io.QueueObject;
import app.quadtree.QuadtreeEngine;
import app.rendering.DifferenceEngine;
import app.rendering.RenderEngine;
import app.utils.LoadDistributor;
import app.utils.MacroBlock;
import app.utils.PixelRaster;
import app.utils.ReferenceFrameManager;

/**
 * The {@code Encoder} class is responsible for encoding given frames into
 * a YAVC portable format that can be read by the {@code Encoder}. This class
 * is a wrapper that is only responsible for calling the encoding methods.
 * 
 * @author Lukas Lampl
 * @since 1.1.0
 */
public class Encoder {
	/**
	 * The {@link app.dct.DCTEngine DCTEngine} used in the encoding process,
	 * with precalculated values and tables.
	 */
	public DCTEngine DCT_ENGINE = null;
	
	/**
	 * The {@link app.quadtree.QuadtreeEngine QuadtreeEngine} used for the whole
	 * encoding process.
	 */
	private static QuadtreeEngine QUADTREE_ENGINE = new QuadtreeEngine();
	
	/**
	 * The {@link app.rendering.DifferenceEngine DifferenceEngine} used for the whole
	 * encoding process.
	 */
	private static DifferenceEngine DIFFERENCE_ENGINE = new DifferenceEngine();
	
	/**
	 * The {@link app.interprediction.VectorEngine VectorEngine} used for the whole
	 * encoding process.
	 */
	private static VectorEngine VECTOR_ENGINE = new VectorEngine();
	
	private IntraEngine INTRA_ENGINE = new IntraEngine();
	
	/**
	 * The {@link app.utils.ReferenceFrameManager ReferenceFrameManager} used for
	 * managing all reference frames.
	 */
	private ReferenceFrameManager referenceManager = new ReferenceFrameManager();
	
	/**
	 * Creates a new Encoder object that is ready for encoding a list or folder
	 * of frames.
	 * 
	 * @param dctEngine	The DCTEngine to use in the encoding process.
	 */
	public Encoder(DCTEngine dctEngine) {
		this.DCT_ENGINE = dctEngine;
	}
	
	/**
	 * The main encode function that invokes the sub-tasks sequentially by
	 * first generating a Quadtree then calculating the differences, followed
	 * by interprediction and finally rendering and output.
	 */
	public void encode() {
		int files = ArgumentProcessor.inputFile.listFiles().length;
		OutputStream outStream = new OutputStream(ArgumentProcessor.outputFile);
		Deblocker deblocker = new Deblocker();
		ImagePreReader imgReader = new ImagePreReader(files, ArgumentProcessor.inputFile, referenceManager);
		
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
				
				if (frame == null) {
					System.out.println("Skip: " + i);
					continue;
				} else if (frame.notInvokedWithData == true) {
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
//				List<MacroBlock> differences = DIFFERENCE_ENGINE.computeDifferences(prevFrame, leaveNodeManager);
				long end_difference = System.currentTimeMillis();
//				BufferedImage[] part = RenderEngine.renderQuadtree(leaveNodeManager.getRawData(), curFrame.getDimension(), curFrame);
				
				INTRA_ENGINE.computeIntraPrediction(leaveNodeManager.getRawData(), curFrame);
			//	BufferedImage intraComposit = RenderEngine.renderIntraPrediction(intraBlocks, curFrame.getDimension());
			//	ImageIO.write(intraComposit, "png", new File(ArgumentProcessor.outputFile.getParent() + "/IP_" + i + ".png"));
				BufferedImage r = RenderEngine.renderDifferences(leaveNodeManager.getRawData(), curFrame.getDimension());
				ImageIO.write(r, "png", new File(ArgumentProcessor.outputFile.getParent() + "/INTRA_" + i + ".png"));
				
				long start_vector_movement = System.currentTimeMillis();
				VectorEngineResult vectorEngineResult = VECTOR_ENGINE.computeMovementVectors(leaveNodeManager.getRawData(), this.referenceManager);
				LoadDistributor<Vector> movementVectors = vectorEngineResult.getVectors();
				LoadDistributor<MacroBlock> differenceManager = vectorEngineResult.getRestBlocks();
				long end_vector_movement = System.currentTimeMillis();
				
//				BufferedImage vectors = RenderEngine.renderVectors(movementVectors, curFrame.getDimension());
				long start_render = System.currentTimeMillis();
				PixelRaster composite = RenderEngine.renderComposit(movementVectors, this.referenceManager, differenceManager, false);
				outStream.addObjectToOutputQueue(new QueueObject(movementVectors, differenceManager));
				long end_render = System.currentTimeMillis();
				
				long start_deblock = System.currentTimeMillis();
				deblocker.deblock(movementVectors, composite);
				long end_deblock = System.currentTimeMillis();
				
//				ImageIO.write(part[0], "png", new File(ArgumentProcessor.outputFile.getParent() + "/MB_" + i + ".png"));
//				ImageIO.write(part[1], "png", new File(ArgumentProcessor.outputFile.getParent() + "/MBA_" + i + ".png"));
//				ImageIO.write(part[2], "png", new File(ArgumentProcessor.outputFile.getParent() + "/MBAV_" + i + ".png"));
//				ImageIO.write(vectors, "png", new File(output.getParent() + "/V_" + i + ".png"));
//				ImageIO.write(composite.toBufferedImage(), "png", new File(ArgumentProcessor.outputFile.getParent() + "/VR_" + i + ".png"));
				
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
				
				leaveNodeManager.discard();
				movementVectors.discard();
				differenceManager.discard();
				
				this.referenceManager.add(composite.copy());
				prevFrame = composite;
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
			double averageMSE = (VECTOR_ENGINE.getVectorMSE() / vecs.getNumberOfObjects());
			TOTAL_MSE += averageMSE;
			TOTAL_MSE_ADDITION_COUNT++;
			System.out.println("- Vectors: " + vecs.getNumberOfObjects() + " | Covered area: " + vecs.getNumberOfData() + "px | Avg. MSE: " + averageMSE);
		}
		
		if (diffs != null) {
			System.out.println("- Non-Coded blocks: " + diffs.getNumberOfObjects() + " | Covered area: " + diffs.getNumberOfData() + "px");
		}
		
		System.out.println("- Total Avg. MSE of inter prediction: " + (TOTAL_MSE / TOTAL_MSE_ADDITION_COUNT));
		
		int usedMemory = (int)Runtime.getRuntime().totalMemory();
		int memory = usedMemory / 1000000;
		System.out.println("- Memory usage: " + memory + "MB");
		long endOutput = System.currentTimeMillis();
		System.out.println("- Total time used for writing statistics: " + (endOutput - startOutput) + "ms");
	}
}
