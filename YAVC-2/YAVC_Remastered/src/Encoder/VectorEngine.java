package Encoder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Main.config;
import Utils.MacroBlock;
import Utils.PixelRaster;
import Utils.Vector;

public class VectorEngine {
	private double TOTAL_MSE = 0;
	
	public ArrayList<Vector> computeMovementVectors(ArrayList<MacroBlock> differences, ArrayList<PixelRaster> refs, PixelRaster futureFrame) {
		ArrayList<Vector> vecs = new ArrayList<Vector>(differences.size() / 2);
		ArrayList<Future<Vector>> futureVecs = new ArrayList<Future<Vector>>(differences.size() / 2);
		
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		this.TOTAL_MSE = 0;
		
		for (MacroBlock block : differences) {
			Callable<Vector> task = () -> {
				int maxSize = refs.size();
				MacroBlock[] canidates = new MacroBlock[maxSize + 1];
				
				for (int i = 0, index = 0; i < maxSize; i++, index++) {
					MacroBlock bestMatch = computeHexagonSearch(refs.get(i), block);
					
					if (bestMatch != null) {
						bestMatch.setReference(config.MAX_REFERENCES - i);
					}
					
					canidates[index] = bestMatch;
				}
				
				if (futureFrame != null) {
					MacroBlock futureMatch = computeHexagonSearch(futureFrame, block);
					if (futureMatch != null) futureMatch.setReference(-1);
					canidates[refs.size()] = futureMatch;
				}
				
				MacroBlock best = evaluateBestGuess(canidates);
				Vector vec = null;
				
				if (best != null) {
					this.TOTAL_MSE += best.getMSE();
					vec = new Vector(best.getPosition());
					vec.setAppendedBlock(block);
					vec.setMostEqualBlock(best);
					vec.setSize(block.getSize());
					vec.setReference(best.getReference());
					vec.setSpanX(block.getPosition().x - best.getPosition().x);
					vec.setSpanY(block.getPosition().y - best.getPosition().y);
				}
				
				return vec;
			};
			
			futureVecs.add(executor.submit(task));
		}
		
		for (Future<Vector> fvec : futureVecs) {
			try {
				Vector vec = fvec.get();
				
				if (vec != null) {
					vecs.add(vec);
					differences.remove(vec.getAppendedBlock());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		executor.shutdown();
		
		return vecs;
	}
	
	private MacroBlock evaluateBestGuess(MacroBlock[] canidates) {
		MacroBlock best = canidates[0];
		
		for (MacroBlock b : canidates) {
			if (b == null) continue;
			else if (best == null) best = b;
			
			if (b.getMSE() < best.getMSE()) {
				best = b;
			}
		}
		
		return best;
	}
	
	private MacroBlock computeHexagonSearch(PixelRaster ref, MacroBlock blockToBeSearched) {
		double lowestMSE = Double.MAX_VALUE;
		int radius = 4, searchWindow = 24, size = blockToBeSearched.getSize();
		Point blockPos = blockToBeSearched.getPosition();
		Point centerPoint = blockToBeSearched.getPosition();
		MacroBlock mostEqualBlock = null;
		
		Point initPos = new Point(0, 0);
		Point[] searchPoints = new Point[7];
		HashSet<Point> searchedPoints = new HashSet<Point>();
		double[][][] cache = null;
		
		while (radius > 1) {
			searchPoints = getGeomPoints(6, radius, centerPoint);
			
			for (Point p : searchPoints) {
				if (p.x > blockPos.x + searchWindow
					|| p.x < blockPos.x - searchWindow
					|| p.y > blockPos.y + searchWindow
					|| p.y < blockPos.y - searchWindow
					|| searchedPoints.contains(p)
					|| p.x > ref.getWidth()
					|| p.x < 0
					|| p.y > ref.getHeight()
					|| p.y < 0) {
					continue;
				}
				
				searchedPoints.add(p);
				cache = ref.getPixelBlock(p, size, cache);
				double MSE = getMSEOfColors(cache, blockToBeSearched.getColors(), size);
				
				if (MSE < lowestMSE) {
					lowestMSE = MSE;
					initPos = p;
					mostEqualBlock = new MacroBlock(p, size, cache);
				}
			}
			
			if (initPos.equals(centerPoint)) {
				radius /= 2;
				continue;
			}
			
			centerPoint = initPos;
		}
		
		searchPoints = new Point[9];
		searchPoints[0] = centerPoint;
		searchPoints[1] = new Point(centerPoint.x + radius, centerPoint.y);
		searchPoints[2] = new Point(centerPoint.x - radius, centerPoint.y);
		searchPoints[3] = new Point(centerPoint.x, centerPoint.y + radius);
		searchPoints[4] = new Point(centerPoint.x, centerPoint.y - radius);
		searchPoints[5] = new Point(centerPoint.x + radius, centerPoint.y + radius);
		searchPoints[6] = new Point(centerPoint.x - radius, centerPoint.y - radius);
		searchPoints[7] = new Point(centerPoint.x - radius, centerPoint.y + radius);
		searchPoints[8] = new Point(centerPoint.x + radius, centerPoint.y - radius);
		
		for (Point p : searchPoints) {
			if (p.x > blockPos.x + searchWindow
				|| p.x < blockPos.x - searchWindow
				|| p.y > blockPos.y + searchWindow
				|| p.y < blockPos.y - searchWindow
				|| searchedPoints.contains(p)
				|| p.x > ref.getWidth()
				|| p.x < 0
				|| p.y > ref.getHeight()
				|| p.y < 0) {
				continue;
			}
			
			cache = ref.getPixelBlock(p, size, cache);
			double MSE = getMSEOfColors(cache, blockToBeSearched.getColors(), size);
			
			if (MSE < lowestMSE) {
				lowestMSE = MSE;
				mostEqualBlock = new MacroBlock(p, size, cache);
			}
		}
		
		if (!isValidMSE(lowestMSE, size)) return null;
		
		mostEqualBlock.setMSE(lowestMSE);
		return mostEqualBlock;
	}
	
	private boolean isValidMSE(double MSE, int size) {
		switch (size) {
		case 128:
			return MSE > 1000 ? false : true;
		case 64:
			return MSE > 1000 ? false : true;
		case 32:
			return MSE > 1000 ? false : true;
		case 16:
			return MSE > 1000 ? false : true;
		case 8:
			return MSE > 1000 ? false : true;
		case 4:
			return MSE > 1000 ? false : true;
		}
		
		return true;
	}
	
	private Point[] getGeomPoints(int edges, int radius, Point pos) {
		Point[] points = new Point[edges + 1];
		points[edges] = pos;
		double Pirad = Math.PI / (edges / 2);
		
		for (int i = 0; i < edges; i++) {
			double rad = Pirad * i;
			points[i] = new Point((int)(Math.cos(rad) * radius + pos.x), (int)(Math.sin(rad) * radius + pos.y));
		}
		
		return points;
	}
	
	private double getMSEOfColors(double[][][] col1, double[][][] col2, int size) {
		double resY = 0;
		double resU = 0;
		double resV = 0;
		double resA = 0;
		int halfSize = size / 2;
		
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double deltaY = col1[0][x][y] - col2[0][x][y];
				double deltaA = col1[3][x][y] - col2[3][x][y];
				
				resY += deltaY * deltaY;
				resA += deltaA * deltaA;
			}
		}
		
		for (int y = 0; y < halfSize; y++) {
			for (int x = 0; x < halfSize; x++) {
				double deltaU = col1[1][x][y] - col2[1][x][y];
				double deltaV = col1[2][x][y] - col2[2][x][y];
				
				resU += deltaU * deltaU;
				resV += deltaV * deltaV;
			}
		}
		
		resY *= resY;
		resA = Math.pow(resA, resA);
		
		return ((resY + resU + resV + resA) / (size * size * 4));
	}
	
	public double getVectorMSE() {
		return this.TOTAL_MSE;
	}
	
	public void drawVectorizedImage(PixelRaster render, ArrayList<Vector> vecs, ArrayList<PixelRaster> refs, PixelRaster futureFrame) {
		for (Vector v : vecs) {
			PixelRaster cache = null;
			
			if (v.getReference() == -1 && futureFrame != null) {
				cache = futureFrame;
			} else {
				cache = refs.get(config.MAX_REFERENCES - v.getReference());
			}
			
			int size = v.getSize();
			int vecEndX = v.getPosition().x + v.getSpanX();
			int vecEndY = v.getPosition().y + v.getSpanY();
			
			if (vecEndX >= render.getWidth()
				|| vecEndY >= render.getHeight()
				|| vecEndX < 0
				|| vecEndY < 0) {
				continue;
			}
			
			for (int x = 0; x < size; x++) {
				if (x + vecEndX < 0 || x + vecEndX >= render.getWidth()) continue;
				
				for (int y = 0; y < size; y++) {
					if (y + vecEndY < 0 || y + vecEndY >= render.getHeight()) continue;
					render.setYUV(vecEndX + x, vecEndY + y, cache.getYUV(x + vecEndX, y + vecEndY));
					
//					if (x == 0 && y == 0) {
//						if (v.getReference() == -1) {
//							render.setRGB(x + vecEndX, y + vecEndY, Color.GREEN.getRGB());
//						} else if (v.getReference() == 0) {
//							render.setRGB(x + vecEndX, y + vecEndY, Color.ORANGE.getRGB());
//						} else if (v.getReference() == 1) {
//							render.setRGB(x + vecEndX, y + vecEndY, Color.YELLOW.getRGB());
//						} else if (v.getReference() == 2) {
//							render.setRGB(x + vecEndX, y + vecEndY, Color.BLUE.getRGB());
//						} else if (v.getReference() == 3) {
//							render.setRGB(x + vecEndX, y + vecEndY, Color.RED.getRGB());
//						}
//					}
				}
			}
		}
	}
	
	public BufferedImage drawVectors(ArrayList<Vector> vecs, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.createGraphics();
		g2d.setColor(Color.RED);
		
		for (Vector v : vecs) {
			Point pos = v.getPosition();
			int x1 = pos.x, y1 = pos.y;
			int x2 = pos.x + v.getSpanX(), y2 = pos.y + v.getSpanY();
			
			switch (v.getReference()) {
			case -1:
				g2d.setColor(Color.GREEN); break;
			case 0:
				g2d.setColor(Color.ORANGE); break;
			case 1:
				g2d.setColor(Color.YELLOW); break;
			case 2:
				g2d.setColor(Color.BLUE); break;
			case 3:
				g2d.setColor(Color.RED); break;
			}
			
			g2d.drawLine(x1, y1, x2, y2);
		}
		
		g2d.dispose();
		return render;
	}
}
