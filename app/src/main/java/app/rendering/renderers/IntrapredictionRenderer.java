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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

import app.engines.prediction.intraprediction.IntraPredictionBlock;
import app.managers.LoadDistributor;
import app.rendering.ColorManager;
import app.utils.MacroBlock;

public class IntrapredictionRenderer {
	public static BufferedImage renderIntraPrediction(List<MacroBlock> intraBlocks, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.getGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
		g2d.setColor(new Color(255, 0, 0, 100));
		double[] YUVCache = new double[3]; //Size of 3 because of 3 channels
		
		for (MacroBlock b : intraBlocks) {
			for (int x = 0; x < b.getSize(); x++) {
				for (int y = 0; y < b.getSize(); y++) {
					if (x + b.getPosition().x >= dim.width
						|| x + b.getPosition().x < 0
						|| y + b.getPosition().y >= dim.height
						|| y + b.getPosition().y < 0) {
						continue;
					}
					
					int argb = ColorManager.convertYUVToRGB(b.getYUV(x, y, YUVCache));
					render.setRGB(x + b.getPosition().x, y + b.getPosition().y, argb);
				}
			}
		}
		
		for (MacroBlock b : intraBlocks) {
			g2d.drawRect(b.getPositionX(), b.getPositionY(), b.getSize(), b.getSize());
		}
		
		g2d.dispose();
		return render;
	}
	
	public static BufferedImage[] renderIntraPredictionDeltas(LoadDistributor<IntraPredictionBlock> intraPredictedBlocks,
			Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		BufferedImage render2 = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		
		for (List<IntraPredictionBlock> blockList : intraPredictedBlocks.getIterable()) {
			for (IntraPredictionBlock b : blockList) {
				if (b == null) continue;
				
				final double[][][] deltas = b.getDelta();
				
				for (int x = 0; x < b.getSize(); x++) {
					final int imgX = x + b.getPosX();
					
					for (int y = 0; y < b.getSize(); y++) {
						final int imgY = y + b.getPosY();
						final double Y = deltas[ColorManager.Y_INDEX][x][y];
						final double U = deltas[ColorManager.U_INDEX][x / 2][y / 2];
						final double V = deltas[ColorManager.V_INDEX][x / 2][y  / 2];
						render.setRGB(imgX, imgY, ColorManager.convertYUVToRGB(new double[] {Y, U, V}));
					}
				}
			}
		}
		
		for (List<IntraPredictionBlock> blockList : intraPredictedBlocks.getIterable()) {
			for (IntraPredictionBlock b : blockList) {
				if (b == null) continue;
				
				final MacroBlock m = b.getAppendedBlock();
				
				final double[][][] deltas = b.getDelta();
				final double[][][] color = m.getColors();
				
				for (int x = 0; x < b.getSize(); x++) {
					final int imgX = x + b.getPosX();
					
					for (int y = 0; y < b.getSize(); y++) {
						final int imgY = y + b.getPosY();
						
						final double Y = color[ColorManager.Y_INDEX][x][y] + deltas[ColorManager.Y_INDEX][x][y];
						final double U = color[ColorManager.U_INDEX][x / 2][y / 2] + deltas[ColorManager.U_INDEX][x / 2][y / 2];
						final double V = color[ColorManager.V_INDEX][x / 2][y / 2] + deltas[ColorManager.V_INDEX][x / 2][y / 2];
						
						render2.setRGB(imgX, imgY, ColorManager.convertYUVToRGB(new double[] {Y, U, V}));
					}
				}
			}
		}
		
		return new BufferedImage[] {render, render2};
	}
}
