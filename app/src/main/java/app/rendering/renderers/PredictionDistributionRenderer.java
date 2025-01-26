package app.rendering.renderers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;

import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.intraprediction.IntraPredictionBlock;

public class PredictionDistributionRenderer {
	private final static Color INTRA_COLOR = Color.RED;
	private final static Color INTER_COLOR = Color.GREEN;
	
	public static BufferedImage renderPredictionDistribution(List<IntraPredictionBlock> intraBlocks,
			List<Vector> movementVectors, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.getGraphics();
		g2d.setColor(INTER_COLOR);
		
		for (Vector v : movementVectors) {
			Point pos = v.getPosition();
			g2d.fillRect(pos.x, pos.y, v.getSize(), v.getSize());
		}
		
		g2d.setColor(INTRA_COLOR);
		
		for (IntraPredictionBlock b : intraBlocks) {
			g2d.fillRect(b.getPosX(), b.getPosY(), b.getSize(), b.getSize());
		}
		
		g2d.dispose();
		return render;
	}
}
