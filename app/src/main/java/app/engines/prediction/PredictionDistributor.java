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

package app.engines.prediction;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import app.managers.LoadDistributor;
import app.rendering.ColorManager;
import app.utils.MacroBlock;

public class PredictionDistributor {
	private final static double TRIGGER_VALUE = 42;
	
	public static class Result {
		private final List<MacroBlock> inter;
		private final List<MacroBlock> intra;
		
		public Result(final List<MacroBlock> inter, final List<MacroBlock> intra) {
			this.inter = inter;
			this.intra = intra;
		}
		
		public List<MacroBlock> getIntraPredictables() {
			return this.intra;
		}
		
		public List<MacroBlock> getInterPredictables() {
			return this.inter;
		}
	}
	
	public PredictionDistributor.Result estimateBlockPredictionType(LoadDistributor<MacroBlock> macroblocks, Dimension dim) {
		List<MacroBlock> interpredictables = new ArrayList<MacroBlock>();
		List<MacroBlock> intrapredictables = new ArrayList<MacroBlock>();
		
		for (List<MacroBlock> blockList : macroblocks.getIterable()) {
			for (MacroBlock block : blockList) {
				if (block.getPositionX() == 0 || block.getPositionY() == 0
					|| block.getPositionX() + block.getSize() >= dim.width
					|| block.getPositionY() + block.getSize() >= dim.height) {
					interpredictables.add(block);
					continue;
				}
				
				double deviation = calculateDeviation(block.getSize(), block.getColors(), block.getMeanColor());
				System.out.println("Deviation: " + deviation);
				if (deviation > TRIGGER_VALUE) {
					interpredictables.add(block);
				} else {
					intrapredictables.add(block);
				}
			}
		}
		
		System.out.println(String.format("Intra:  %.2f%% | Inter: %.2f%%",
				((double)intrapredictables.size() / (double)macroblocks.getRawData().size() * 100),
				((double)interpredictables.size() / (double)macroblocks.getRawData().size() * 100)));
		
		return new PredictionDistributor.Result(interpredictables, intrapredictables);
	}
	
	private double calculateDeviation(final int size, final double[][][] color, final double[] meanColor) {
		final double mean_Y = meanColor[ColorManager.Y_INDEX];
		final double mean_U = meanColor[ColorManager.U_INDEX];
		final double mean_V = meanColor[ColorManager.V_INDEX];
		final int halfSize = size / 2;
		double max = Double.MIN_VALUE;
		double deviation = 0;
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				double delta = color[ColorManager.Y_INDEX][x][y] - mean_Y;
				deviation += delta * delta;
//				max = max < delta ? delta : max;
			}
		}
		
		for (int x = 0; x < halfSize; x++) {
			for (int y = 0; y < halfSize; y++) {
				double deltaU = color[ColorManager.U_INDEX][x][y] - mean_U;
				double deltaV = color[ColorManager.V_INDEX][x][y] - mean_V;
				deviation += deltaU * deltaU;
				deviation += deltaV * deltaV;
//				max = max < deltaU ? deltaU : max;
//				max = max < deltaV ? deltaV : max;
			}
		}
		
		deviation = Math.sqrt(deviation / (size * size));// / (max * max);
		return deviation;
	}
}
