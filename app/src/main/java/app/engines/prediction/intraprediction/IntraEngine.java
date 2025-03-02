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

package app.engines.prediction.intraprediction;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import app.engines.prediction.intraprediction.encoding.IntraPipeline;
import app.engines.prediction.intraprediction.encoding.IntraPredictionTask;
import app.managers.LoadDistributor;
import app.utils.PixelRaster;
import app.utils.components.MacroBlock;

public class IntraEngine extends IntraPipeline {
	/**
	 * Counter on how many pixels were processed.
	 */
	private int totalPixelsProcessed = 0;

	public LoadDistributor<EncodingIntraPredictionBlock> computeIntraPrediction(final List<MacroBlock> predictionList,
			final PixelRaster curFrame) {
		this.totalPixelsProcessed = 0;
		
		for (final MacroBlock b : predictionList) {
			this.totalPixelsProcessed += b.getArea();
		}
		
		LoadDistributor<EncodingIntraPredictionBlock> predictedBlocks = new LoadDistributor<EncodingIntraPredictionBlock>();
		ForkJoinPool executor = ForkJoinPool.commonPool();
		executor.invoke(new IntraPredictionTask(predictedBlocks, predictionList, 0, predictionList.size(), curFrame));
		executor.shutdown();
		executor.close();
		
		predictedBlocks.compute(this.totalPixelsProcessed);
		return predictedBlocks;
	}
}
