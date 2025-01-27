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

package app.engines.prediction.interprediction;

import app.managers.LoadDistributor;
import app.utils.components.MacroBlock;

/**
 * The {@code VectorEngineResult} class is a wrapper class to group
 * the results of the interprediction step together and return them
 * as a result.
 * 
 * @author Lukas Lampl
 * @since 1.4.1 [Optimized prototype]
 */
public class VectorEngineResult {
	/**
	 * Holds the rest blocks after the interprediction.
	 */
	private LoadDistributor<MacroBlock> restBlocks;
	
	/**
	 * Holds all vectors after the interprediction.
	 */
	private LoadDistributor<Vector> vectors;
	
	/**
	 * A new Instance of the {@code VectorEngineResult} which holds
	 * the given data.
	 * 
	 * @param restBlocks	Remaining non-coded blocks after the interprediction.
	 * @param vectors		All coded vectors.
	 */
	public VectorEngineResult(LoadDistributor<MacroBlock> restBlocks, LoadDistributor<Vector> vectors) {
		this.restBlocks = restBlocks;
		this.vectors = vectors;
	}

	/**
	 * @return The restBlocks.
	 */
	public LoadDistributor<MacroBlock> getRestBlocks() {
		return restBlocks;
	}

	/**
	 * @param restBlocks	The restBlocks to set.
	 */
	public void setRestBlocks(LoadDistributor<MacroBlock> restBlocks) {
		this.restBlocks = restBlocks;
	}

	/**
	 * @return The vectors.
	 */
	public LoadDistributor<Vector> getVectors() {
		return vectors;
	}

	/**
	 * @param vectors	The vectors to set.
	 */
	public void setVectors(LoadDistributor<Vector> vectors) {
		this.vectors = vectors;
	}
}
