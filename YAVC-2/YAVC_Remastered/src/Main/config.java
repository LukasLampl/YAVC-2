package Main;

public class config {
	public static final int CODING_OFFSET = 20;
	public static final int UTF_8_CODING_SIZE = 8;
	public static final int MAX_REFERENCES = 4;
	public static final int CODED_VECTOR_LENGTH = 4;
	
	public static final byte VECTOR_START = (char)1;
	public static final char Y_DCT_END = (char)2;
	public static final char U_DCT_END = (char)3;
	public static final char V_DCT_END = (char)4;
	public static final char DCT_BLOCK_SIZE = (char)5;
	public static final char DCT_BLOCK_END = (char)6;
	public static final char MACRO_BLOCK_START = (char)7;
	public static final byte VECTOR_END = (char)8;
}
