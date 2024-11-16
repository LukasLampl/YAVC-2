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

package app.filter;

import java.awt.Point;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.ArgumentProcessor;
import app.config;
import app.interprediction.Vector;
import app.utils.LoadDistributor;
import app.utils.MathUtils;
import app.utils.PixelRaster;

/**
 * <p>The class {@code Deblocker} goes into the
 * category of filters for YAVC. The deblocking filter reduces
 * the block edges from the inter-prediction step by smoothing
 * the pixels out that are at the affected MacroBlock</p>
 * 
 * @author Lukas Lampl
 * @since 17.0
 * @version 1.0 31 May 2024
 * 
 * @see app.utils.PixelRaster
 * @see app.utils.MacroBlock
 */

public class Deblocker {
	private static int ALPHA_OFFSET = 4;
	private static int BETA_OFFSET = 51;
	private static int STRENGTH = 7;
	
	/**
	 * <p>Defines the maximum quantity of coefficients the filter can use.</p>
	 * 
	 * @see app.io.Protocol
	 */
	final static int MAX_QUANT = 100;
	
	/**
	 * <p>Starts the deblocking process for all vectorized MaroBlocks.</p>
	 * <p>The Position of the vectorized MacroBlocks is calculated and then
	 * the filter is applied. The process is multithreaded.</p>
	 * 
	 * <p><strong>Performance Warning:</strong> Even though the process
	 * is executed multithreaded, the function might impact performance a
	 * lot if either it is called often or there are a large amount of
	 * vectors to deblock.</p>
	 * 
	 * @param movementVecs	Vectors from the inter-prediction step
	 * @param composite	Frame that has the encoded vectors in it
	 */
	public void deblock(LoadDistributor<Vector> movementVecs, PixelRaster composite) {
		if (ArgumentProcessor.noDeblock) {
			return;
		}
		
		int index = clip(STRENGTH, 0, MAX_QUANT);
		int alpha = config.DEBLOCKER_ALPHAS[index + ALPHA_OFFSET];
		int beta = config.DEBLOCKER_BETAS[index + BETA_OFFSET];
		int c = config.DEBLOCKER_CS[index];
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		for (final List<Vector> vecList : movementVecs.getIterable()) {
			for (Vector vec : vecList) {
				Point vecPos = vec.getPosition();
				Point blockPos = new Point(vecPos.x + vec.getSpanX(), vecPos.y + vec.getSpanY());
				
				if (blockPos.x == 0 || blockPos.y == 0) {
					continue;
				}
				
				executor.submit(createMacroBlockDeblockRunnable(composite, blockPos, vec.getSize(), alpha, beta, c));
			}
		}
		
		executor.shutdown();
		
		try {
			while (!executor.awaitTermination(250, TimeUnit.MICROSECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Creates a runnable task that executes the deblocking filter
	 * for one MacroBlock.</p>
	 * 
	 * @return Runnable that executes the deblocking filter for a MacroBlock
	 * 
	 * @param composite	Frame that has the encoded vectors in it
	 * @param blockPos	Position of the block that should be deblocked
	 * @param offset	Offset to the block position (row and line)
	 * @param alpha	Threshold for the filter to stop filtering
	 * @param beta	"Strength" of the filtering
	 * @param c	Boundary and "strength" of the filter
	 */
	private Runnable createMacroBlockDeblockRunnable(PixelRaster composite, Point blockPos, int size, int alpha, int beta, int c) {
		Runnable task = () -> {
			for (int line = 0; line < size; line++) {
				executeDeblocking(composite, blockPos, line, alpha, beta, c, false);
			}
			
			for (int row = 0; row < size; row++) {
				executeDeblocking(composite, blockPos, row, alpha, beta, c, true);
			}
		};
		
		return task;
	}
	
	/**
	 * <p>Executed the deblocking filter for either vertical or horizontal direction.</p>
	 * <p>The dimension of the deblocking is approximately 6px x 1px. The deblocking only
	 * occur on the luma channel, due to chroma / color loss.</p>
	 * 
	 * @param composite	Frame that has the encoded vectors in it
	 * @param blockPos	Position of the block that should be deblocked
	 * @param offset	Offset to the block position (row and line)
	 * @param alpha	Threshold for the filter to stop filtering
	 * @param beta	"Strength" of the filtering
	 * @param c	Boundary and "strength" of the filter
	 * @param vertical	Is the filter applied vertically or horizontally
	 */
	private void executeDeblocking(PixelRaster composite, Point blockPos, int offset, int alpha, int beta, int c, boolean vertical) {
		Point[] points = getPositionPoints(vertical, offset, blockPos);
		double[] q0 = composite.getYUV(points[0].x, points[0].y);
		double[] q1 = composite.getYUV(points[1].x, points[1].y);
		double[] q2 = composite.getYUV(points[2].x, points[2].y);
		double[] p0 = composite.getYUV(points[3].x, points[3].y);
		double[] p1 = composite.getYUV(points[4].x, points[4].y);
		double[] p2 = composite.getYUV(points[5].x, points[5].y);
		
		double[] YDeblocked = executeDeblocking(q0[0], q1[0], q2[0], p0[0], p1[0], p2[0], beta, alpha, c);
		double[] UDeblocked = executeDeblocking(q0[1], q1[1], q2[1], p0[1], p1[1], p2[1], beta, alpha, c);
		double[] VDeblocked = executeDeblocking(q0[2], q1[2], q2[2], p0[2], p1[2], p2[2], beta, alpha, c);
		
		composite.setLuma(points[0].x, points[0].y, YDeblocked[0]);
		composite.setLuma(points[1].x, points[1].y, YDeblocked[1]);
		composite.setLuma(points[3].x, points[3].y, YDeblocked[2]);
		composite.setLuma(points[4].x, points[4].y, YDeblocked[3]);
		composite.setChroma(points[0].x, points[0].y, UDeblocked[0], VDeblocked[0]);
		composite.setChroma(points[1].x, points[1].y, UDeblocked[1], VDeblocked[1]);
		composite.setChroma(points[3].x, points[3].y, UDeblocked[2], VDeblocked[2]);
		composite.setChroma(points[4].x, points[4].y, UDeblocked[3], VDeblocked[3]);
	}

	/**
	 * <p>Get the points for q0, q1, q2, p0, p1 and p2 based and the deblocking block.</p>
	 * <p>The order of the components is always as followed:
	 * <ul><li>[0] = q0
	 * <li>[1] = q1
	 * <li>[2] = q2
	 * <li>[3] = p0
	 * <li>[4] = p1
	 * <li>[5] = p2
	 * </ul></p>
	 * 
	 * @return A list of points containing q0, q1, q2, p0, p1 and p2.	
	 * 
	 * @param vertical	Is the deblocking executed vertically or not
	 * @param offset	Offset the the original position (row and line)
	 * @param blockPos	Position of the MacroBlock to deblock
	 */
	private Point[] getPositionPoints(boolean vertical, int offset, Point blockPos) {
		Point p[] = new Point[6];
		
		if (vertical) {
			p[0] = new Point(blockPos.x + offset, blockPos.y);
			p[1] = new Point(blockPos.x + offset, blockPos.y + 1);
			p[2] = new Point(blockPos.x + offset, blockPos.y + 2);
			p[3] = new Point(blockPos.x + offset, blockPos.y - 1);
			p[4] = new Point(blockPos.x + offset, blockPos.y - 2);
			p[5] = new Point(blockPos.x + offset, blockPos.y - 3);
		} else {
			p[0] = new Point(blockPos.x, offset + blockPos.y);
			p[1] = new Point(blockPos.x + 1, offset + blockPos.y);
			p[2] = new Point(blockPos.x + 2, offset + blockPos.y);
			p[3] = new Point(blockPos.x - 1, offset + blockPos.y);
			p[4] = new Point(blockPos.x - 2, offset + blockPos.y);
			p[5] = new Point(blockPos.x - 3, offset + blockPos.y);
		}
		
		return p;
	}
	
	/**
	 * <p>This function executes the deblocking and stores the deblocked
	 * values in an array.</p>
	 * <p>First of all there's a check whether the values have to be deblocked
	 * or not (based on previous entered alpha, beta, c). If it should be
	 * deblocked, the difference is calculated and added / subtracted from
	 * the according pixel to create a near linear value increase / decrease.</p>
	 * <p>The alpha says at which threshold the filtering occurs. Beta is the "strength"
	 * as well as c.</p>
	 * 
	 * @return An array that contains the smoothed values
	 * 
	 * @param q0	Pixel 0 in the current block
	 * @param q1	Pixel 1 in the current block
	 * @param q2	Pixel 2 in the current block
	 * @param p0	Pixel (size - 1) of the left neighboring block
	 * @param p1	Pixel (size - 2) of the left neighboring block
	 * @param p2	Pixel (size - 3) of the left neighboring block
	 * @param beta	"Strength" of the filtering
	 * @param alpha	Threshold for the filtering to occur
	 * @param c	Boundary and "strength" of the filtering
	 */
	private double[] executeDeblocking(double q0, double q1, double q2, double p0, double p1, double p2, int beta, int alpha, int c) {
		double[] newColors = new double[] {q0, q1, p0, p1};
		
		if ((MathUtils.abs(p0 - q0) > alpha)
			|| MathUtils.abs(p1 - p0) > beta
			|| MathUtils.abs(q1 - q0) > beta) {
			return newColors;
		}
		
		boolean filterP1 = MathUtils.abs(p2 - p0) < beta;
		boolean filterQ1 = MathUtils.abs(p2 - p0) < beta;
		int c0 = c + (filterP1 == true ? 1 : 0) + (filterQ1 == true ? 1 : 0);
		int deltaOrigin = clip((int)(4 * (q0 - p0) + (p1 - q1) + 4) >> 3, -c0, c0);
		int deltaPosition1 = clip((int)(p2 + ((int)(p0 + q0) >> 1) - 2 * p1) >> 1, -c, c);
		
		newColors[0] = q0 - deltaOrigin;
		newColors[2] = p0 + deltaOrigin;
		newColors[3] = filterP1 ? p1 + deltaPosition1 : p1;
		newColors[1] = filterQ1 ? q1 - deltaPosition1 : q1;
		return newColors;
	}

	/**
	 * <p>Clips a value to a minimum and maximum.</p>
	 * 
	 * @return A value where <u>min <= x <= max</u>.
	 * 
	 * @param x	Value to clip
	 * @param min	Minimum value
	 * @param max	Maximum value
	 * @return
	 */
	private int clip(int x, int min, int max) {
		return x < min ? min : x > max ? max : x;
	};
}
