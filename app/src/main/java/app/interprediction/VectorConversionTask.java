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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import app.io.Protocol;
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
public class VectorConversionTask extends RecursiveTask<Void> {
	private static final long serialVersionUID = -1416920943935831433L;
	
	/**
	 * Determines the total amount of work per Recursive task measured in pixels.
	 */
	private static final int MAX_WORK = 256 * 256;
	
	/**
	 * Holds the start index in the indexes array.
	 */
	private int start = 0;
	
	/**
	 * Holds the end index in the indexes array.
	 */
	private int end = 0;
	
	/**
	 * A possible cache structure to reduce stress on GC, when the vectors
	 * are read out.
	 */
	private double[][][] fileDataCache = null;
	
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
	 * Computes how much workload a task would have with the current
	 * start and end and based on that decides whether to split further
	 * or execute the vector translation.
	 */
	@Override
	protected Void compute() {
		int totalWorkload = getWorkloadOfThread();
		
		if (totalWorkload > MAX_WORK) {
			int middle = (this.start + this.end) / 2;
			VectorConversionTask tl = new VectorConversionTask(this.start, middle, this.indexes, this.data, this.vectorManager);
			VectorConversionTask tr = new VectorConversionTask(middle, this.end, this.indexes, this.data, this.vectorManager);
			invokeAll(tl, tr);
		} else {
			initFileDataCache();
			execute();
		}
		
		return null;
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
	 * Sets up the {@link #fileDataCache} to assure that the
	 * vector converter can use caches and thus reducing GC
	 * pressure.
	 */
	private void initFileDataCache() {
		//int[] sizes = {2 * 2, 4 * 4, 8 * 8, 16 * 16, 32 * 32, 64 * 64, 128 * 128};
		int[] sizes = {4, 16, 64, 256, 1024, 4096, 16384};
		this.fileDataCache = new double[7][][]; //First dim for index, second for optional two channels, thrid the actual cache
		
		for (int i = 0; i < sizes.length; i++) {
			int index = convertLengthToIndex(sizes[i]);
			this.fileDataCache[index] = new double[2][]; //For two channels (U and V)
			this.fileDataCache[index][0] = new double[sizes[i]];
			this.fileDataCache[index][1] = new double[sizes[i]];
		}
	}
	
	/**
	 * Converts a given length into an index.
	 * 
	 * @param length	The length to convert.
	 * @return The index in the {@link #fileDataCache}.
	 * @throws IllegalArgumentException	When the given length is not supported.
	 */
	private int convertLengthToIndex(int length) {
		switch (length) {
		case 4: //2 * 2
			return 0;
		case 16: //4 * 4
			return 1;
		case 64: //8 * 8
			return 2;
		case 256: //16 * 16
			return 3;
		case 1024: //32 * 32
			return 4;
		case 4096: //64 * 64
			return 5;
		case 16384: //128 * 128
			return 6;
		default:
			throw new IllegalArgumentException("The length " + length + " cannot be converted to an index.");
		}
	}
	
	/**
	 * Executes the given task by working the indexes from the given start
	 * and end down. The function essentially creates the vectors based on the
	 * {@link #data} and {@link #indexes}, in the end it adds the vectors to
	 * the {@link #vectorManager}
	 */
	public void execute() {
		for (int i = this.start; i < this.end; i++) {
			int index = indexes.get(i).intValue();
			int posX = Protocol.getPosition(data[index], data[index + 1]);
			int posY = Protocol.getPosition(data[index + 2], data[index + 3]);
			int spanX = Protocol.getVectorSpanInt(data[index + 4]);
			int spanY = Protocol.getVectorSpanInt(data[index + 5]);
			int[] refAndSize = Protocol.getReferenceAndSizeInt(data[index + 6]);
			int ref = refAndSize[0];
			int size = refAndSize[1];
			Vector vec = vectorManager.getCachedObj();
			
			if (vec == null) {
				vec = new Vector(posX, posY, size);
			}
			
			vec.setSize(size);
			vec.setPosition(posX, posY);
			
			ArrayList<double[][][]> diffs = getVectorDifferences(data, Protocol.VECTOR_HEADER_LENGTH + index, size, vec);
			vec.setAbsolutedifferenceDCTCoefficients(diffs);
			vec.setSpanX(spanX);
			vec.setSpanY(spanY);
			vec.setReference(ref);
			vectorManager.add(vec);
		}
	}

	/**
	 * Converts a raw data stream into the representing vector color differences.
	 * First the data is received out of the data stream, then it is processed by
	 * setting the according values.
	 * 
	 * @param vectorPart	The raw data containing the DCT-II coefficients.
	 * @param startPos		The position from where to start getting the DCT-II coefficients.
	 * @param size			The size of the vector.
	 * @param cachedVector	A Vector that can be overwritten (GC reasons).
	 * @return The converted vector color difference.
	 */
	private ArrayList<double[][][]> getVectorDifferences(byte[] vectorPart, int startPos, int size, Vector cachedVector) {
		ArrayList<double[][][]> cachedGroups = cachedVector.getDCTCoefficientsOfAbsoluteColorDifference();
		ArrayList<double[][][]> DCTCoeffGroups = new ArrayList<double[][][]>();
		double[][] data = getDCTCoeffsOutOfFile(vectorPart, startPos, size);
		int YLength = size * size;
		
		if (size == 4) {
			DCTCoeffGroups.add(process4x4DCTCoefficients(cachedGroups, data));
		} else {
			boolean wasCachedGroup4x4Block = cachedGroups == null ? true : cachedGroups.size() == 1;
			
			for (int u = 0; u < YLength; u += 64) {
				DCTCoeffGroups.add(processNon4x4Coefficients(cachedGroups, data, u, wasCachedGroup4x4Block));
			}
		}
		
		return DCTCoeffGroups;
	}
	
	/**
	 * Processes a sub-block of a non 4x4 block.
	 * 
	 * @param cachedGroups				Cached double arrays. (GC reasons)
	 * @param data						The data to put.
	 * @param fraction					The fraction in which to put the data into.
	 * @param wasCachedGroup4x4Block	Flag for whether the previous group was a 4x4 block.
	 * @return An array filled with the data.
	 */
	private double[][][] processNon4x4Coefficients(ArrayList<double[][][]> cachedGroups,
			double[][] data, int fraction, boolean wasCachedGroup4x4Block) {
		int uFrac = (fraction / 4);
		double[][][] res;
		
		if (cachedGroups == null) {
			res = getArray(8);
		} else {
			if (cachedGroups.isEmpty() || wasCachedGroup4x4Block) {
				res = getArray(8);
			} else {
				res = cachedGroups.remove(0);
			}
		}
		
		for (int x = 0, i = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				res[0][x][y] = data[0][fraction + i++];
			}
		}
		
		for (int x = 0, i = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				res[1][x][y] = data[1][uFrac + i];
				res[2][x][y] = data[2][uFrac + i++];
			}
		}
		
		return res;
	}
	
	/**
	 * Processes a single 4x4 block.
	 * 
	 * @param cachedGroups	A cache of double arrays. (GC reasons)
	 * @param data			The data to put.
	 * @return An array filled with the coefficients.
	 */
	private double[][][] process4x4DCTCoefficients(ArrayList<double[][][]> cachedGroups, double[][] data) {
		double[][][] res = cachedGroups == null ? getArray(4) : cachedGroups.size() > 0 ? cachedGroups.remove(0) : getArray(4);
		
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
		
		return res;
	}
	
	/**
	 * Creates an empty 3D array with the given size
	 * and subsampled sizes. (4:2:0)
	 * 
	 * @param size	The size of the array.
	 * @return The created 3D array.
	 */
	private double[][][] getArray(int size) {
		int halfSize = size / 2;
		double[][][] arr = new double[3][][];
		arr[0] = new double[size][size];
		arr[1] = new double[halfSize][halfSize];
		arr[2] = new double[halfSize][halfSize];
		return arr;
	}
	
	/**
	 * Gets the DCT-II coefficients out of the raw data stream.
	 * 
	 * @param vectorPart	The raw data from which to get the DCT-II coefficients.
	 * @param startPos		Position from where to start getting the DCT-II coefficients.
	 * @param size			Size of the vector.
	 * @return An array with the DCT-II coefficients.
	 */
	private double[][] getDCTCoeffsOutOfFile(byte[] vectorPart, int startPos, int size) {
		int halfSize = size / 2;
		int YLength = size * size;
		int UVLength = halfSize * halfSize;
		int YStart = startPos;
		int UStart = YStart + YLength;
		int VStart = UStart + UVLength;
		
		int YIndex = convertLengthToIndex(YLength);
		int UIndex = convertLengthToIndex(UVLength);
		int VIndex = convertLengthToIndex(UVLength);
		double[] YBytes = this.fileDataCache[YIndex][0];
		double[] UBytes = this.fileDataCache[UIndex][0];
		double[] VBytes = this.fileDataCache[VIndex][1];
		
		for (int n = 0; n < YLength; n++) {
			YBytes[n] = Protocol.getDCTCoeff(vectorPart[YStart + n]);
		}

		for (int n = 0; n < UVLength; n++) {
			UBytes[n] = Protocol.getDCTCoeff(vectorPart[UStart + n]);
			VBytes[n] = Protocol.getDCTCoeff(vectorPart[VStart + n]);
		}

		return new double[][] {YBytes, UBytes, VBytes};
	}
}
