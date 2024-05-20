/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

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

package Utils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.util.HashSet;

public class PixelRaster {
	private double[][] Y = null;
	private double[][] U = null;
	private double[][] V = null;
	
	private HashSet<Integer> colors = new HashSet<Integer>(65536);
	private Dimension dim = null;
	private ColorManager COLOR_MANAGER = new ColorManager();
	
	public PixelRaster(BufferedImage img) {
		if (img == null) {
			return;
		}
		
		this.dim = new Dimension(img.getWidth(), img.getHeight());
		this.Y = new double[img.getWidth()][img.getHeight()];
		this.U = new double[img.getWidth() / 2][img.getHeight() / 2];
		this.V = new double[img.getWidth() / 2][img.getHeight() / 2];
		
		if (img.getRaster().getDataBuffer() instanceof DataBufferInt) {
			int temp[] = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
			int x = 0, y = 0;
			
			for (int argb : temp) {
				if (x >= img.getWidth()) {
					y++;
					x = 0;
				}
				
				this.colors.add(argb);
				setYUV(x, y, this.COLOR_MANAGER.convertRGBToYUV(argb));
				x++;
			}
		} else if (img.getRaster().getDataBuffer() instanceof DataBufferByte) {
			byte[] buffer = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
			boolean hasAlpha = img.getAlphaRaster() != null ? true : false;
			int x = 0, y = 0;
			
			for (int i = 0; i < buffer.length; i++) {
				int argb = -16777216;
				
				if (hasAlpha) {
					argb += (((int)buffer[i++] & 0xff) << 24); //Alpha
				}
				
				argb += ((int)buffer[i++] & 0xff); //Blue
				argb += (((int)buffer[i++] & 0xff) << 8); //Green
				argb += (((int)buffer[i] & 0xff) << 16); //Red
				
				if (x >= img.getWidth()) {
					y++;
					x = 0;
				}
				
				this.colors.add(argb);
				setYUV(x, y, this.COLOR_MANAGER.convertRGBToYUV(argb));
				x++;
			}
		}
	}
	
	public PixelRaster(Dimension dim, double[][] Y, double[][] U, double[][] V) {
		this.dim = dim;
		this.Y = Y;
		this.U = U;
		this.V = V;
	}
	
	public double[] getYUV(int x, int y) {
		if (x < 0 || x >= this.dim.width) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + " is out of bounds 0:" + this.dim.width);
		} else if (y < 0 || y >= this.dim.height) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + " is out of bounds 0:" + this.dim.height);
		}
		
		int subSX = (int)(x * 0.5), subSY = (int)(y * 0.5);
		return new double[] {this.Y[x][y], this.U[subSX][subSY], this.V[subSX][subSY]};
	}
	
	public void setYUV(int x, int y, double[] YUV) {
		if (y < 0 || y >= this.dim.height) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		} else if (x < 0 || x >= this.dim.width) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		}
		
		int subSX = (int)(x * 0.5), subSY = (int)(y * 0.5);
		this.Y[x][y] = YUV[0];
		this.U[subSX][subSY] = YUV[1];
		this.V[subSX][subSY] = YUV[2];
	}
	
	public void setRGB(int x, int y, int argb) {
		if (y < 0 || y >= this.dim.height) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		} else if (x < 0 || x >= this.dim.width) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		}
		
		double[] YUV = this.COLOR_MANAGER.convertRGBToYUV(argb);
		int subSX = (int)(x * 0.5), subSY = (int)(y * 0.5);
		this.Y[x][y] = YUV[0];
		this.U[subSX][subSY] = YUV[1];
		this.V[subSX][subSY] = YUV[2];
	}
	
	public int getColorSpectrum() {
		return this.colors.size();
	}
	
	public void setChroma(int x, int y, double U, double V) {
		if (y < 0 || y >= this.dim.height) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		} else if (x < 0 || x >= this.dim.width) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		}
		
		int subSX = (int)(x * 0.5), subSY = (int)(y * 0.5);
		this.U[subSX][subSY] = U;
		this.V[subSX][subSY] = V;
	}
	
	public int getWidth() {
		return this.dim.width;
	}
	
	public int getHeight() {
		return this.dim.height;
	}
	
	public Dimension getDimension() {
		return this.dim;
	}
	
	public double[][][] getPixelBlock(Point position, int size, double[][][] cache) {
		double[][][] res = null;
		
		if (cache == null) {
			res = new double[4][][]; //0 = Y; 1 = U; 2 = V
			res[0] = new double[size][size];
			res[1] = new double[size / 2][size / 2];
			res[2] = new double[size / 2][size / 2];
			res[3] = new double[size][size];
		} else {
			if (size <= cache[0].length) {
				res = cache;
			} else {
				res = new double[4][][]; //0 = Y; 1 = U; 2 = V
				res[0] = new double[size][size];
				res[1] = new double[size / 2][size / 2];
				res[2] = new double[size / 2][size / 2];
				res[3] = new double[size][size];
			}
		}
		
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int absoluteX = position.x + x, absoluteY = position.y + y;
				
				if (absoluteX >= this.dim.width || x < 0
					|| absoluteY >= this.dim.height || y < 0) {
					res[3][x][y] = 1;
					continue;
				} 

				res[0][x][y] = this.Y[absoluteX][absoluteY];
				res[3][x][y] = 0;
			}
		}
		
		int halfSize = (int)(size * 0.5);
		int halfPosX = (int)(position.x * 0.5), halfPosY = (int)(position.y * 0.5);
		int halfDimWidth = (int)(this.dim.width * 0.5), halfDimHeight = (int)(this.dim.height * 0.5);
		
		for (int y = 0; y < halfSize; y++) {
			for (int x = 0; x < halfSize; x++) {
				int absoluteX = halfPosX + x, absoluteY = halfPosY + y;
				
				if (absoluteX >= halfDimWidth || x < 0
					|| absoluteY >= halfDimHeight || y < 0) {
					res[3][x][y] = 1;
					continue;
				} 
				
				res[1][x][y] = this.U[absoluteX][absoluteY];
				res[2][x][y] = this.V[absoluteX][absoluteY];
			}
		}
		
		return res;
	}
	
	public BufferedImage toBufferedImage() {
		BufferedImage render = new BufferedImage(this.dim.width, this.dim.height, BufferedImage.TYPE_INT_ARGB);
		
		for (int y = 0; y < this.dim.height; y++) {
			for (int x = 0; x < this.dim.width; x++) {
				int argb = this.COLOR_MANAGER.convertYUVToRGB(getYUV(x, y)).getRGB();
				render.setRGB(x, y, argb);
			}
		}
		
		return render;
	}
	
	public PixelRaster copy() {
		return new PixelRaster(this.dim, this.Y.clone(), this.U.clone(), this.V.clone());
	}
}
