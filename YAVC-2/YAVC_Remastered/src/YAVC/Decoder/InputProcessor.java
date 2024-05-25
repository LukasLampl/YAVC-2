package YAVC.Decoder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import YAVC.Main.config;
import YAVC.Utils.ColorManager;
import YAVC.Utils.MacroBlock;
import YAVC.Utils.Vector;

public class InputProcessor {
	public BufferedImage constructStartFrame(String content, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		String[] pixels = content.split("\\.");
		
		for (int x = 0, index = 0; x < dim.width; x++) {
			for (int y = 0; y < dim.height; y++) {
				render.setRGB(x, y, Integer.parseInt(pixels[index++]));
			}
		}
		
		return render;
	}
	
	public BufferedImage processFrame(String content, ArrayList<BufferedImage> refs, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.createGraphics();
		g2d.drawImage(refs.get(refs.size() - 1), 0, 0, dim.width, dim.height, null);
		g2d.dispose();
		
		String[] split = content.split(Character.toString(config.VECTOR_START));
		
		ColorManager colorManager = new ColorManager();
		ArrayList<MacroBlock> diffs = getDifferences(split[0]);
		
		ArrayList<Vector> vecs = null;
		
		if (split.length > 1) {
			vecs = getVectors(split[1]);
		}
		
		for (MacroBlock b : diffs) {
			Point pos = b.getPosition();
			
			for (int x = 0; x < b.getSize(); x++) {
				if (pos.x + x < 0 || pos.x + x >= dim.width) continue;
				
				for (int y = 0; y < b.getSize(); y++) {
					if (pos.y + y < 0 || pos.y + y >= dim.height) continue;
					
					render.setRGB(x + pos.x, y + pos.y, colorManager.convertYUVToRGB(b.getYUV(x, y)));
				}
			}
		}

		for (Vector v : vecs) {
			Point pos = v.getPosition();
			int EndX = pos.x + v.getSpanX(), EndY = pos.y + v.getSpanY();
			BufferedImage cache = refs.get(config.MAX_REFERENCES - v.getReference());

			for (int x = 0; x < v.getSize(); x++) {
				if (EndX + x >= dim.width || EndX + x < 0) continue;
				if (pos.x + x >= dim.width || pos.x + x < 0) continue;
				
				for (int y = 0; y < v.getSize(); y++) {
					if (EndY + y >= dim.height || EndY + y < 0) continue;
					if (pos.y + y >= dim.height || pos.y + y < 0) continue;

					render.setRGB(EndX + x, EndY + y, cache.getRGB(pos.x + x, pos.y + y));
				}
			}
		}

		return render;
	}
	
	private ArrayList<MacroBlock> getDifferences(String differencesPart) {
		ArrayList<MacroBlock> diffs = new ArrayList<MacroBlock>();
		if (differencesPart.length() <= 1) return diffs;
		
		ColorManager colorManager = new ColorManager();
		differencesPart = differencesPart.substring(1, differencesPart.length());
		String[] blocks = differencesPart.split(";");
		
		for (String block : blocks) {
			String[] content = block.split("\\$");
			String[] values = content[0].split("\\.");
			
			if (content[1].length() < 3) continue;
			int size = (int)content[1].charAt(0);
			int posX = (int)content[1].charAt(1), posY = (int)content[1].charAt(2);
			
			double Y[][] = new double[size][size];
			double A[][] = new double[size][size];
			double U[][] = new double[size / 2][size / 2];
			double V[][] = new double[size / 2][size / 2];
			
			for (int x = 0, index = 0; x < size; x++) {
				for (int y = 0; y < size; y++) {
					Color col = new Color(Integer.parseInt(values[index++]));
					double YUV[] = colorManager.convertRGBToYUV(col);
					
					int subSX = x / 2, subSY = y / 2;
					Y[x][y] = YUV[0];
					U[subSX][subSY] = YUV[1];
					V[subSX][subSY] = YUV[2];
				}
			}
			
			diffs.add(new MacroBlock(new Point(posX, posY), size, new double[][][] {Y, U, V, A}));
		}
		
		return diffs;
	}
	
	private ArrayList<Vector> getVectors(String vectorPart) {
		ArrayList<Vector> vecs = new ArrayList<Vector>();
		if (vectorPart.length() <= 1) return vecs;
		
		int offset = config.CODING_OFFSET;
		
		for (int i = 0; i < vectorPart.length(); i += config.CODED_VECTOR_LENGTH) {
			if (vectorPart.charAt(i) == config.VECTOR_START) System.err.println("ERROR");
			int posX = (int)(vectorPart.charAt(i)) - offset, posY = (int)(vectorPart.charAt(i + 1)) - offset;
			int[] span = getVectorSpanInt(vectorPart.charAt(i + 2), offset);
			int[] refAndSize = getReferenceAndSizeInt(vectorPart.charAt(i + 3), offset);
			
			Vector vec = new Vector(new Point(posX, posY), refAndSize[1]);
			vec.setSpanX(span[0]);
			vec.setSpanY(span[1]);
			vec.setReference(refAndSize[0]);
			vecs.add(vec);
		}
		
		return vecs;
	}
	
	private int[] getVectorSpanInt(char span, int offset) {
		int reducedSpan = (int)span - offset;
		int y = (reducedSpan & 0xFF);
		int x = (reducedSpan >> 8) & 0xFF;

		if (((reducedSpan >> 7) & 0x1) == 1) {
			y = -1 * (y & 0x7F);
		} else {
			y = y & 0x7F;
		}
		
		if (((reducedSpan >> 15) & 0x1) == 1) {
			x = -1 * (x & 0x7F);
		} else {
			x = x & 0x7F;
		}
		
		return new int[] {x, y};
	}
	
	private int[] getReferenceAndSizeInt(char refAndSize, int offset) {
		int ref = (refAndSize >> 8) & 0xFF, size = refAndSize & 0xFF;
		return new int[] {ref, size};
	}
}
