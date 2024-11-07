package app.decoder;

import java.awt.Point;
import java.util.ArrayList;

import app.encoder.LoadDistributor;
import app.interprediction.Vector;
import app.utils.Protocol;

public class VectorConverter {
	private final int numOfThreads = Runtime.getRuntime().availableProcessors();
	private ConversionThread[] threads = new ConversionThread[this.numOfThreads];
	private ArrayList<Vector> vectors = new ArrayList<Vector>();
	private LoadDistributor<Integer> dist = null;
	
	private int currentLoadIndex = 0;
	private byte[] data = null;
	
	public VectorConverter(byte[] data, ArrayList<Integer> indexes) {
		this.data = data;
		this.dist = new LoadDistributor<Integer>(this.numOfThreads * 4);
		this.dist.setAll(indexes);
		this.dist.compute(indexes.size());
		
		for (int i = 0; i < this.numOfThreads; i++) {
			this.threads[i] = new ConversionThread();
			this.threads[i].setName("Vector-converter-thread_#" + i);
		}
	}
	
	private ArrayList<Integer> getLoad() {
		synchronized (this.dist) {
			if (this.currentLoadIndex >= this.dist.getNumberOfChunks()) {
				return null;
			}
			
			return this.dist.getLoadOf(this.currentLoadIndex++);
		}
	}
	
	public void start() {
		for (ConversionThread t : this.threads) {
			t.start();
		}
	}
	
	public ArrayList<Vector> awaitTermination() {
		for (ConversionThread t : this.threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		for (ConversionThread t : this.threads) {
			this.vectors.addAll(t.getResult());
		}
		
		return this.vectors;
	}
	
	private class ConversionThread extends Thread {
		private ArrayList<Vector> tempList = new ArrayList<Vector>();
		private double[][][] fileDataCache = new double[7][][]; //First dim for index, second for optional two channels, thrid the actual cache
		
		public ConversionThread() {
			initFileDataCache();
		}
		
		private void initFileDataCache() {
			int[] sizes = {2 * 2, 4 * 4, 8 * 8, 16 * 16, 32 * 32, 64 * 64, 128 * 128};
			
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
		
		@Override
		public void run() {
			long start = System.currentTimeMillis();
			ArrayList<Integer> load = null;
			
			while ((load = getLoad()) != null) {
				for (Integer rawIndex : load) {
					int index = rawIndex.intValue();
					int posX = Protocol.getPosition(data[index], data[index + 1]);
					int posY = Protocol.getPosition(data[index + 2], data[index + 3]);
					int spanX = Protocol.getVectorSpanInt(data[index + 4]);
					int spanY = Protocol.getVectorSpanInt(data[index + 5]);
					int[] refAndSize = Protocol.getReferenceAndSizeInt(data[index + 6]);
					int ref = refAndSize[0];
					int size = refAndSize[1];

					ArrayList<double[][][]> diffs = getVectorDifferences(data, Protocol.VECTOR_HEADER_LENGTH + index, size);
					
					Vector vec = new Vector(new Point(posX, posY), size);
					vec.setAbsolutedifferenceDCTCoefficients(diffs);
					vec.setSpanX(spanX);
					vec.setSpanY(spanY);
					vec.setReference(ref);
					this.tempList.add(vec);
				}
			}
			
			long end = System.currentTimeMillis();
			System.out.println("   >>> Time of Thread: " + (end - start) + "ms");
		}
		
		public ArrayList<Vector> getResult() {
			return this.tempList;
		}
		
		
		private ArrayList<double[][][]> getVectorDifferences(byte[] vectorPart, int startPos, int size) {
			ArrayList<double[][][]> DCTCoeffGroups = new ArrayList<double[][][]>();
			double[][] data = getDCTCoeffsOutOfFile(vectorPart, startPos, size);
			int YLength = size * size;
			
			if (size == 4) {
				double[][][] res = new double[3][][];
				res[0] = new double[4][4];
				res[1] = new double[2][2];
				res[2] = new double[2][2];
				
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
				int halfSize = size / 2;
				
				for (int u = 0; u < YLength; u += 64) {
					double[][][] res = new double[3][][];
					res[0] = new double[size][size];
					res[1] = new double[halfSize][halfSize];
					res[2] = new double[halfSize][halfSize];
					
					for (int x = 0, i = 0; x < 8; x++) {
						for (int y = 0; y < 8; y++) {
							res[0][x][y] = data[0][u + i++];
						}
					}
					
					for (int x = 0, i = 0; x < 4; x++) {
						for (int y = 0; y < 4; y++) {
							res[1][x][y] = data[1][(u / 4) + i];
							res[2][x][y] = data[2][(u / 4) + i++];
						}
					}
					
					DCTCoeffGroups.add(res);
				}
			}
			
			return DCTCoeffGroups;
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
}
