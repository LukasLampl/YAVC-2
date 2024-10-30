package app.interprediction;

import app.encoder.LoadDistributor;
import app.utils.MacroBlock;

public class VectorEngineResult {
	private LoadDistributor<MacroBlock> restBlocks;
	private LoadDistributor<Vector> vectors;
	
	public VectorEngineResult(LoadDistributor<MacroBlock> restBlocks, LoadDistributor<Vector> vectors) {
		this.restBlocks = restBlocks;
		this.vectors = vectors;
	}

	/**
	 * @return the restBlocks
	 */
	public LoadDistributor<MacroBlock> getRestBlocks() {
		return restBlocks;
	}

	/**
	 * @param restBlocks the restBlocks to set
	 */
	public void setRestBlocks(LoadDistributor<MacroBlock> restBlocks) {
		this.restBlocks = restBlocks;
	}

	/**
	 * @return the vectors
	 */
	public LoadDistributor<Vector> getVectors() {
		return vectors;
	}

	/**
	 * @param vectors the vectors to set
	 */
	public void setVectors(LoadDistributor<Vector> vectors) {
		this.vectors = vectors;
	}
}
