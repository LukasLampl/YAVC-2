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

package app.rendering.renderers;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

import app.engines.prediction.intraprediction.IntraPredictionBlock;
import app.managers.LoadDistributor;
import app.rendering.ColorManager;
import app.utils.components.MacroBlock;

public class IntrapredictionRenderer {
	public static BufferedImage[] renderIntraPredictionDeltas(LoadDistributor<IntraPredictionBlock> intraPredictedBlocks,
			Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		BufferedImage render2 = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		
		for (List<IntraPredictionBlock> blockList : intraPredictedBlocks.getIterable()) {
			for (IntraPredictionBlock b : blockList) {
				if (b == null) continue;
				
				final double[][][] deltas = b.getIDCTYUVDelta();
				
				for (int x = 0; x < b.getSize(); x++) {
					final int imgX = x + b.getPositionX();
					
					for (int y = 0; y < b.getSize(); y++) {
						final int imgY = y + b.getPositionY();
						final double Y = deltas[ColorManager.Y_INDEX][x][y];
						final double U = deltas[ColorManager.U_INDEX][x >> 1][y >> 1];
						final double V = deltas[ColorManager.V_INDEX][x >> 1][y  >> 1];
						render.setRGB(imgX, imgY, ColorManager.convertYUVToRGB(new double[] {Y, U, V}));
					}
				}
			}
		}
		
		for (List<IntraPredictionBlock> blockList : intraPredictedBlocks.getIterable()) {
			for (IntraPredictionBlock b : blockList) {
				if (b == null) continue;
				
				final MacroBlock m = b.getAppendedBlock();
				
				final double[][][] deltas = b.getIDCTYUVDelta();
				final double[][][] color = m.getColors();
				
				for (int x = 0; x < b.getSize(); x++) {
					final int imgX = x + b.getPositionX();
					
					for (int y = 0; y < b.getSize(); y++) {
						final int imgY = y + b.getPositionY();
						
						final double Y = color[ColorManager.Y_INDEX][x][y] + deltas[ColorManager.Y_INDEX][x][y];
						final double U = color[ColorManager.U_INDEX][x >> 1][y >> 1] + deltas[ColorManager.U_INDEX][x >> 1][y >> 1];
						final double V = color[ColorManager.V_INDEX][x >> 1][y >> 1] + deltas[ColorManager.V_INDEX][x >> 1][y >> 1];
						
						render2.setRGB(imgX, imgY, ColorManager.convertYUVToRGB(new double[] {Y, U, V}));
					}
				}
			}
		}
		
		return new BufferedImage[] {render, render2};
	}
}
