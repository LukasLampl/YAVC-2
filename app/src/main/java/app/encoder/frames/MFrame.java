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
 * The "M" stands for "Marker" which means this frame will only predict
 * using intra prediction. These frames should only be used as a marker
 * to ensure that jumping around in a video is possible. This frame is
 * free of references.
 * 
 * @author Lukas Lampl
 * @since 1.1.0 [optimized_prototype_2]
 */
public class MFrame extends Frame {
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
	public MFrame(final PixelRaster previousFrame, final PixelRaster currentFrame,
			final ReferenceFrameManager referenceFrameManager) {
		super(previousFrame, currentFrame);
		this.referenceFrameManager = referenceFrameManager;
	}

	@Override
	public void compute() {
		LoadDistributor<MacroBlock> macroblocks = super.computeQuadtree();

		LoadDistributor<EncodingIntraPredictionBlock> intraPredictedBlocks =
				Encoder.INTRA_ENGINE.computeIntraPrediction(macroblocks.getRawData(), this.currentFrame);
		
		super.composit = RenderEngine.renderComposit(null, this.referenceFrameManager, intraPredictedBlocks, Mode.ENCODE);
		this.queueObject = new QueueObject(null, intraPredictedBlocks);
		
		macroblocks.discard();
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
