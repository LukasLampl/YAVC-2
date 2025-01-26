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

package app.rendering;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.intraprediction.IntraPredictionBlock;
import app.managers.LoadDistributor;
import app.managers.ReferenceFrameManager;
import app.rendering.renderers.CompositRenderer;
import app.rendering.renderers.IntrapredictionRenderer;
import app.rendering.renderers.PredictionDistributionRenderer;
import app.rendering.renderers.QuadtreeRenderer;
import app.rendering.renderers.VectorRenderer;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

/**
 * The {@code RenderEngine} is one of the main parts and is responsible
 * for the whole rendering pipeline of the YAVC program. The {@code RenderEngine}
 * provides basic utilities for rendering intermediate YAVC steps, like the
 * differences, the Quadtree, the Vectors and the composit image.
 * 
 * @author Lukas Lampl
 * @since 1.1
 */
public class RenderEngine {
	/**
	 * Renders a composited image of all vectors, non-coded blocks and reference
	 * frames used. It runs asynchronously to achieve a higher throughput.
	 * 
	 * @param vecs					The vector to use for the composit.
	 * @param refs					The reference frames used by the vectors.
	 * @param differenceManager		All non-coded MacroBlocks.
	 * @param allowModToAbsDiff		Flag for whether modifications can be made to the vectors absolute color difference or not.
	 * @return A composit PixelRaster with all vectors, non-coded blocks and reference used.
	 */
	public static PixelRaster renderComposit(LoadDistributor<Vector> vecs, ReferenceFrameManager refs, LoadDistributor<IntraPredictionBlock> intraBlocks,
			boolean allowModToAbsDiff) {
		return CompositRenderer.renderComposit(vecs, refs, intraBlocks, allowModToAbsDiff);
	}
	
	
	/**
	 * Renders an image of the quadtree with the given leave nodes.
	 * 
	 * @param leaveNodes	The leave nodes of the quadtree.
	 * @param dim			Dimension of the frame.
	 * @return An array of BufferedImages, where at:
	 * <ul>
	 * <li>[0] - The quadtree boxes can be seen.
	 * <li>[1] - The mean color of each MacroBlock can be seen.
	 * <li>[2] - The fused image.
	 * </ul>
	 */
	public static BufferedImage[] renderQuadtree(List<MacroBlock> leaveNodes, Dimension dim, PixelRaster curFrame) {
		return QuadtreeRenderer.renderQuadtree(leaveNodes, dim, curFrame);
	}
	
	/**
	 * <p>This function provides a good debugging base. It draws all vectors in the
	 * ArrayList to an image and returns it.</p>
	 * 
	 * <p>For better visualization the vectors have different colors:
	 * <br><br><table border="1">
	 * <tr>
	 * <td>Reference</td> <td>Assigned color</td>
	 * </tr><tr>
	 * <td>0</td> <td>Color.Orange</td>
	 * </tr><tr>
	 * <td>1</td> <td>Color.Yellow</td>
	 * </tr><tr>
	 * <td>2</td> <td>Color.Blue</td>
	 * </tr><tr>
	 * <td>3</td> <td>Color.Red</td>
	 * </tr>
	 * </table>
	 * </p>
	 * 
	 * @return Image with all vectors drawn on it
	 * 
	 * @param vecs	Vectors to draw
	 * @param dim	Dimension of the frame
	 * 
	 * @see app.interprediction.T
	 * @see java.awt.Color
	 */
	public static BufferedImage renderVectors(List<Vector> vecs, Dimension dim) {
		return VectorRenderer.renderVectors(vecs, dim);
	}
	
	public static BufferedImage renderPredictionDistribution(List<IntraPredictionBlock> intraBlocks,
			List<Vector> movementVectors, Dimension dim) {
		return PredictionDistributionRenderer.renderPredictionDistribution(intraBlocks, movementVectors, dim);
	}
	
	public static BufferedImage[] renderIntraPredictionDeltas(LoadDistributor<IntraPredictionBlock> intraPredictedBlocks,
			Dimension dim) {
		return IntrapredictionRenderer.renderIntraPredictionDeltas(intraPredictedBlocks, dim);
	}
	
	public static BufferedImage renderIntraPrediction(List<MacroBlock> intraBlocks, Dimension dim) {
		return IntrapredictionRenderer.renderIntraPrediction(intraBlocks, dim);
	}
}
