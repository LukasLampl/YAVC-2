/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2025  Lukas Nian En Lampl, Hans Lampl

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

package app.engines.prediction.interprediction.decoding;

import java.util.List;
import java.util.concurrent.RecursiveAction;

import app.Main;
import app.engines.prediction.interprediction.DecodingVector;
import app.io.Protocol;
import app.io.ProtocolBase;
import app.managers.ListManager;

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
	private ListManager<DecodingVector> vectorManager = null;
	
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
	public VectorConversionTask(int start, int end, List<Integer> indexes, byte[] data,
			ListManager<DecodingVector> vectorManager) {
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
		final int totalWorkload = getWorkloadOfThread();
		
		if (totalWorkload > MAX_WORK && !this.executeOnSingleThread) {
			final int middle = (this.start + this.end) / 2;
			final VectorConversionTask tl = new VectorConversionTask(this.start, middle,
					this.indexes, this.data, this.vectorManager);
			final VectorConversionTask tr = new VectorConversionTask(middle, this.end,
					this.indexes, this.data, this.vectorManager);
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
				totalWorkload = this.indexes.get(i);
				continue;
			}
			
			totalWorkload += (this.indexes.get(i) - this.indexes.get(i - 1));
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
			final int posX = ProtocolBase.getPosition(this.data[index], this.data[index + 1]);
			final int posY = ProtocolBase.getPosition(this.data[index + 2], this.data[index + 3]);
			final int spanX = Protocol.getVectorSpanInt(this.data[index + 4]);
			final int spanY = Protocol.getVectorSpanInt(this.data[index + 5]);
			final int[] refAndSize = Protocol.getReferenceAndSizeInt(this.data[index + 6]);
			final int ref = refAndSize[0];
			final int size = refAndSize[1];
			DecodingVector vec = this.vectorManager.getCachedObj();
			
			if (vec == null) {
				vec = new DecodingVector(posX, posY, size);
			} else {
				vec.setSize(size);
				vec.move(posX, posY);
			}
			
			double[][][] diffs = ProtocolBase.getDeltaCoefficientsFromDatastream(this.data,
					index + Protocol.VECTOR_HEADER_LENGTH, size);
			diffs = Main.DCT_ENGINE.computeIDCTOfDeltas(diffs, size, true, true);
			vec.setYUVDelta(diffs);
			vec.setSpanX(spanX);
			vec.setSpanY(spanY);
			vec.setReference(ref);
			this.vectorManager.add(vec);
		}
	}
}
