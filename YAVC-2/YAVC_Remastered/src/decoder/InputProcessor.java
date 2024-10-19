package decoder;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import app.config;
import utils.ColorManager;
import utils.PixelRaster;
import utils.Vector;

public class InputProcessor {
	private ColorManager COLOR_MANAGER = new ColorManager();
	private Dimension FRAME_DIM = null;
	
	public void proessMetadata(String stream) {
		int dimPos = stream.indexOf("DIM:") + 4;
		String part = "";
		
		for (int i = dimPos; i < stream.length(); i++) {
			if (stream.charAt(i) == '}') {
				break;
			}
			
			part += stream.charAt(i);
		}
		
		String[] dimSizes = part.split(",");
		this.FRAME_DIM = new Dimension(Integer.parseInt(dimSizes[0]), Integer.parseInt(dimSizes[1]));
		System.out.println(this.FRAME_DIM);
	}
	
	public BufferedImage constructStartFrame(byte[] data) {
		BufferedImage render = new BufferedImage(this.FRAME_DIM.width, this.FRAME_DIM.height, BufferedImage.TYPE_INT_ARGB);

		for (int x = 0, index = 0; x < this.FRAME_DIM.width; x++) {
			for (int y = 0; y < this.FRAME_DIM.height; y++) {
				byte r = data[index];
				byte g = data[index + 1];
				byte b = data[index + 2];
				int rgb = (0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
				render.setRGB(x, y, rgb);
				index += 3;
			}
		}
		
		return render;
	}
	
	public BufferedImage processFrame(String content, ArrayList<PixelRaster> refs) {
		BufferedImage render = new BufferedImage(this.FRAME_DIM.width, this.FRAME_DIM.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.createGraphics();
		g2d.drawImage(refs.get(refs.size() - 1).toBufferedImage(), 0, 0, this.FRAME_DIM.width, this.FRAME_DIM.height, null);
		g2d.dispose();
		
		String[] split = content.split(Character.toString(config.VECTOR_START));
		ArrayList<Vector> vecs = split.length > 1 ? getVectors(split[1]) : null;

		if (vecs != null) {
			for (Vector v : vecs) {
				Point pos = v.getPosition();
				int EndX = pos.x + v.getSpanX();
				int EndY = pos.y + v.getSpanY();
				PixelRaster cache = refs.get(config.MAX_REFERENCES - v.getReference());
				double[][][] reconstructedColor = reconstructColors(v.getIDCTCoefficientsOfAbsoluteColorDifference(), cache.getPixelBlock(pos, v.getSize(), null), v.getSize());

				for (int x = 0; x < v.getSize(); x++) {
					if (EndX + x >= this.FRAME_DIM.width || EndX + x < 0) {
						continue;
					} else if (pos.x + x >= this.FRAME_DIM.width || pos.x + x < 0) {
						continue;
					}
					
					for (int y = 0; y < v.getSize(); y++) {
						if (EndY + y >= this.FRAME_DIM.height || EndY + y < 0) {
							continue;
						} else if (pos.y + y >= this.FRAME_DIM.height || pos.y + y < 0) {
							continue;
						}
						
						int subSX = x / 2, subSY = y / 2;
						double[] YUV = new double[] {reconstructedColor[0][x][y], reconstructedColor[1][subSX][subSY], reconstructedColor[2][subSX][subSY]};
						render.setRGB(EndX + x, EndY + y, this.COLOR_MANAGER.convertYUVToRGB(YUV));
					}
				}
			}
		}

		return render;
	}
	
	private double[][][] reconstructColors(double[][][] differenceOfColor, double[][][] referenceColor, int size) {
		int halfSize = size / 2;
		double[][][] reconstructedColor = new double[3][][];
		reconstructedColor[0] = new double[size][size];
		reconstructedColor[1] = new double[halfSize][halfSize];
		reconstructedColor[2] = new double[halfSize][halfSize];
		
		//Reconstruct Y-Comp
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				reconstructedColor[0][x][y] = referenceColor[0][x][y] + differenceOfColor[0][x][y];
			}
		}
		
		//Reconstruct U,V-Comp
		for (int x = 0; x < halfSize; x++) {
			for (int y = 0; y < halfSize; y++) {
				reconstructedColor[1][x][y] = referenceColor[1][x][y] + differenceOfColor[1][x][y];
				reconstructedColor[2][x][y] = referenceColor[2][x][y] + differenceOfColor[2][x][y];
			}
		}
		
		return reconstructedColor;
	}
	
	private ArrayList<Vector> getVectors(String vectorPart) {
		ArrayList<Vector> vecs = new ArrayList<Vector>();
		
		if (vectorPart.length() <= 1) {
			return vecs;
		}
		
		int offset = config.CODING_OFFSET;
		String[] vectors = vectorPart.split(Character.toString(config.VECTOR_END));
		
		//  LAYOUT:
		//  POSX ⊥ POSY ⊥ SPANX ⊥ SPANY ⊥ REFERENCE << 4 | SIZE ⊥ DIFFERENCE
		// ^_____________________________________________________^
		//                      = 7 Bytes offset
		
		for (String vector : vectors) {
			int posX = getPositionCoordinate(vector.charAt(0), vector.charAt(1));
			int posY = getPositionCoordinate(vector.charAt(2), vector.charAt(3));
			int spanX = getVectorSpanInt(vector.charAt(4), offset);
			int spanY = getVectorSpanInt(vector.charAt(5), offset);
			int[] refAndSize = getReferenceAndSizeInt((byte)vector.charAt(6), offset);
			
			int skip = 6;
			
			while (vector.charAt(skip) != config.VECTOR_DCT_START) {
				skip++;
			}
			
			ArrayList<double[][][]> diffs = getVectorDifferences(vector, ++skip, refAndSize[1]);
			
			Vector vec = new Vector(new Point(posX, posY), refAndSize[1]);
			vec.setAbsolutedifferenceDCTCoefficients(diffs);
			vec.setSpanX(spanX);
			vec.setSpanY(spanY);
			vec.setReference(refAndSize[0]);
			vecs.add(vec);
		}
		
		return vecs;
	}
	
	private int getPositionCoordinate(char c1, char c2) {
		int res = (c1 << 8) | c2;
		res -= config.CODING_OFFSET;
		return res;
	}
	
	private ArrayList<double[][][]> getVectorDifferences(String vectorPart, int startPos, int size) {
		ArrayList<double[][][]> DCTCoeffGroups = new ArrayList<double[][][]>();
		double[][] data = getDCTCoeffsOutOfFile(vectorPart, startPos, size);
		
		int halfSize = size / 2;
		int YLength = size * size;
		
		if (size == 4) {
			double[][][] res = new double[3][][];
			res[0] = new double[4][4];
			res[1] = new double[2][2];
			res[2] = new double[2][2];
			
			for (int x = 0, i = 0; x < 4; x++) {
				for (int y = 0; y < 4; y++) {
					res[0][x][y] = data[0][i++];
				}
			}

			for (int x = 0, i = 0; x < 2; x++) {
				for (int y = 0; y < 2; y++) {
					res[1][x][y] = data[1][i];
					res[2][x][y] = data[2][i++];
				}
			}
			
			DCTCoeffGroups.add(res);
		} else {
			for (int u = 0; u < YLength; u += 64) {
				double[][][] res = new double[3][][];
				res[0] = new double[size][size];
				res[1] = new double[halfSize][halfSize];
				res[2] = new double[halfSize][halfSize];
				
				for (int x = 0, i = 0; x < 8; x++) {
					for (int y = 0; y < 8; y++) {
						res[0][x][y] = data[0][u + i++];
					}
				}
				
				for (int x = 0, i = 0; x < 4; x++) {
					for (int y = 0; y < 4; y++) {
						res[1][x][y] = data[1][u + i];
						res[2][x][y] = data[2][u + i++];
					}
				}
				
				DCTCoeffGroups.add(res);
			}
		}
		
		return DCTCoeffGroups;
	}
	
	private double[][] getDCTCoeffsOutOfFile(String vectorPart, int startPos, int size) {
		int halfSize = size / 2;
		int YLength = size * size;
		int UVLength = halfSize * halfSize;
		
		double[] YBytes = new double[YLength];
		double[] UBytes = new double[UVLength];
		double[] VBytes = new double[UVLength];
		
		if (vectorPart.charAt(startPos + 0) == config.VECTOR_DCT_START) System.err.println("Err!");
		
		for (int n = 0; n < YLength; n++) {
			YBytes[n] = getDCTCoeff(vectorPart.charAt(startPos + n));
		}
		
		startPos += YLength;
		
		for (int n = 0; n < UVLength; n++) {
			UBytes[n] = getDCTCoeff(vectorPart.charAt(startPos + n));
		}
		
		startPos += UVLength;
		
		for (int n = 0; n < UVLength; n++) {
			VBytes[n] = getDCTCoeff(vectorPart.charAt(startPos + n));
		}

		return new double[][] {YBytes, UBytes, VBytes};
	}
	
	private double getDCTCoeff(char coeff) {
		coeff -= config.CODING_OFFSET;
		int result = coeff & 0x7F;
		
		if (((coeff >> 7) & 0x01) == 1) {
			result *= -1;
		}
		
		return (double)result;
	}
	
	private int getVectorSpanInt(char span, int offset) {
		span -= offset;
		int res = span & 0x7F;

		if (((span >> 7) & 0x1) == 1) {
			res *= -1;
		}
		
		return res;
	}
	
	private int[] getReferenceAndSizeInt(byte refAndSize, int offset) {
		refAndSize -= config.CODING_OFFSET;
		int ref = (refAndSize >> 4) & 0x0F, size = refAndSize & 0x0F;
		
		switch (size) {
			case 6:
				size = 128;
				break;
			case 5:
				size = 64;
				break;
			case 4:
				size = 32;
				break;
			case 3: 
				size = 16;
				break;
			case 2:
				size = 8;
				break;
			case 1:
				size = 4;
				break;
		}
		
		return new int[] {ref, size};
	}
}
