package app.interprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import app.io.Protocol;
import app.utils.ListManager;

public class VectorConversionTask extends RecursiveTask<Void> {
	private static final long serialVersionUID = -1416920943935831433L;
	private static final int MAX_WORK = 256 * 256;
	private int start = 0;
	private int end = 0;
	private double[][][] fileDataCache = null;
	
	private List<Integer> indexes = null;
	private byte[] data = null;
	private ListManager<Vector> vectorManager = null;
	
	public VectorConversionTask(int start, int end, List<Integer> indexes, byte[] data, ListManager<Vector> vectorManager) {
		this.start = start;
		this.end = end;
		this.indexes = indexes;
		this.data = data;
		this.vectorManager = vectorManager;
	}
	
	@Override
	protected Void compute() {
		int totalWorkload = getWorkloadOfThread();
		
		if (totalWorkload > MAX_WORK) {
			int middle = (this.start + this.end) / 2;
			VectorConversionTask tl = new VectorConversionTask(this.start, middle, this.indexes, this.data, this.vectorManager);
			VectorConversionTask tr = new VectorConversionTask(middle, this.end, this.indexes, this.data, this.vectorManager);
			invokeAll(tl, tr);
		} else {
			initFileDataCache();
			execute();
		}
		
		return null;
	}
	
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
	
	private void initFileDataCache() {
		//int[] sizes = {2 * 2, 4 * 4, 8 * 8, 16 * 16, 32 * 32, 64 * 64, 128 * 128};
		int[] sizes = {4, 16, 64, 256, 1024, 4096, 16384};
		this.fileDataCache = new double[7][][]; //First dim for index, second for optional two channels, thrid the actual cache
		
		for (int i = 0; i < sizes.length; i++) {
			int index = convertLengthToIndex(sizes[i]);
			this.fileDataCache[index] = new double[2][]; //For two channels (U and V)
			this.fileDataCache[index][0] = new double[sizes[i]];
			this.fileDataCache[index][1] = new double[sizes[i]];
		}
	}
	
	private int convertLengthToIndex(int length) {
		switch (length) {
		case 4: //2 * 2
			return 0;
		case 16: //4 * 4
			return 1;
		case 64: //8 * 8
			return 2;
		case 256: //16 * 16
			return 3;
		case 1024: //32 * 32
			return 4;
		case 4096: //64 * 64
			return 5;
		case 16384: //128 * 128
			return 6;
		default:
			throw new IllegalArgumentException("The length " + length + " cannot be converted to an index.");
		}
	}
	
	public void execute() {
		for (int i = this.start; i < this.end; i++) {
			int index = indexes.get(i).intValue();
			int posX = Protocol.getPosition(data[index], data[index + 1]);
			int posY = Protocol.getPosition(data[index + 2], data[index + 3]);
			int spanX = Protocol.getVectorSpanInt(data[index + 4]);
			int spanY = Protocol.getVectorSpanInt(data[index + 5]);
			int[] refAndSize = Protocol.getReferenceAndSizeInt(data[index + 6]);
			int ref = refAndSize[0];
			int size = refAndSize[1];
			Vector vec = vectorManager.getCachedObj();
			
			if (vec == null) {
				vec = new Vector(posX, posY, size);
			}
			
			vec.setSize(size);
			vec.setPosition(posX, posY);
			
			ArrayList<double[][][]> diffs = getVectorDifferences(data, Protocol.VECTOR_HEADER_LENGTH + index, size, vec);
			vec.setAbsolutedifferenceDCTCoefficients(diffs);
			vec.setSpanX(spanX);
			vec.setSpanY(spanY);
			vec.setReference(ref);
			vectorManager.add(vec);
		}
	}

	private ArrayList<double[][][]> getVectorDifferences(byte[] vectorPart, int startPos, int size, Vector cachedVector) {
		ArrayList<double[][][]> cachedGroups = cachedVector.getDCTCoefficientsOfAbsoluteColorDifference();
		ArrayList<double[][][]> DCTCoeffGroups = new ArrayList<double[][][]>();
		double[][] data = getDCTCoeffsOutOfFile(vectorPart, startPos, size);
		int YLength = size * size;
		
		if (size == 4) {
			double[][][] res = cachedGroups == null ? getArray(4) : cachedGroups.size() > 0 ? cachedGroups.remove(0) : getArray(4);
			
			for (int x = 0, i = 0; x < 4; x++) {
				for (int y = 0; y < 4; y++) {
					res[0][x][y] = data[0][i++];
				}
			}

			for (int x = 0, i = 0; x < 2; x++) {
				for (int y = 0; y < 2; y++) {
					res[1][x][y] = data[1][i];
					res[2][x][y] = data[2][i++];
				}
			}
			
			DCTCoeffGroups.add(res);
		} else {
			boolean wasCachedGroup4x4Block = cachedGroups == null ? true : cachedGroups.size() == 1;
			
			for (int u = 0; u < YLength; u += 64) {
				int uFrac = (u / 4);
				double[][][] res;
				
				if (cachedGroups == null) {
					res = getArray(8);
				} else {
					if (cachedGroups.isEmpty()) {
						res = getArray(8);
					} else if (wasCachedGroup4x4Block) {
						res = getArray(8);
					} else {
						res = cachedGroups.remove(0);
					}
				}
				
				for (int x = 0, i = 0; x < 8; x++) {
					for (int y = 0; y < 8; y++) {
						res[0][x][y] = data[0][u + i++];
					}
				}
				
				for (int x = 0, i = 0; x < 4; x++) {
					for (int y = 0; y < 4; y++) {
						res[1][x][y] = data[1][uFrac + i];
						res[2][x][y] = data[2][uFrac + i++];
					}
				}
				
				DCTCoeffGroups.add(res);
			}
		}
		
		return DCTCoeffGroups;
	}
	
	private double[][][] getArray(int size) {
		int halfSize = size / 2;
		double[][][] arr = new double[3][][];
		arr[0] = new double[size][size];
		arr[1] = new double[halfSize][halfSize];
		arr[2] = new double[halfSize][halfSize];
		return arr;
	}
	
	private double[][] getDCTCoeffsOutOfFile(byte[] vectorPart, int startPos, int size) {
		int halfSize = size / 2;
		int YLength = size * size;
		int UVLength = halfSize * halfSize;
		int YStart = startPos;
		int UStart = YStart + YLength;
		int VStart = UStart + UVLength;
		
		int YIndex = convertLengthToIndex(YLength);
		int UIndex = convertLengthToIndex(UVLength);
		int VIndex = convertLengthToIndex(UVLength);
		double[] YBytes = this.fileDataCache[YIndex][0];
		double[] UBytes = this.fileDataCache[UIndex][0];
		double[] VBytes = this.fileDataCache[VIndex][1];
		
		for (int n = 0; n < YLength; n++) {
			YBytes[n] = Protocol.getDCTCoeff(vectorPart[YStart + n]);
		}

		for (int n = 0; n < UVLength; n++) {
			UBytes[n] = Protocol.getDCTCoeff(vectorPart[UStart + n]);
			VBytes[n] = Protocol.getDCTCoeff(vectorPart[VStart + n]);
		}

		return new double[][] {YBytes, UBytes, VBytes};
	}
}
