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
import app.utils.MathUtils;
import app.utils.components.MacroBlock;

public class PredictionDistributor {
	private final static double TRIGGER_VALUE = Math.pow(Math.E, -1.125);
	
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
		double totalDeviation = 0;
		
		for (List<MacroBlock> blockList : macroblocks.getIterable()) {
			for (MacroBlock block : blockList) {
				if (isEdgeBlock(block, dim)) {
					interpredictables.add(block);
					continue;
				}
				
				double deviation = calculateDeviation(block.getSize(), block.getColors()[ColorManager.Y_INDEX],
						block.getMeanColor()[ColorManager.Y_INDEX]);
				totalDeviation += deviation;
				
				if (deviation > TRIGGER_VALUE) {
					interpredictables.add(block);
				} else {
					intrapredictables.add(block);
				}
			}
		}
		
		System.out.println("Avg Dev: " + (totalDeviation / ((double)macroblocks.getRawData().size())));
		System.out.println(String.format("Intra:  %.2f%% | Inter: %.2f%%",
				((double)intrapredictables.size() / (double)macroblocks.getRawData().size() * 100),
				((double)interpredictables.size() / (double)macroblocks.getRawData().size() * 100)));
		
		return new PredictionDistributor.Result(interpredictables, intrapredictables);
	}
	
	private boolean isEdgeBlock(final MacroBlock block, final Dimension dim) {
		return block.getPositionX() == 0 || block.getPositionY() == 0
				|| block.getPositionX() + block.getSize() >= dim.width
				|| block.getPositionY() + block.getSize() >= dim.height;
	}
	
	private double calculateDeviation(final int size, final double[][] Ycolor, final double meanYColor) {
		double deviation = 0;
		double max = Double.MIN_VALUE;
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				double delta = Ycolor[x][y] - meanYColor;
				deviation += delta * delta;
				max = MathUtils.max(max, MathUtils.abs(delta));
			}
		}
		
		deviation = Math.sqrt(deviation / (size * size)) / (max + 1e-10);
		return deviation;
	}
}
