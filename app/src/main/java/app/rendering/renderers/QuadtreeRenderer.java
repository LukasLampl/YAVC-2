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
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;

import app.rendering.ColorManager;
import app.utils.PixelRaster;
import app.utils.components.MacroBlock;

public class QuadtreeRenderer {
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
		int[] colorCache = new int[3];
		BufferedImage[] render = new BufferedImage[3];
		render[0] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		render[1] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		render[2] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2d1 = (Graphics2D)render[0].createGraphics();
		Graphics2D g2d2 = (Graphics2D)render[1].createGraphics();
		Graphics2D g2d3 = (Graphics2D)render[2].createGraphics();
		g2d1.setColor(Color.RED);
		
		for (MacroBlock leaf : leaveNodes) {
			Point pos = leaf.getPosition();
			int size = leaf.getSize();
			g2d1.drawRect(pos.x, pos.y, size, size);
			g2d1.drawLine(pos.x, pos.y, pos.x + size, pos.y + size);
			
			int[] rgb = ColorManager.convertYUVToRGB_intARR(leaf.getMeanColor(), colorCache);
			g2d2.setColor(new Color(rgb[ColorManager.R_INDEX], rgb[ColorManager.G_INDEX], rgb[ColorManager.B_INDEX]));
			g2d2.fillRect(pos.x, pos.y, size, size);
		}
		
		g2d3.drawImage(curFrame.toBufferedImage(), 0, 0, null);
		g2d3.drawImage(render[0], 0, 0, null);
		
		g2d1.dispose();
		g2d2.dispose();
		g2d3.dispose();
		return render;
	}
}
