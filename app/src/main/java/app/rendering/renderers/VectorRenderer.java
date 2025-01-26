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

import app.engines.prediction.interprediction.Vector;

public class VectorRenderer {
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
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.createGraphics();
		g2d.setColor(Color.RED);
		
		for (Vector v : vecs) {
			Point pos = v.getPosition();
			int x1 = pos.x;
			int y1 = pos.y;
			int x2 = pos.x + v.getSpanX();
			int y2 = pos.y + v.getSpanY();
			
			switch (v.getReference()) {
			case 0:
				g2d.setColor(Color.ORANGE); break;
			case 1:
				g2d.setColor(Color.YELLOW); break;
			case 2:
				g2d.setColor(Color.BLUE); break;
			case 3:
				g2d.setColor(Color.RED); break;
			case 4:
				g2d.setColor(Color.MAGENTA); break;
			case 5:
				g2d.setColor(Color.GREEN); break;
			case 6:
				g2d.setColor(Color.CYAN); break;
			case 7:
				g2d.setColor(Color.GRAY); break;
			}
			
			g2d.drawLine(x1, y1, x2, y2);
		}
		
		g2d.dispose();
		return render;
	}
}
