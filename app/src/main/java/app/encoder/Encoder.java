/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2025  Lukas Nian En Lampl, Hans Lampl

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

import java.util.List;

import app.ArgumentProcessor;
import app.engines.dct.DCTEngine;
import app.engines.prediction.PredictionDistributor;
import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.interprediction.encoding.VectorEngine;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.engines.prediction.intraprediction.IntraEngine;
import app.engines.prediction.intraprediction.IntraPredictionBlock;
import app.engines.quadtree.QuadtreeEngine;
import app.filter.Deblocker;
import app.io.ImagePreReader;
import app.io.OutputStream;
import app.io.QueueObject;
import app.managers.LoadDistributor;
import app.managers.ReferenceFrameManager;
import app.rendering.DifferenceEngine;
import app.rendering.RenderEngine;
import app.utils.Mode;
import app.utils.PixelRaster;
import app.utils.components.MacroBlock;

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
	 * The {@link app.engines.dct.DCTEngine DCTEngine} used in the encoding process,
	 * with precalculated values and tables.
	 */
	public DCTEngine DCT_ENGINE = null;
	
	/**
	 * The {@link app.engines.quadtree.QuadtreeEngine QuadtreeEngine} used for the whole
	 * encoding process.
	 */
	private static QuadtreeEngine QUADTREE_ENGINE = new QuadtreeEngine();
	
	/**
	 * The {@link app.rendering.DifferenceEngine DifferenceEngine} used for the whole
	 * encoding process.
	 */
	private static DifferenceEngine DIFFERENCE_ENGINE = new DifferenceEngine();
	
	/**
	 * The {@link app.engines.prediction.interprediction.encoding.VectorEngine VectorEngine} used for the whole
	 * encoding process.
	 */
	private static VectorEngine VECTOR_ENGINE = new VectorEngine();
	
	private IntraEngine INTRA_ENGINE = new IntraEngine();

	private PredictionDistributor predictionDistributor = new PredictionDistributor();
	
	/**
	 * The {@link app.managers.ReferenceFrameManager ReferenceFrameManager} used for
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
				List<MacroBlock> quadtreeRoots = QuadtreeEngine.constructQuadtree(curFrame);
				long end_construct_quadtree = System.currentTimeMillis();
				long start_get_leave_nodes = System.currentTimeMillis();
				LoadDistributor<MacroBlock> leaveNodeManager = QuadtreeEngine.getLeaveNodes(quadtreeRoots);
				leaveNodeManager.compute(frame.getWidth() * frame.getHeight());
				long end_get_leave_nodes = System.currentTimeMillis();
				
//				BufferedImage[] part = RenderEngine.renderQuadtree(leaveNodes, curFrame.getDimension());
				long start_difference = System.currentTimeMillis();
//				List<MacroBlock> differences = DIFFERENCE_ENGINE.computeDifferences(prevFrame, leaveNodeManager);
				long end_difference = System.currentTimeMillis();
//				BufferedImage[] part = RenderEngine.renderQuadtree(leaveNodeManager.getRawData(), curFrame.getDimension(), curFrame);

				PredictionDistributor.Result predictionTypes = this.predictionDistributor.estimateBlockPredictionType(leaveNodeManager, curFrame.getDimension());
				
				long start_intra = System.currentTimeMillis();
				LoadDistributor<EncodingIntraPredictionBlock> intraPredictedBlocks = INTRA_ENGINE.computeIntraPrediction(predictionTypes.getIntraPredictables(), curFrame);
				long end_intra = System.currentTimeMillis();
//				BufferedImage[] d = RenderEngine.renderIntraPredictionDeltas(intraPredictedBlocks, curFrame.getDimension());
//				ImageIO.write(d[0], "png", new File(ArgumentProcessor.outputFile.getParent() + "/INTRA_DELTAS_" + i + ".png"));
//				ImageIO.write(d[1], "png", new File(ArgumentProcessor.outputFile.getParent() + "/INTRA_RECONSTRUCTED_" + i + ".png"));
//				
//				BufferedImage devIntra = RenderEngine.renderDeviation(predictionTypes.getIntraPredictables(), true, curFrame.getDimension());
//				BufferedImage devInter = RenderEngine.renderDeviation(predictionTypes.getInterPredictables(), false, curFrame.getDimension());
//				ImageIO.write(devIntra, "png", new File(ArgumentProcessor.outputFile.getParent() + "/DEV_INTRA_" + i + ".png"));
//				ImageIO.write(devInter, "png", new File(ArgumentProcessor.outputFile.getParent() + "/DEV_INTER_" + i + ".png"));

				long start_vector_movement = System.currentTimeMillis();
				LoadDistributor<EncodingVector> movementVectors = VECTOR_ENGINE.computeMovementVectors(predictionTypes.getInterPredictables(), this.referenceManager);
				long end_vector_movement = System.currentTimeMillis();
				
//				BufferedImage distribution = RenderEngine.renderPredictionDistribution(intraPredictedBlocks.getRawData(), movementVectors.getRawData(), curFrame.getDimension());
//				ImageIO.write(distribution, "png", new File(ArgumentProcessor.outputFile.getParent() + "/PRED_DIST_" + i + ".png"));
				
//				BufferedImage vectors = RenderEngine.renderVectors(movementVectors.getRawData(), curFrame.getDimension());
				long start_render = System.currentTimeMillis();
				PixelRaster composite = RenderEngine.renderComposit(movementVectors, this.referenceManager, intraPredictedBlocks, Mode.ENCODE);
				
				outStream.addObjectToOutputQueue(new QueueObject(movementVectors, intraPredictedBlocks));
				long end_render = System.currentTimeMillis();
				
				long start_deblock = System.currentTimeMillis();
				deblocker.deblock(movementVectors, composite);
				long end_deblock = System.currentTimeMillis();
				
//				ImageIO.write(composite.toBufferedImage(), "png", new File(ArgumentProcessor.outputFile.getParent() + "/VR_" + i + ".png"));
//				ImageIO.write(part[0], "png", new File(ArgumentProcessor.outputFile.getParent() + "/MB_" + i + ".png"));
//				ImageIO.write(part[1], "png", new File(ArgumentProcessor.outputFile.getParent() + "/MBA_" + i + ".png"));
//				ImageIO.write(part[2], "png", new File(ArgumentProcessor.outputFile.getParent() + "/MBAV_" + i + ".png"));
//				ImageIO.write(vectors, "png", new File(ArgumentProcessor.outputFile.getParent() + "/V_" + i + ".png"));
				
				long end = System.currentTimeMillis();
				long time = end - start;
				long imgReadTime = (end_img_read - start_img_read);
				long quadtreeConstructionTime = (end_construct_quadtree - start_construct_quadtree);
				long leaveNodesTime = (end_get_leave_nodes - start_get_leave_nodes);
				long differenceTime = (end_difference - start_difference);
				long vectorTime = (end_vector_movement - start_vector_movement);
				long renderTime = (end_render - start_render);
				long deblockTime = (end_deblock - start_deblock);
				long intraTime = (end_intra - start_intra);
				sumOfMilliSeconds += time;
				printStatistics(time, sumOfMilliSeconds, i, movementVectors, intraPredictedBlocks, imgReadTime, quadtreeConstructionTime, leaveNodesTime, differenceTime, intraTime, vectorTime, renderTime, deblockTime);
				
				leaveNodeManager.discard();
//				movementVectors.discard();
//				differenceManager.discard();
				
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
	
	private void printStatistics(long time, long fullTime, int index, LoadDistributor<? extends Vector> vecs, LoadDistributor<? extends IntraPredictionBlock> intra,
			long imgReadTime, long quadtreeConstructionTime, long leaveNodeTime, long differenceTime, long intraTime, long vectorTime, long renderTime, long deblockTime) {
		long startOutput = System.currentTimeMillis();
//		System.out.println("");
//		System.out.println("Frame " + index + ":");
		System.out.println("- Time: " + time + "ms | Avg. time: " + (fullTime / index) + "ms");
		System.out.println("   > Image read time: " + imgReadTime + "ms");
		System.out.println("   > Quadtree construction time: " + quadtreeConstructionTime + "ms");
		System.out.println("   > Leave node time: " + leaveNodeTime + "ms");
		System.out.println("   > Difference analysis time: " + differenceTime + "ms");
		System.out.println("   > Vector calculation time: " + vectorTime + "ms");
		System.out.println("   > Intraprediction time: " + intraTime + "ms");
		System.out.println("   > Rendering time of frame (with coding errors): " + renderTime + "ms");
		System.out.println("   > Deblocking filter time: " + deblockTime + "ms");
		System.out.println("   > Sum (process only, no output): " + (quadtreeConstructionTime + leaveNodeTime + differenceTime + vectorTime + renderTime + deblockTime) + "ms");
		
		if (vecs != null) {
			double averageMSE = (VECTOR_ENGINE.getVectorMSE() / vecs.getNumberOfObjects());
			TOTAL_MSE += averageMSE;
			TOTAL_MSE_ADDITION_COUNT++;
			System.out.println("- Vectors: " + vecs.getNumberOfObjects() + " | Covered area: " + vecs.getNumberOfData() + "px | Avg. MSE: " + averageMSE);
		}
		
		if (intra != null) {
			System.out.println("- Intra-Coded blocks: " + intra.getNumberOfObjects() + " | Covered area: " + intra.getNumberOfData() + "px");
		}
		
		System.out.println("- Total Avg. MSE of inter prediction: " + (TOTAL_MSE / TOTAL_MSE_ADDITION_COUNT));
		
		int usedMemory = (int)Runtime.getRuntime().totalMemory();
		int memory = usedMemory / 1000000;
		System.out.println("- Memory usage: " + memory + "MB");
		long endOutput = System.currentTimeMillis();
		System.out.println("- Total time used for writing statistics: " + (endOutput - startOutput) + "ms");
	}
}
