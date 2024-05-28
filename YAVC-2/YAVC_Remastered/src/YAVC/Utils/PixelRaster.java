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

package YAVC.Utils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class PixelRaster {
	private double[][] Y = null;
	private double[][] U = null;
	private double[][] V = null;
	
	private Dimension dim = null;
	private ColorManager COLOR_MANAGER = new ColorManager();
	
	
	public PixelRaster(final BufferedImage img) {
		if (img == null) throw new NullPointerException("Can't invoke NULL image");
		
		this.dim = new Dimension(img.getWidth(), img.getHeight());
		this.Y = new double[img.getWidth()][img.getHeight()];
		this.U = new double[img.getWidth() / 2][img.getHeight() / 2];
		this.V = new double[img.getWidth() / 2][img.getHeight() / 2];
		
		if (img.getRaster().getDataBuffer() instanceof DataBufferInt) {
			int temp[] = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
			processIntBuffer(temp);
		} else if (img.getRaster().getDataBuffer() instanceof DataBufferByte) {
			byte[] buffer = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
			boolean hasAlpha = img.getAlphaRaster() != null ? true : false;
			processByteBuffer(buffer, hasAlpha);
		} else throw new IllegalArgumentException("Unsupported DataBuffer! DataBufferInt and DataBufferByte are supported");
	}
	
	private void processByteBuffer(final byte[] buffer, final boolean hasAlpha) {
		final int chunkSize = 4096, length = (hasAlpha == true) ? 4 : 3;
		final int width = this.dim.width, height = this.dim.height;
		int inc = length * chunkSize;
		
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for (int i = 0; i < buffer.length; i += inc) {
			final int index = i;
			final int startPosX = (i / length) % width, startPosY = (i / length) / width;
			
			Runnable task = () -> {
				int innerX = startPosX, innerY = startPosY;
				
				for (int n = 0; n < inc; n += length) {
					if (n + index >= buffer.length) {
						break;
					} else if (innerX >= width) {
						innerY++;
						innerX = 0;
					} else if (innerY >= height) break;
					
					int argb = -16777216, jumper = index + n;
					boolean newInit = (innerX % 2 == 0 && innerY % 2 == 0) ? true : false;
					
					if (hasAlpha) {
						argb += (((int)buffer[jumper++] & 0xFF) << 24); //Alpha
					}
					
					argb += ((int)buffer[jumper++] & 0xFF); //Blue
					argb += (((int)buffer[jumper++] & 0xFF) << 8); //Green
					argb += (((int)buffer[jumper] & 0xFF) << 16); //Red
					
					setThreadSafeYUV(innerX++, innerY, this.COLOR_MANAGER.convertRGBToYUV(argb), newInit);
				}
			};
			
			executor.submit(task);
		}
		
		executor.shutdown();
	
		try {
			while (!executor.awaitTermination(20, TimeUnit.MICROSECONDS)) {}
		} catch (Exception e) {e.printStackTrace();}
	}
	
	private void processIntBuffer(final int[] buffer) {
		int x = 0, y = 0, width = this.dim.width;
		
		for (int argb : buffer) {
			if (x >= width) {
				y++;
				x = 0;
			}
			
			boolean newInit = (x % 2 == 0 && y % 2 == 0) ? true : false;
			
			setThreadSafeYUV(x, y, this.COLOR_MANAGER.convertRGBToYUV(argb), newInit);
			x++;
		}
	}
	
	public PixelRaster(final Dimension dim, final double[][] Y, final double[][] U, final double[][] V) {
		if (dim.getWidth() <= 0) throw new IllegalArgumentException("Width " + dim.getWidth() + " is not supported");
		else if (dim.getHeight() <= 0) throw new IllegalArgumentException("Height " + dim.getHeight() + " is not supported");
		else if (Y == null) throw new NullPointerException("PixelRaster can't have NULL data for Luma-Y");
		else if (U == null) throw new NullPointerException("PixelRaster can't have NULL data for Chroma-U");
		else if (V == null) throw new NullPointerException("PixelRaster can't have NULL data for Chroma-V");
		
		this.dim = dim;
		this.Y = Y;
		this.U = U;
		this.V = V;
	}
	
	public double[] getYUV(final int x, final int y) {
		if (x < 0 || x >= this.dim.width) throw new ArrayIndexOutOfBoundsException("(X) " + x + " is out of bounds 0:" + this.dim.width);
		else if (y < 0 || y >= this.dim.height) throw new ArrayIndexOutOfBoundsException("(Y) " + y + " is out of bounds 0:" + this.dim.height);
		
		int subSX = x / 2, subSY = y / 2;
		return new double[] {this.Y[x][y], this.U[subSX][subSY], this.V[subSX][subSY]};
	}
	
	public void setYUV(final int x, final int y, final double[] YUV) {
		if (y < 0 || y >= this.dim.height) throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		else if (x < 0 || x >= this.dim.width) throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		
		int subSX = x / 2, subSY = y / 2;
		this.Y[x][y] = YUV[0];
		this.U[subSX][subSY] = YUV[1];
		this.V[subSX][subSY] = YUV[2];
	}
	
	public void setThreadSafeYUV(final int x, final int y, final double[] YUV, final boolean invokedUV) {
		if (y < 0 || y >= this.dim.height) throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		else if (x < 0 || x >= this.dim.width) throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		
		int subSX = x / 2, subSY = y / 2;
		this.Y[x][y] = YUV[0];

		if (invokedUV == true) {
			this.U[subSX][subSY] = YUV[1];
			this.V[subSX][subSY] = YUV[2];
		} else {
			this.U[subSX][subSY] = (this.U[subSX][subSY] + YUV[1]) / 2;
			this.V[subSX][subSY] = (this.V[subSX][subSY] + YUV[2]) / 2;
		}
	}
	
	public void setChroma(final int x, final int y, final double U, final double V) {
		if (y < 0 || y >= this.dim.height) throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		else if (x < 0 || x >= this.dim.width) throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		
		int subSX = x / 2, subSY = y / 2;
		this.U[subSX][subSY] = U;
		this.V[subSX][subSY] = V;
	}
	
	public void setLuma(final int x, final int y, final double Y) {
		if (y < 0 || y >= this.dim.height) throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		else if (x < 0 || x >= this.dim.width) throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		
		this.Y[x][y] = Y;
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
	
	public double[][][] getPixelBlock(final Point position, final int size, double[][][] cache) {
		if (position == null) throw new NullPointerException();
		double[][][] res = cache == null ? getArray(size) : size <= cache[0].length ? cache : getArray(size);
		
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
		
		int halfSize = size / 2;
		int halfPosX = position.x / 2, halfPosY = position.y / 2;
		int halfDimWidth = this.dim.width / 2, halfDimHeight = this.dim.height / 2;
		
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
	
	private double[][][] getArray(final int size) {
		double[][][] res = new double[4][][]; //0 = Y; 1 = U; 2 = V
		res[0] = new double[size][size];
		res[1] = new double[size / 2][size / 2];
		res[2] = new double[size / 2][size / 2];
		res[3] = new double[size][size];
		return res;
	}
	
	public BufferedImage toBufferedImage() {
		BufferedImage render = new BufferedImage(this.dim.width, this.dim.height, BufferedImage.TYPE_INT_ARGB);
		
		IntStream.range(0, this.dim.height).parallel().forEach(y -> {
			for (int x = 0; x < this.dim.width; x++) {
				render.setRGB(x, y, this.COLOR_MANAGER.convertYUVToRGB(getYUV(x, y)));
			}
		});
		
		return render;
	}
	
	public PixelRaster copy() {
		double[][] clonedY = new double[this.dim.width][];
		double[][] clonedU = new double[this.dim.width / 2][];
		double[][] clonedV = new double[this.dim.width / 2][];
		
		int size = this.dim.width, halfSize = this.dim.width / 2;
		
		for (int i = 0; i < size; i++) {
			clonedY[i] = this.Y[i].clone();
		}
		
		for (int i = 0; i < halfSize; i++) {
			clonedU[i] = this.U[i].clone();
			clonedV[i] = this.V[i].clone();
		}
		
		return new PixelRaster(this.dim, clonedY, clonedU, clonedV);
	}
}
