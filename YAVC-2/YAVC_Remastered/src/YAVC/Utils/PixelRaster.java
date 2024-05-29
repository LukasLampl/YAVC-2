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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * <p>The class {@code YAVC.Utils.PixelRaster} is a replacement for
 * the BufferedImage, due to performance issues and wrong colorspace.
 * The PixelRaster has all important features, like getYUV (getRGB()
 * in YUV) or get PixelBlock() as replacement for getRGB()[].</p>
 * 
 * <p>While converting the BufferedImage to PixelRaster, Chroma subsampling
 * takes place. This means, for 4 pixels of luma, there is 1 pixel of 
 * chroma.</p>
 * 
 * @author Lukas Lampl
 * @since 1.0
 */

public class PixelRaster {
	private static final int PX_WITH_ALPHA_LENGTH = 4;
	private static final int PX_WITHOUT_ALPHA_LENGTH = 3;
	
	/**
	 * The Y stores all luma values of the image without subsampling
	 */
	private double[][] Y = null;
	
	/**
	 * The U stores all chroma-u values of the image with 4:2:0 subsampling
	 */
	private double[][] U = null;
	
	/**
	 * The V stores all chroma-v values of the image wit 4:2:0 subsampling
	 */
	private double[][] V = null;
	
	/**
	 * Dimension of the PixelRaster (width and height)
	 */
	private Dimension dim = null;
	
	/**
	 * <p>Invokes a ColorManager for color conversion from RGB to YUV.</p>
	 * 
	 * @see YAVC.Utils.ColorManager
	 */
	private ColorManager COLOR_MANAGER = new ColorManager();
	
	/**
	 * <p>Initialize the PixelRaster using the data of a BufferedImage.
	 * If the image is not a divisor by 4, the image gets resized.</p>
	 * 
	 * @param img	Image to convert to PixelRaster
	 * 
	 * @throws NullPointerException	if the BufferedImage is null
	 * @throws IllegalArgumentException	when the image DataBuffer is not supported
	 */
	public PixelRaster(BufferedImage img) {
		if (img == null) throw new NullPointerException("Can't invoke NULL image");
		img = scaleToNearest4Divisor(img);
		
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
	
	/**
	 * <p>Initialize the PixelRaster using a Dimension, the Y components,
	 * U components and V components</p>
	 * 
	 * @param dim	Dimension of the PixelRaster (width and height)
	 * @param Y		Y component for the PixelRaster
	 * @param U		U component for the PixelRaster
	 * @param V		V component for the PixelRaster	
	 * 
	 * @throws NullPointerException	if one of the provided components is null
	 * @throws IllegalArgumentException	when either width or height is lower or equal to 0
	 */
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
	
	/**
	 * <p>Resizes the provided image to the smaller divisor of 4</p>
	 * 
	 * @param img	Image to resize
	 */
	private BufferedImage scaleToNearest4Divisor(BufferedImage img) {
		int newWidth = img.getWidth() - (img.getWidth() % 4);
		int newHeight = img.getHeight() - (img.getHeight() % 4);
		
		if (newWidth == img.getWidth() && newHeight == img.getHeight()) return img;
		
		BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, img.getType());
		Graphics2D g2d = scaledImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.drawImage(img, 0, 0, newWidth, newHeight, null);
		g2d.dispose();
		
		return scaledImage;
	}
	
	/**
	 * <p>Reads the buffer byte by byte and fills out the YUV colors.
	 * The buffer is packed into batches, which then are processed using
	 * multithreading.</p>
	 * 
	 * <p><strong>Performance Warning:</strong> Even though there is multi-
	 * threading involved, the overall performance is totally dependent
	 * on the buffer size.
	 * Time: O(n)</p>
	 * 
	 * @param buffer	Raw data of the image, that should be processed
	 * to YUV colors.
	 * @param hasAlpha	true if the image has an alpha channel, if not
	 * hasAlpha = false
	 */
	private void processByteBuffer(final byte[] buffer, final boolean hasAlpha) {
		final int chunkSize = 4096, length = (hasAlpha == true) ? PX_WITH_ALPHA_LENGTH : PX_WITHOUT_ALPHA_LENGTH;
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
					
					//ARGB for a = 0, r = 0, g = 0, b = 0
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
	
	/**
	 * <p>Convert the DataBufferInt to the YUV colorspace
	 * and initialize the according colors in the Y, U and V
	 * components.</p>
	 * 
	 * <p><strong>Performance Warning:</strong> Even though there is multi-
	 * threading involved, the overall performance is totally dependent
	 * on the buffer size.
	 * Time: O(n)</p>
	 * 
	 * <p><strong>NOTE:</strong> the integers should be in the order of
	 * ARGB.
	 * <ul><li>First 8 bits:	Alpha
	 * <li>Bits from 8 to 16:	Red
	 * <li>Bits from 16 to 8:	Green
	 * <li>Last 8 bits:			Blue 
	 * </ul>The order is the same as in the {@code java.awt.Color}
	 * Object.</p>
	 * 
	 * @param buffer	Data of the image in form of integers.
	 */
	private void processIntBuffer(final int[] buffer) {
		int width = this.dim.width;
		final int chunkSize = 4096;
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		for (int i = 0; i < buffer.length; i += chunkSize) {
			final int index = i;
			final int startPosX = i % width, startPosY = i / width;
			
			Runnable task = () -> {
				int innerX = startPosX, innerY = startPosY;
				
				for (int n = 0; n < chunkSize && index + n < buffer.length; n++) {
					if (innerX >= width) {
						innerY++;
						innerX = 0;
					}
					
					boolean newInit = (innerX % 2 == 0 && innerY % 2 == 0) ? true : false;
					int argb = buffer[index + n];
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
	
	/**
	 * <p>Get the YUV color at the specific position
	 * with the following layout:
	 * <ul><li>double[0] = Y
	 * <li>double[1] = U
	 * <li>double[2] = V
	 * </ul></p>
	 * 
	 * @return Adouble containing all color components, for the
	 * order see above.
	 * 
	 * @param x	position X from which to get the Pixel
	 * @param y	position Y from which to get the Pixel
	 * 
	 * @throws ArrayIndexOutOfBoundsException	when either the x
	 * or y coordinate is out of the raster
	 */
	public double[] getYUV(final int x, final int y) {
		if (y < 0 || y >= this.dim.height) throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		else if (x < 0 || x >= this.dim.width) throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		
		int subSX = x / 2, subSY = y / 2;
		return new double[] {this.Y[x][y], this.U[subSX][subSY], this.V[subSX][subSY]};
	}
	
	/**
	 * <p>Sets the desired YUV color at the
	 * desired position. The YUV color should have
	 * the following order:
	 * <ul><li>double[0] = Y
	 * <li>double[1] = U
	 * <li>double[2] = V
	 * </ul></p>
	 * 
	 * @param x	position X to set the YUV color
	 * @param y	position Y to set the YUV color
	 * @param YUV	YUV color to set
	 * 
	 * @throws ArrayIndexOutOfBoundsException	when either the x
	 * or y coordinate is out of the raster
	 */
	public void setYUV(final int x, final int y, final double[] YUV) {
		if (y < 0 || y >= this.dim.height) throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		else if (x < 0 || x >= this.dim.width) throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		
		int subSX = x / 2, subSY = y / 2;
		this.Y[x][y] = YUV[0];
		this.U[subSX][subSY] = YUV[1];
		this.V[subSX][subSY] = YUV[2];
	}
	
	/**
	 * <p>Sets the desired YUV color at the
	 * desired position. The YUV color should have
	 * the following order:
	 * <ul><li>double[0] = Y
	 * <li>double[1] = U
	 * <li>double[2] = V
	 * </ul></p>
	 * 
	 * <p><strong>NOTE:</strong> the chroma subsampling is automatically
	 * handled here (from 4:4:4 to 4:2:0). chroma subsampling
	 * means, that on basis of the original luma values there
	 * is only a fraction of the chroma values, due to the
	 * lack of human eyes noticing minor changes in chroma.
	 * 4:4:4 means, that there are 4 luma values, 4 chroma
	 * values on 4 pixels. 4:2:0 means, that there are 4
	 * luma values, but only 1 chroma value on 4 pixels.
	 * <br><br>
	 * Here's an example:
	 * The values are in the following order {Y, U, V}
	 * <br><br>
	 * - Original<br>
	 * +---------------+---------------+<br>
	 * | 255, 034, 026 | 045, 255, 067 |<br>
	 * +---------------+---------------+<br>
	 * | 002, 130, 167 | 157, 182, 036 |<br>
	 * +---------------+---------------+<br>
	 * <br>
	 * - Chroma subsampled<br>
	 * +---------------+---------------+<br>
	 * | 255, 034, 026 | 045, 034, 026 |<br>
	 * +---------------+---------------+<br>
	 * | 002, 034, 026 | 157, 034, 026 |<br>
	 * +---------------+---------------+<br>
	 * <br>
	 * To prevent strong differences, YAVC averages the
	 * chroma out:<br>
	 * - YAVC Chroma subsampled<br>
	 * +---------------+---------------+<br>
	 * | 255, 150, 074 | 045, 150, 074 |<br>
	 * +---------------+---------------+<br>
	 * | 002, 150, 074 | 157, 150, 074 |<br>
	 * +---------------+---------------+<br>
	 * </p>
	 * 
	 * @param x	position X to set the YUV color
	 * @param y	position Y to set the YUV color
	 * @param YUV	YUV color to set
	 * @param invokedUV	is it the first Chroma-sample
	 * of the 2x2 pixel area
	 * 
	 * @throws ArrayIndexOutOfBoundsException	when either the x
	 * or y coordinate is out of the raster
	 */
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
	
	/**
	 * <p>Sets the desired chroma value at the
	 * desired position.</p>
	 * 
	 * @param x	position X to set the YUV color
	 * @param y	position Y to set the YUV color
	 * @param U	U value to set
	 * @param V	V value to set
	 * 
	 * @throws ArrayIndexOutOfBoundsException	when either the x
	 * or y coordinate is out of the raster
	 */
	public void setChroma(final int x, final int y, final double U, final double V) {
		if (y < 0 || y >= this.dim.height) throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		else if (x < 0 || x >= this.dim.width) throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		
		int subSX = x / 2, subSY = y / 2;
		this.U[subSX][subSY] = U;
		this.V[subSX][subSY] = V;
	}
	
	/**
	 * <p>Sets the desired luma value at the
	 * desired position.</p>
	 * 
	 * @param x	position X to set the YUV color
	 * @param y	position Y to set the YUV color
	 * @param Y	Y value to set
	 * 
	 * @throws ArrayIndexOutOfBoundsException	when either the x
	 * or y coordinate is out of the raster
	 */
	public void setLuma(final int x, final int y, final double Y) {
		if (y < 0 || y >= this.dim.height) throw new ArrayIndexOutOfBoundsException("(Y) " + y + "is out of bounds!");
		else if (x < 0 || x >= this.dim.width) throw new ArrayIndexOutOfBoundsException("(X) " + x + "is out of bounds!");
		
		this.Y[x][y] = Y;
	}
	
	/**
	 * <p>Get the width of the PixelRaster.</p>
	 * 
	 * @return width of the PixelRaster
	 */
	public int getWidth() {
		return this.dim.width;
	}
	
	/**
	 * <p>Get the height of the PixelRaster.</p>
	 * 
	 * @return height of the PixelRaster
	 */
	public int getHeight() {
		return this.dim.height;
	}
	
	/**
	 * <p>Get the Dimension of the PixelRaster.</p>
	 * 
	 * @return dimension of the PixelRaster
	 */
	public Dimension getDimension() {
		return this.dim;
	}
	
	/**
	 * <p>Get a ixelblock within the PixelRaster.
	 * The block is ordered like a PixelRaster, full 4:4:4
	 * luma and 4:2:0 chroma.</p>
	 * 
	 * <p><strong>Performance Warning:</strong> The performance is
	 * totally dependent on the size of the block.
	 * Time: O(n)</p>
	 * 
	 * @param position	Position of the block
	 * @param size	Size of the block
	 * @param cache	double array to store the values in
	 * without creating a new array
	 * @return PixelBlock from the PixelRaster
	 */
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
	
	/**
	 * <p>Get an array of 2D arrays.</p>
	 * 
	 * @param size	size if the arrays
	 * @return initialized array
	 */
	private double[][][] getArray(final int size) {
		double[][][] res = new double[4][][]; //0 = Y; 1 = U; 2 = V
		res[0] = new double[size][size];
		res[1] = new double[size / 2][size / 2];
		res[2] = new double[size / 2][size / 2];
		res[3] = new double[size][size];
		return res;
	}
	
	/**
	 * <p>Converts the PixelRaster into a BufferedImage.</p>
	 * 
	 * <p><strong>Performance Warning:</strong> Even though there is multi-
	 * threading involved, the overall performance is totally dependent
	 * on the Image dimensions.
	 * Time: O(n)</p>
	 * 
	 * @return Reconstructed BufferedImage
	 */
	public BufferedImage toBufferedImage() {
		BufferedImage render = new BufferedImage(this.dim.width, this.dim.height, BufferedImage.TYPE_INT_ARGB);
		
		IntStream.range(0, this.dim.height).parallel().forEach(y -> {
			for (int x = 0; x < this.dim.width; x++) {
				render.setRGB(x, y, this.COLOR_MANAGER.convertYUVToRGB(getYUV(x, y)));
			}
		});
		
		return render;
	}
	
	/**
	 * <p>Creates a copy of the PixelRaster
	 * without any references to other values</p>
	 * 
	 * @return Cloned PixelRaster
	 */
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
