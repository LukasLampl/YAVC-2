package YAVC.Main;

public class config {
	public static final int CODING_OFFSET = 15;
	public static final int UTF_8_CODING_SIZE = 8;
	public static final int MAX_REFERENCES = 4;
	
	public static final byte VECTOR_START = (char)1;
	public static final char DCT_BLOCK_SIZE = (char)2;
	public static final char DCT_BLOCK_END = (char)3;
	public static final char MACRO_BLOCK_START = (char)4;
	public static final byte VECTOR_END = (char)5;
	
	// DCT - ONLY! //
	public static final int[][] QUANTIZATION_MATRIX_8x8_Luma = {
		{16, 11, 10, 16, 24, 40, 51, 61},
		{12, 12, 14, 19, 26, 58, 60, 55},
		{14, 13, 16, 24, 40, 57, 69, 56},
		{14, 17, 22, 29, 51, 87, 80, 62},
		{18, 22, 37, 56, 68, 109, 103, 77},
		{24, 35, 55, 64, 81, 104, 113, 92},
		{49, 64, 78, 87, 103, 121, 120, 101},
		{72, 92, 95, 98, 112, 100, 103, 99}
	};

	public static final int[][] QUANTIZATION_MATRIX_4x4_Luma = {
		{8, 12, 24, 32},
		{5, 14, 25, 34},
		{16, 23, 24, 43},
		{21, 24, 26, 48}
	};
	
	public static final int[][] QUANTIZATION_MATRIX_4x4_Chroma = {
		{8, 12, 26, 48},
		{13, 15, 27, 48},
		{24, 23, 22, 48},
		{48, 48, 48, 48}
	};
	
	public static final int[][] QUANTIZATION_MATRIX_2x2_Chroma = {
		{4, 15},
		{17, 42}
	};
	
////	public static final int[][] QUANTIZATION_MATRIX_8x8_Luma = {
////			{32, 1, 1, 1, 1, 1, 1, 1},
////			{1, 1, 1, 1, 1, 1, 1, 1},
////			{1, 1, 1, 1, 1, 1, 1, 1},
////			{1, 1, 1, 1, 1, 1, 1, 1},
////			{1, 1, 1, 1, 1, 1, 1, 1},
////			{1, 1, 1, 1, 1, 1, 1, 1},
////			{1, 1, 1, 1, 1, 1, 1, 1},
////			{1, 1, 1, 1, 1, 1, 1, 1}
////		};
//		
//		public static final int[][] QUANTIZATION_MATRIX_4x4_Luma = {
//			{16, 1, 1, 1},
//			{1, 1, 1, 1},
//			{1, 1, 1, 1},
//			{1, 1, 1, 1}
//		};
//		
//		public static final int[][] QUANTIZATION_MATRIX_4x4_Chroma = {
//			{16, 1, 1, 1},
//			{1, 1, 1, 1},
//			{1, 1, 1, 1},
//			{1, 1, 1, 1}
//		};
//		
//		public static final int[][] QUANTIZATION_MATRIX_2x2_Chroma = {
//			{4, 1},
//			{1, 1}
//		};
}
