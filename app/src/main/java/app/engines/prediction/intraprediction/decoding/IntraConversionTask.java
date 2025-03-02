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

package app.engines.prediction.intraprediction.decoding;

import java.util.List;
import java.util.concurrent.RecursiveAction;

import app.Main;
import app.engines.prediction.intraprediction.DecodingIntraPredictionBlock;
import app.io.Protocol;
import app.io.ProtocolBase;
import app.managers.ListManager;
import app.rendering.ColorManager;

public class IntraConversionTask extends RecursiveAction {
	private static final long serialVersionUID = -9153076396071631916L;
	
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
	private ListManager<DecodingIntraPredictionBlock> intraBlocksManager = null;
	
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
	public IntraConversionTask(int start, int end, List<Integer> indexes, byte[] data,
			ListManager<DecodingIntraPredictionBlock> intraBlocksManager) {
		this.start = start;
		this.end = end;
		this.indexes = indexes;
		this.data = data;
		this.intraBlocksManager = intraBlocksManager;
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
			IntraConversionTask tl = new IntraConversionTask(this.start, middle,
					this.indexes, this.data, this.intraBlocksManager);
			IntraConversionTask tr = new IntraConversionTask(middle, this.end,
					this.indexes, this.data, this.intraBlocksManager);
			invokeAll(tl, tr);
		} else {
			execute();
		}
	}
	
	/**
	 * Calculates the current workload of the RecursiveTask
	 * by adding the sizes of the intra blocks and returning it.
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
	 * and end down. The function essentially creates the intra block based on the
	 * {@link #data} and {@link #indexes}, in the end it adds the intra block to
	 * the {@link #intraBlocksManager}
	 */
	public void execute() {
		for (int i = this.start; i < this.end; i++) {
			final int index = indexes.get(i).intValue();
			final int posX = ProtocolBase.getPosition(this.data[index], this.data[index + 1]);
			final int posY = ProtocolBase.getPosition(this.data[index + 2], this.data[index + 3]);
			final int[] sizeAndAngle = Protocol.getSizeAndAngle(this.data[index + 4], this.data[index + 5]);
			final int size = sizeAndAngle[0];
			final int angle = sizeAndAngle[1];
			final double[][][] borderColors = Protocol.getBorderColors(this.data, size, index + 6);
			final int borderOffset = (size * 2) * ColorManager.CHANNELS;
			DecodingIntraPredictionBlock intraBlock = this.intraBlocksManager.getCachedObj();
			
			if (intraBlock == null) {
				intraBlock = new DecodingIntraPredictionBlock(posX, posY, angle, size);
			} else {
				intraBlock.setSize(size);
				intraBlock.move(posX, posY);
			}
			
			double[][][] diffs = ProtocolBase.getDeltaCoefficientsFromDatastream(this.data,
					index + Protocol.INTRA_BLOCK_HEADER_LENGTH + borderOffset, size);
			diffs = Main.DCT_ENGINE.computeIDCTOfDeltas(diffs, size, true, true);
			double[][][] yuvDelta = diffs;
			intraBlock.setYUVDelta(yuvDelta);
			intraBlock.setAngle(angle);
			intraBlock.setVertical(borderColors[0]);
			intraBlock.setHorizontal(borderColors[1]);
			this.intraBlocksManager.add(intraBlock);
		}
	}
}
