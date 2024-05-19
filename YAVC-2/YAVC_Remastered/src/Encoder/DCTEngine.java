package Encoder;

import java.awt.Point;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Utils.PixelRaster;

public class DCTEngine {
	private double step(int x, int m) {
		return x == 0 ? 1 / Math.sqrt(m) : Math.sqrt(2.0 / (double)m);
	}
	
	public void applyDCTOnPixelRaster(PixelRaster raster) {
		int size = 8;
		
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		for (int x = 0; x < raster.getWidth(); x += size) {
			for (int y = 0; y < raster.getHeight(); y += size) {
				final int posX = x, posY = y;
				
				Callable<Void> task = () -> {
					double[][][] comps = raster.getPixelBlock(new Point(posX, posY), size, null);
					double[][][] DCT = computeChromaDCTCoefficients(comps[1], comps[2], (int)(size * 0.5));
					DCT = computeChromaIDCTCoefficients(DCT[0], DCT[1], size / 2);
					
					for (int u = 0; u < size / 2; u++) {
						for (int v = 0; v < size / 2; v++) {
							raster.setChroma((u * 2) + posX, (v * 2) + posY, DCT[0][u][v], DCT[1][u][v]);
						}
					}
					
					return null;
				};
				
				executor.submit(task);
			}
		}
		
		executor.shutdown();
	}
	
	private double[][][] computeChromaDCTCoefficients(double[][] U, double[][] V, int m) {
		double resU[][] = new double[m][m];
		double resV[][] = new double[m][m];
		
		for (int v = 0; v < m; v++) {
            for (int u = 0; u < m; u++) {
            	double sumU = 0, sumV = 0;
            	
                for (int x = 0; x < m; x++) {
                	double cos1 = Math.cos(((double)(2 * x + 1) * (double)v * Math.PI) / (double)(2 * m));
                	
                    for (int y = 0; y < m; y++) {
                        double cos2 = Math.cos(((double)(2 * y + 1) * (double)u * Math.PI) / (double)(2 * m)); 
                        sumU += (U[x][y] - 128) * cos1 * cos2;
                        sumV += (V[x][y] - 128) * cos1 * cos2;
                    }
                }
                
                double step = step(v, m) * step(u, m);
                resU[v][u] = Math.round(step * sumU);
                resV[v][u] = Math.round(step * sumV);
            }
        }
		
		return new double[][][] {resU, resV};
	}
	
	private double[][][] computeChromaIDCTCoefficients(double[][] U, double[][] V, int m) {
		double[][] resU = new double[m][m];
		double[][] resV = new double[m][m];
		
		for (int x = 0; x < m; x++) {
			for (int y = 0; y < m; y++) {
				double sumU = 0, sumV = 0;
				
				for (int u = 0; u < m; u++) {
					double cos1 = Math.cos(((double)(2 * x + 1) * (double)u * Math.PI) / (double)(2 * m));
					
					for (int v = 0; v < m; v++) {
						double step = step(u, m) * step(v, m);
                        double cos2 = Math.cos(((double)(2 * y + 1) * (double)v * Math.PI) / (double)(2 * m)); 
                        sumU += U[u][v] * step * cos1 * cos2;
                        sumV += V[u][v] * step * cos1 * cos2;
					}
				}
				
				resU[x][y] = sumU + 128;
				resV[x][y] = sumV + 128;
			}
		}
		
		return new double[][][] {resU, resV};
	}
}
