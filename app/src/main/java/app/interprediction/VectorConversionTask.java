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

package app.interprediction;

import java.util.List;
import java.util.concurrent.RecursiveAction;

import app.config;
import app.dct.DCTConstants;
import app.io.Protocol;
import app.utils.ArrayUtils;
import app.utils.ListManager;

/**
 * The {@code VectorConversionTask} class is a Recursive splitting
 * task that allows to process a byte array containing the vectors and
 * a list of the indexes within a very short amount of time.
 * 
 * <p><b>Stats:</b><br>
 * Processing time for <u>~15.000</u> Vectors on a i7-7700HQ @ 2.80 GHz:<br><br>
 * <table border="1">
 * 	<tr>
 * 		<td>Min</td><td>Max</td><td>Avg.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>7ms</td><td>86ms</td><td>~12ms</td>
 * 	</tr>
 * </table>
 * <br>
 * <i>The data is from the 21.11.2024 and might not represent the current stats.
 * It is only there for an orientation.</i>
 * </p>
 * 
 * @author Lukas Lampl
 * @since 1.2.5 [Optimized prototype]
 */
public class VectorConversionTask extends RecursiveAction {
	private static final long serialVersionUID = -1416920943935831433L;
	
	/**
	 * Determines the total amount of work per Recursive task measured in pixels.
	 */
	private static final int MAX_WORK = 512 * 512;
	
	/**
	 * Holds the start index in the indexes array.
	 */
	private int start = 0;
	
	/**
	 * Holds the end index in the indexes array.
	 */
	private int end = 0;
	
	/**
	 * Holds the indexes of each vector.
	 */
	private List<Integer> indexes = null;
	
	/**
	 * The raw data that contains all vectors.
	 */
	private byte[] data = null;
	
	/**
	 * The vector manager in which to write all read in vector for
	 * further processing.
	 */
	private ListManager<Vector> vectorManager = null;
	
	/**
	 * Flag for whether the vectors should be converted by a single thread or not.
	 * This should only be used, when the order of vectors is important.
	 */
	private boolean executeOnSingleThread = false;
	
	/**
	 * Initializes a {@code VectorConversionTask} with the given
	 * boundaries and data.
	 * 
	 * @param start			From where to start working in the indexes array.
	 * @param end			To where working in the indexes array.
	 * @param indexes		An array that contains all vector indexes/positions in the raw data.
	 * @param data			Raw data containing the vectors in byte form.
	 * @param vectorManager	The vector manager in which to write the vectors into.
	 */
	public VectorConversionTask(int start, int end, List<Integer> indexes, byte[] data, ListManager<Vector> vectorManager) {
		this.start = start;
		this.end = end;
		this.indexes = indexes;
		this.data = data;
		this.vectorManager = vectorManager;
	}
	
	/**
	 * Sets the ConversionTask to a single threaded execution mode.
	 */
	public void setSingleThreaded() {
		this.executeOnSingleThread = true;
	}
	
	/**
	 * Computes how much workload a task would have with the current
	 * start and end and based on that decides whether to split further
	 * or execute the vector translation.
	 */
	@Override
	protected void compute() {
		int totalWorkload = getWorkloadOfThread();
		
		if (totalWorkload > MAX_WORK && !this.executeOnSingleThread) {
			int middle = (this.start + this.end) / 2;
			VectorConversionTask tl = new VectorConversionTask(this.start, middle, this.indexes, this.data, this.vectorManager);
			VectorConversionTask tr = new VectorConversionTask(middle, this.end, this.indexes, this.data, this.vectorManager);
			invokeAll(tl, tr);
		} else {
			execute();
		}
	}
	
	/**
	 * Calculates the current workload of the RecursiveTask
	 * by adding the sizes of the vectors and returning it.
	 * 
	 * <p><b>Note:</b><br>
	 * Since the task gets indexes the length of an object is
	 * equal to this: {@code index_obj2 - index_obj1} or expressed
	 * in other terms {@code obj[i] - obj[i - 1]}.
	 * </p>
	 * 
	 * @return The workload of the current task measured in pixels.
	 */
	private int getWorkloadOfThread() {
		int totalWorkload = 0;
		
		for (int i = this.start; i < this.end; i++) {
			if (i == 0) {
				totalWorkload += indexes.get(i);
				continue;
			}
			
			totalWorkload += (indexes.get(i) - indexes.get(i - 1));
		}
		
		return totalWorkload;
	}
	
	/**
	 * Executes the given task by working the indexes from the given start
	 * and end down. The function essentially creates the vectors based on the
	 * {@link #data} and {@link #indexes}, in the end it adds the vectors to
	 * the {@link #vectorManager}
	 */
	public void execute() {
		for (int i = this.start; i < this.end; i++) {
			final int index = indexes.get(i).intValue();
			final int posX = Protocol.getPosition(this.data[index], this.data[index + 1]);
			final int posY = Protocol.getPosition(this.data[index + 2], this.data[index + 3]);
			final int spanX = Protocol.getVectorSpanInt(this.data[index + 4]);
			final int spanY = Protocol.getVectorSpanInt(this.data[index + 5]);
			final int[] refAndSize = Protocol.getReferenceAndSizeInt(this.data[index + 6]);
			final int ref = refAndSize[0];
			final int size = refAndSize[1];
			Vector vec = vectorManager.getCachedObj();
			
			if (vec == null) {
				vec = new Vector(posX, posY, size);
			}
			
			vec.setSize(size);
			vec.setPosition(posX, posY);
			
			double[][][] diffs = getVectorDifferences(this.data, index + Protocol.VECTOR_HEADER_LENGTH, size, vec);
			vec.setAbsolutedifferenceDCTCoefficients(diffs);
			vec.setSpanX(spanX);
			vec.setSpanY(spanY);
			vec.setReference(ref);
			vectorManager.add(vec);
		}
	}

	/**
	 * Gets an 3D double array that represent the DCT-II coefficients.
	 * 
	 * <p><b>Note:</b><br>
	 * The function gets one when the {@code size = 4}. When the {@code size > 4} the
	 * function iterates multiple times until the size is fully read in. The partial reading
	 * in is defined by the Protocol, which says that a n-sized block has 3 data channels for
	 * the 3 color channels, each composes of 8x8 (or subsampled 4x4) datablocks that are
	 * aligned in a row. In other terms:<br><br>
	 * 
	 * If I had an 16x16 sized DCT-II block data the Protocol will split it into these arrays:<br>
	 * - Y[16 * 16]<br>
	 * - U[8 * 8]<br>
	 * - V[8 * 8]<br><br>
	 * 
	 * After that It'll split the 16x16 block into 8x8 subblocks, for performance reasons in the DCT-II
	 * conversion process. Now we have 4 8x8 blocks each containing a 8x8 Y-Channel, 4x4 U-Channel and
	 * 4x4 V-Channel.<br><br>
	 * 
	 * This data is then filled in the previously mentioned arrays by firstly taking the first block,
	 * writing it's data into the given array (Y-Channel in Y-array, U-Channel in U-array and so on).
	 * After the first has finished the second block is read in, but now the offset of the previous
	 * data is used to offset the writer to the end of the previous block data, which in other words
	 * mean, the second, third and fourth block are starting all at a offset of 64 from each other or
	 * 16 for the subsampled channels.<br><br>
	 * 
	 * To convert it back it just has to be read with the same algorithm.
	 * </p>
	 * 
	 * @param data			The raw data containing the DCT-II coefficients.
	 * @param startPos		The position from where to start getting the DCT-II coefficients.
	 * @param size			The size of the vector.
	 * @param cachedVector	A Vector that can be overwritten (GC reasons).
	 * @return The converted vector color difference.
	 */
	private double[][][] getVectorDifferences(final byte[] data, final int startPos, final int size, final Vector cachedVector) {
		double[][][] DCTCoeffGroups = ArrayUtils.get3DArray(size, true);
		final int YLength = size * size;
		final int subSSize = size / config.SUBSAMPLE_COEFFICIENT;
		final int UVLength = subSSize * subSSize;
		
		final int YStart = startPos;
		final int UStart = YStart + YLength;
		final int VStart = UStart + UVLength;
		
		if (size == 4) {
			writeDCTCoeffsOutOfByteStream(data, 4, DCTCoeffGroups, YStart, UStart, VStart, 0, 0);
		} else {
			final int subSInc = 64 / config.SUBSAMPLE_COEFFICIENT;
			
			for (int u = 0, subSU = 0, x = 0, y = 0; u < YLength; u += 64, subSU += subSInc, x += 8) {
				if (x >= size) {
					y += 8;
					x = 0;
				}
				
				final int actualYStart = YStart + u;
				final int actualUStart = UStart + subSU;
				final int actualVStart = VStart + subSU;
				writeDCTCoeffsOutOfByteStream(data, 8, DCTCoeffGroups, actualYStart, actualUStart, actualVStart, x, y);
			}
		}
		
		return DCTCoeffGroups;
	}
	
	/**
	 * Gets the DCT-II coefficients out of the raw data stream and writes them into the given array.
	 * 
	 * @param vectorPart				The raw data from which to get the DCT-II coefficients.
	 * @param size						Size of the vector.
	 * @param arrayToWriteInto			Array to write the coefficients into.
	 * @param YStart					Start of the Y channel data.
	 * @param UStart					Start of the U channel data.
	 * @param VStart					Start of the V channel data.
	 * @param offsetX					The offset to the x in which to write the data.
	 * @param offsetY					The offset to the y in which to write the data.
	 */
	private void writeDCTCoeffsOutOfByteStream(final byte[] vectorPart, final int size,
			double[][][] arrayToWriteInto, final int YStart, final int UStart, final int VStart,
			final int offsetX, final int offsetY) {
		final int YLength = size * size;
		final int subSSize = size / config.SUBSAMPLE_COEFFICIENT;
		final int UVLength = subSSize * subSSize;
		final int lengthTillMatrixBreak = size;
		final int halfLengthTillMatrixBreak = subSSize;
		final int subSOffsetX = offsetX / config.SUBSAMPLE_COEFFICIENT;
		final int subSOffsetY = offsetY / config.SUBSAMPLE_COEFFICIENT;
		int x = 0;
		int y = 0;
		
		double[][] YChannel = arrayToWriteInto[DCTConstants.Y_COEFFS_INDEX];
		double[][] UChannel = arrayToWriteInto[DCTConstants.U_COEFFS_INDEX];
		double[][] VChannel = arrayToWriteInto[DCTConstants.V_COEFFS_INDEX];
				
		for (int n = 0; n < YLength; n++, y++) {
			if (y >= lengthTillMatrixBreak) {
				x++;
				y = 0;
			}
			
			final int actualX = x + offsetX;
			final int actualY = y + offsetY;
			YChannel[actualX][actualY] = Protocol.getDCTCoeff(vectorPart[YStart + n]);
		}

		x = 0;
		y = 0;
		
		for (int n = 0; n < UVLength; n++, y++) {
			if (y >= halfLengthTillMatrixBreak) {
				x++;
				y = 0;
			}
			
			final int actualX = subSOffsetX + x;
			final int actualY = subSOffsetY + y;
			UChannel[actualX][actualY] = Protocol.getDCTCoeff(vectorPart[UStart + n]);
			VChannel[actualX][actualY] = Protocol.getDCTCoeff(vectorPart[VStart + n]);
		}
	}
}
