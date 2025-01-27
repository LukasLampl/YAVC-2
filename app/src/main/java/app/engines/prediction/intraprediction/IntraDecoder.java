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

import java.awt.Dimension;

import app.utils.components.MacroBlock;

public class IntraDecoder {
	public static void computeAngularIntraPredictionBlock(MacroBlock predictionBlock, final double[][] verticalYUV,
			final double[][] horizontalYUV, final int angle, final Dimension dim) {
		IntraPipeline.computeAngularIntraPredictionBlock(predictionBlock, verticalYUV, horizontalYUV, angle, dim);
	}
}
