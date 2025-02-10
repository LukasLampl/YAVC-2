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

package app.io.containers;

import java.util.ArrayList;

import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.managers.Discardable;
import app.managers.LoadDistributor;

/**
 * A container object for vectors and non-coded blocks that should be
 * written into an YAVC file.
 * 
 * @author Lukas Lampl
 * @since 1.3
 */
public class QueueObject implements Discardable {
	/**
	 * The vectors to write.
	 */
	private ArrayList<EncodingVector> vectors = new ArrayList<EncodingVector>();
	
	/**
	 * The intra blocks to write.
	 */
	private ArrayList<EncodingIntraPredictionBlock> intraBlocks = new ArrayList<EncodingIntraPredictionBlock>();
	
	/**
	 * Creates an QueueObject container with the given vectors and non-coded blocks
	 * to write.
	 * 
	 * @param vecManager	Vectors to write.
	 * @param diffManager	Non-coded blocks to write.
	 */
	public QueueObject(LoadDistributor<EncodingVector> vecManager, LoadDistributor<EncodingIntraPredictionBlock> intraBlocks) {
		if (vecManager != null) {
			this.vectors.addAll(vecManager.getRawData());
		}
		
		if (intraBlocks != null) {
			this.intraBlocks.addAll(intraBlocks.getRawData());
		}
	}
	
	/**
	 * Gets the vectors to write.
	 * 
	 * @return The vectors to write.
	 */
	public ArrayList<EncodingVector> getVectors() {
		return this.vectors;
	}
	
	/**
	 * Gets the intra blocks to write.
	 * 
	 * @return The intra blocks to write.
	 */
	public ArrayList<EncodingIntraPredictionBlock> getIntraBlocks() {
		return this.intraBlocks;
	}
	
	@Override
	public void discard() {
		this.vectors.clear();
		this.intraBlocks.clear();
	}
}
