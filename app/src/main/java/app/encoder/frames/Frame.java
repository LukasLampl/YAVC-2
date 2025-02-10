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

import java.util.List;

import app.engines.quadtree.QuadtreeEngine;
import app.io.containers.QueueObject;
import app.managers.LoadDistributor;
import app.rendering.DifferenceEngine;
import app.utils.PixelRaster;
import app.utils.components.MacroBlock;

/**
 * This class functions as the base of all frames that will be processed
 * in the YAVC compressor. Essentially this is an encapsulation of processes
 * to process a raw frame into a YAVC representative like a vector or intra
 * prediction block.
 * 
 * @author Lukas Lampl
 * @since 1.1.0 [optimized_prototype_2]
 */
public abstract class Frame {
	/**
	 * Holds the current raw frame.
	 */
	protected final PixelRaster currentFrame;
	
	/**
	 * Holds the previous raw frame.
	 */
	protected final PixelRaster previousFrame;
	
	/**
	 * Holds the compositing frame after the coding has finished.
	 */
	protected PixelRaster composit = null;
	
	/**
	 * The queue object that will be written.
	 */
	protected QueueObject queueObject = null;
	
	/**
	 * Creates a new Frame with the given previous frame and current frame.
	 * 
	 * @param previousFrame	The previous raw frame.
	 * @param currentFrame	The current raw frame.
	 */
	public Frame(final PixelRaster previousFrame, final PixelRaster currentFrame) {
		this.currentFrame = currentFrame;
		this.previousFrame = previousFrame;
	}
	
	/**
	 * Computes the quadtree of the current frame. And returns the leaves / nodes in form
	 * of MacroBlocks.
	 * 
	 * @return The nodes of the Quadtree.
	 */
	protected LoadDistributor<MacroBlock> computeQuadtree() {
		List<MacroBlock> quadtreeRoots = QuadtreeEngine.constructQuadtree(this.currentFrame);
		LoadDistributor<MacroBlock> leaveNodeManager = QuadtreeEngine.getLeaveNodes(quadtreeRoots);
		leaveNodeManager.compute(this.currentFrame.getWidth() * this.currentFrame.getHeight());
		return leaveNodeManager;
	}
	
	/**
	 * Computes the differences from the current frame to the previous frame based
	 * on block error estimation (If a block has to much error compared to the
	 * previous frame it'll be recognized as a difference).
	 * 
	 * @param macroblocks	The macroblocks to compare.
	 * @return A LoadDistributor with the differences to the previous frame.
	 */
	protected LoadDistributor<MacroBlock> computeDifferences(final LoadDistributor<MacroBlock> macroblocks) {
		return DifferenceEngine.computeDifferences(this.previousFrame, macroblocks);
	}
	
	/**
	 * Computes the coding elements of the current raw frame and
	 * sets the {@link #composit} and {@link #queueObject} accordingly.
	 */
	public abstract void compute();
	
	/**
	 * Returns the composit of the frame. The composit is a composition of all
	 * coding elements and will be equal to the decoded frame / reconstructed frame.
	 * 
	 * @return The composit frame out of all coding elements.
	 */
	public abstract PixelRaster getComposit();
	
	/**
	 * Gets the QueueObject with all information to write to the YAVC file to
	 * reconstruct the frame.
	 * 
	 * @return The QueueObject of the current frame which contains information
	 * that should be written to the YAVC file.
	 */
	public abstract QueueObject getProcessedQueueObject();
}
