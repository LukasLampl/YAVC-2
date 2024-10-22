package decoder;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import app.config;
import utils.ColorManager;
import utils.PixelRaster;
import utils.Protocol;
import utils.Vector;

public class InputProcessor {
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
	
	public BufferedImage processFrame(byte[] content, ArrayList<PixelRaster> refs) {
		BufferedImage render = new BufferedImage(this.FRAME_DIM.width, this.FRAME_DIM.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.createGraphics();
		g2d.drawImage(refs.get(refs.size() - 1).toBufferedImage(), 0, 0, this.FRAME_DIM.width, this.FRAME_DIM.height, null);
		g2d.dispose();
		
		byte[][] split = splitFirst(content, config.VECTOR_START);
		ArrayList<Vector> vecs = split.length > 1 ? getVectors(split[1]) : null;

		if (vecs != null) {
			for (Vector v : vecs) {
				Point pos = v.getPosition();
				int EndX = pos.x + v.getSpanX();
				int EndY = pos.y + v.getSpanY();
				int reference = config.MAX_REFERENCES - v.getReference();
				PixelRaster cache = refs.get(reference);
				double[][][] block = cache.getPixelBlock(pos, v.getSize(), null);
				double[][][] IDCT = v.getIDCTCoefficientsOfAbsoluteColorDifference();
				double[][][] reconstructedColor = reconstructColors(IDCT, block, v.getSize());

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
						render.setRGB(EndX + x, EndY + y, ColorManager.convertYUVToRGB(YUV));
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
	
	private ArrayList<Vector> getVectors(byte[] vectorPart) {
		ArrayList<Vector> vecs = new ArrayList<Vector>();
		
		if (vectorPart == null) {
			return vecs;
		} else if (vectorPart.length <= 1) {
			return vecs;
		}
		
		//  LAYOUT:
		//  POSX ⊥ POSY ⊥ SPANX ⊥ SPANY ⊥ REFERENCE << 4 | SIZE ⊥ DIFFERENCE
		// ^_____________________________________________________^
		//                      = 7 Bytes offset
		int i = 0;

		while (i < vectorPart.length) {
			int posX = Protocol.getPosition(vectorPart[i], vectorPart[i + 1]);
			int posY = Protocol.getPosition(vectorPart[i + 2], vectorPart[i + 3]);
			int spanX = Protocol.getVectorSpanInt(vectorPart[i + 4]);
			int spanY = Protocol.getVectorSpanInt(vectorPart[i + 5]);
			int[] refAndSize = Protocol.getReferenceAndSizeInt(vectorPart[i + 6]);
			int ref = refAndSize[0];
			int size = refAndSize[1];

			ArrayList<double[][][]> diffs = getVectorDifferences(vectorPart, config.VECTOR_HEADER_LENGTH + i, size);
			//Length of the vector diffs
			i += ((size * size) + 2 * ((size / 2) * (size / 2))) + config.VECTOR_HEADER_LENGTH;

			Vector vec = new Vector(new Point(posX, posY), size);
			vec.setAbsolutedifferenceDCTCoefficients(diffs);
			vec.setSpanX(spanX);
			vec.setSpanY(spanY);
			vec.setReference(ref);
			vecs.add(vec);
		}
		
		return vecs;
	}
	
	private ArrayList<double[][][]> getVectorDifferences(byte[] vectorPart, int startPos, int size) {
		ArrayList<double[][][]> DCTCoeffGroups = new ArrayList<double[][][]>();
		double[][] data = getDCTCoeffsOutOfFile(vectorPart, startPos, size);
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
			int halfSize = size / 2;
			
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
						res[1][x][y] = data[1][(u / 4) + i];
						res[2][x][y] = data[2][(u / 4) + i++];
					}
				}
				
				DCTCoeffGroups.add(res);
			}
		}
		
		return DCTCoeffGroups;
	}
	
	private double[][] getDCTCoeffsOutOfFile(byte[] vectorPart, int startPos, int size) {
		int halfSize = size / 2;
		int YLength = size * size;
		int UVLength = halfSize * halfSize;
		
		double[] YBytes = new double[YLength];
		double[] UBytes = new double[UVLength];
		double[] VBytes = new double[UVLength];
		
		for (int n = 0; n < YLength; n++) {
			YBytes[n] = Protocol.getDCTCoeff(vectorPart[startPos + n]);
		}
		
		startPos += YLength;
		
		for (int n = 0; n < UVLength; n++) {
			UBytes[n] = Protocol.getDCTCoeff(vectorPart[startPos + n]);
		}
		
		startPos += UVLength;
		
		for (int n = 0; n < UVLength; n++) {
			VBytes[n] = Protocol.getDCTCoeff(vectorPart[startPos + n]);
		}

		return new double[][] {YBytes, UBytes, VBytes};
	}
	
	private byte[][] splitFirst(byte[] data, byte regex) {
		byte[][] res = new byte[2][];
		
		for (int i = 0; i < data.length; i++) {
			if (data[i] != regex) {
				continue;
			}
			
			byte[] head = new byte[i];
			byte[] tail = new byte[data.length - (i + 1)];
			System.arraycopy(data, 0, head, 0, i);
			System.arraycopy(data, i + 1, tail, 0, data.length - (i + 1));
			res[0] = head;
			res[1] = tail;
			break;
		}
		
		return res;
	}
}
