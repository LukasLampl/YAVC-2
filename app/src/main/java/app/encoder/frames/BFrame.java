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

package app.encoder.frames;

import app.encoder.Encoder;
import app.engines.prediction.PredictionDistributor;
import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.io.containers.QueueObject;
import app.managers.LoadDistributor;
import app.managers.ReferenceFrameManager;
import app.rendering.RenderEngine;
import app.utils.Mode;
import app.utils.PixelRaster;
import app.utils.components.MacroBlock;

/**
 * For doc see {@link app.encoder.frames.Frame Frame}.
 * 
 * The "B" stands for "Bidirectional" which means this frame will predict using
 * intra prediction as well as inter prediction.
 * 
 * @author Lukas Lampl
 * @since 1.1.0 [optimized_prototype_2]
 */
public class BFrame extends Frame {
	/**
	 * Holds the reference frame manager with all previous frames.
	 */
	private final ReferenceFrameManager referenceFrameManager;
	
	/**
	 * Creates a new Frame with the given previous frame and current frame.
	 * 
	 * @param previousFrame			The previous raw frame.
	 * @param currentFrame			The current raw frame.
	 * @param referenceFrameManager	The reference frame manager which contains all previous frames.
	 */
	public BFrame(final PixelRaster previousFrame, final PixelRaster currentFrame,
			final ReferenceFrameManager referenceFrameManager) {
		super(previousFrame, currentFrame);
		this.referenceFrameManager = referenceFrameManager;
	}

	@Override
	public void compute() {
		LoadDistributor<MacroBlock> macroblocks = super.computeQuadtree();
		LoadDistributor<MacroBlock> differences = super.computeDifferences(macroblocks);
		PredictionDistributor.Result predictionTypes = PredictionDistributor.estimateBlockPredictionType(
								differences, this.currentFrame.getDimension());
		
		LoadDistributor<EncodingIntraPredictionBlock> intraPredictedBlocks =
				Encoder.INTRA_ENGINE.computeIntraPrediction(predictionTypes.getIntraPredictables(), this.currentFrame);
		LoadDistributor<EncodingVector> movementVectors =
				Encoder.VECTOR_ENGINE.computeMovementVectors(predictionTypes.getInterPredictables(), this.referenceFrameManager);
		
		super.composit = RenderEngine.renderComposit(movementVectors, this.referenceFrameManager, intraPredictedBlocks, Mode.ENCODE);
		super.queueObject = new QueueObject(movementVectors, intraPredictedBlocks);
		
		macroblocks.discard();
		differences.discard();
	}
	
	@Override
	public PixelRaster getComposit() {
		return super.composit;
	}
	
	@Override
	public QueueObject getProcessedQueueObject() {
		return super.queueObject;
	}
}
