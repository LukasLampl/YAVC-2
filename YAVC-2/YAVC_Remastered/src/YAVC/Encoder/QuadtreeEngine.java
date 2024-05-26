package YAVC.Encoder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import YAVC.Utils.MacroBlock;
import YAVC.Utils.PixelRaster;

public class QuadtreeEngine {
	private final int MAX_SIZE = 128;
	
	public ArrayList<MacroBlock> constructQuadtree(PixelRaster currentFrame) {
		if (currentFrame == null) throw new NullPointerException("PixelRaster \"currentFrame\" == NULL!");
		
		ArrayList<MacroBlock> roots = new ArrayList<MacroBlock>();
		
		final double errorThreshold = 120;
		int threads = Runtime.getRuntime().availableProcessors();
		int currentOrderNumber = 0, width = currentFrame.getWidth(), height = currentFrame.getHeight();
		ArrayList<Future<MacroBlock>> futureRoots = new ArrayList<Future<MacroBlock>>();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		for (int x = 0; x < width; x += this.MAX_SIZE) {
			for (int y = 0; y < height; y += this.MAX_SIZE) {
				final int posX = x, posY = y;
				final int currentOrder = currentOrderNumber++;
				
				Callable<MacroBlock> task = () -> {
					MacroBlock origin = new MacroBlock(new Point(posX, posY), this.MAX_SIZE);
					double[][][] comps = currentFrame.getPixelBlock(new Point(posX, posY), this.MAX_SIZE, null);
					origin.setColorComponents(comps[0], comps[1], comps[2], comps[3]);
					origin.setOrder(currentOrder);
					
					int[][] meanOf4x4BlocksInBlock = origin.calculate4x4Means();
					double originStdDeviation = origin.computeStandardDeviation(origin.calculateMeanOfCurrentBlock(meanOf4x4BlocksInBlock));
					
					if (originStdDeviation > errorThreshold) {
						origin.subdivide(errorThreshold, 0, meanOf4x4BlocksInBlock, currentFrame.getDimension());
					}
					
					return origin;
				};
				
				futureRoots.add(executor.submit(task));
			}
		}
		
		for (Future<MacroBlock> root : futureRoots) {
			try {
				MacroBlock block = root.get();
				
				if (block != null) {
					roots.add(block);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		executor.shutdown();
		
		return roots;
	}
	
	public ArrayList<MacroBlock> getLeaveNodes(ArrayList<MacroBlock> roots) {
		if (roots == null) throw new NullPointerException("No QuadtreeRoots to process");
		
		ArrayList<MacroBlock> leaveNodes = new ArrayList<MacroBlock>();
		ArrayList<Future<ArrayList<MacroBlock>>> futureLeavesList = new ArrayList<Future<ArrayList<MacroBlock>>>();
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		for (MacroBlock root : roots) {
			Callable<ArrayList<MacroBlock>> task = () -> {
				return getLeaves(root);
			};
			
			futureLeavesList.add(executor.submit(task));
		}
		
		for (Future<ArrayList<MacroBlock>> flist : futureLeavesList) {
			try {
				ArrayList<MacroBlock> nodes = flist.get();
				if (nodes != null) leaveNodes.addAll(nodes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		executor.shutdown();
		return leaveNodes;
	}
	
	private ArrayList<MacroBlock> getLeaves(MacroBlock block) {
		if (block == null) return null;
		
		ArrayList<MacroBlock> blocks = new ArrayList<MacroBlock>(4);
		
		if (block.isSubdivided()) {
			for (MacroBlock node : block.getNodes()) {
				if (node == null) continue;
				
				blocks.addAll(getLeaves(node));
			}
		} else {
			blocks.add(block);
		}
		
		return blocks;
	}
	
	public BufferedImage[] drawMacroBlocks(ArrayList<MacroBlock> leaveNodes, Dimension dim) {
		BufferedImage[] render = new BufferedImage[2];
		render[0] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		render[1] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2d1 = (Graphics2D)render[0].createGraphics();
		Graphics2D g2d2 = (Graphics2D)render[1].createGraphics();
		g2d1.setColor(Color.RED);
		
		for (MacroBlock leaf : leaveNodes) {
			Point pos = leaf.getPosition();
			int size= leaf.getSize();
			g2d1.drawRect(pos.x, pos.y, size, size);
			g2d1.drawLine(pos.x, pos.y, pos.x + size, pos.y + size);
			
			int[] rgb = leaf.getMeanColor();
			g2d2.setColor(new Color(rgb[0], rgb[1], rgb[2]));
			g2d2.fillRect(pos.x, pos.y, size, size);
		}
		
		g2d1.dispose();
		g2d2.dispose();
		
		return render;
	}
}
