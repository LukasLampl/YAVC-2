package app.io.coder.cabac;

public class ContextModelManager {
	public static enum CodingType {
		VECTOR_SPAN_X(0),
		VECTOR_SPAN_Y(1),
		VECTOR_REFERENCE(2),
		
		INTRA_BORDER_HORIZONTAL(3),
		INTRA_BORDER_VERTICAL(4),
		INTRA_PREDICTION_ANGLE(5),
		
		RESIDUALS_Y(6),
		RESIDUALS_U(7),
		RESIDUALS_V(8),
		
		QUADTREE_POSITION_X(9),
		QUADTREE_POSITION_Y(10),
		QUADTREE_SUBDIVISION(11),
		
		PREDICTION_TYPE(12);
		
		private int index = 0;
		
		CodingType(final int index) {
			this.index = index;
		}
		
		public int getIndex() {
			return this.index;
		}
	}
	
	/**
	 * Interprediction:
	 * - Span X
	 * - Span Y
	 * - Reference frame
	 * 
	 * Intraprediction:
	 * - Border pixels horizontal
	 * - Border pixels vertical
	 * - Prediction angle
	 * 
	 * General:
	 * - Residuals
	 */
	public final static int NUMBER_OF_MODELS = CodingType.values().length;
	private final BinaryContextModel[] models = new BinaryContextModel[NUMBER_OF_MODELS];
	
	public ContextModelManager() {
		for (int i = 0; i < NUMBER_OF_MODELS; i++) {
			this.models[i] = new BinaryContextModel();
		}
	}
	
	public BinaryContextModel getModel(final CodingType codingType) {
		return this.models[codingType.getIndex()];
	}
	
	public void resetModels() {
		for (final BinaryContextModel model : this.models) {
			model.reset();
		}
	}
}
