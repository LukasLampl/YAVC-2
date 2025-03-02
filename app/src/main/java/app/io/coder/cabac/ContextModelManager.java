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

package app.io.coder.cabac;

/**
 * The {@code ContextModelManager} class is responsible
 * for providing all available context model to the CABAC coder
 * with their current state.
 * 
 * @author Lukas Lampl
 * @since 2.1.1 [QT_COMP]
 */
public class ContextModelManager {
	public static enum ResidualType {
		RESIDUAL_Y,
		RESIDUAL_U,
		RESIDUAL_V
	}
	
	public static enum CodingType {
		VECTOR_SPAN_X(0),
		VECTOR_SPAN_Y(1),
		VECTOR_REFERENCE(2),
		
		INTRA_BORDER_HORIZONTAL(3),
		INTRA_BORDER_VERTICAL(4),
		INTRA_PREDICTION_ANGLE(5),
		
		RESIDUALS_Y_FIRST_QUARTER(6),
		RESIDUALS_Y_SECOND_QUARTER(7),
		RESIDUALS_Y_THIRD_QUARTER(8),
		RESIDUALS_Y_FOURTH_QUARTER(9),
		
		RESIDUALS_U_FIRST_QUARTER(10),
		RESIDUALS_U_SECOND_QUARTER(11),
		RESIDUALS_U_THIRD_QUARTER(12),
		RESIDUALS_U_FOURTH_QUARTER(13),
		
		RESIDUALS_V_FIRST_QUARTER(14),
		RESIDUALS_V_SECOND_QUARTER(15),
		RESIDUALS_V_THIRD_QUARTER(16),
		RESIDUALS_V_FOURTH_QUARTER(17),
		
		QUADTREE_POSITION_X(18),
		QUADTREE_POSITION_Y(19),
		QUADTREE_SUBDIVISION(20),
		NUMBER_OF_QUADTREES(21),
		
		PREDICTION_TYPE(22);
		
		private int index = 0;
		
		CodingType(final int index) {
			this.index = index;
		}
		
		public int getIndex() {
			return this.index;
		}
	}
	
	/**
	 * Number of available context models.
	 */
	public final static int NUMBER_OF_MODELS = CodingType.values().length;
	
	/**
	 * Holds all available context models used for CABAC coding.
	 */
	private final BinaryContextModel[] models = new BinaryContextModel[NUMBER_OF_MODELS];
	
	/**
	 * Initializes all available coding model.
	 */
	public ContextModelManager() {
		for (int i = 0; i < NUMBER_OF_MODELS; i++) {
			this.models[i] = new BinaryContextModel();
		}
	}
	
	/**
	 * Gets a specific context model based on the given {@code CodingType}.
	 * 
	 * @param codingType	Coding type from which to get the model from.
	 * @return The according context model.
	 */
	public BinaryContextModel getModel(final CodingType codingType) {
		return this.models[codingType.getIndex()];
	}
	
	/**
	 * Resets all context models to their initial state.
	 */
	public void resetModels() {
		for (final BinaryContextModel model : this.models) {
			model.reset();
		}
	}
}
