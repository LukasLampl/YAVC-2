package Encoder;

import java.awt.Point;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Utils.PixelRaster;
import Utils.Vector;

public class DCTEngine {
	private double step(int x, int m) {
		return x == 0 ? 1 / Math.sqrt(m) : Math.sqrt(2.0 / (double)m);
	}
	
	public void applyDCTOnPixelRaster(PixelRaster raster) {
		int size = 8;
		
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		int width = raster.getWidth(), height = raster.getHeight();
		
		for (int x = 0; x < width; x += size) {
			for (int y = 0; y < height; y += size) {
				final int posX = x, posY = y;
				
				Callable<Void> task = () -> {
					double[][][] comps = raster.getPixelBlock(new Point(posX, posY), size, null);
					double[][][] ChromaDCT = computeChromaDCTCoefficients(comps[1], comps[2], size / 2);
					double[][] LumaDCT = computeLumaDCTCoefficients(comps[0], size);
					ChromaDCT = computeChromaIDCTCoefficients(ChromaDCT[0], ChromaDCT[1], size / 2);
					LumaDCT = computeLumaIDCTCoefficients(LumaDCT, size);
					
					for (int u = 0; u < size; u++) {
						for (int v = 0; v < size; v++) {
							raster.setLuma(posX + u, posY + v, LumaDCT[u][v]);
						}
					}
					
					for (int u = 0; u < size / 2; u++) {
						for (int v = 0; v < size / 2; v++) {
							raster.setChroma((u * 2) + posX, (v * 2) + posY, ChromaDCT[0][u][v], ChromaDCT[1][u][v]);
						}
					}
					
					return null;
				};
				
				executor.submit(task);
			}
		}
		
		executor.shutdown();
	}
	
	public double[][][] computeDCTOfVectorColorDifference(Vector vector) {
		double[][][] diffs = vector.getAbsoluteColorDifference();
		double[][][] chromaDCT = computeChromaDCTCoefficients(diffs[1], diffs[2], vector.getSize() / 2);
		double[][] lumaDCT = computeLumaDCTCoefficients(diffs[0], vector.getSize());
		return new double[][][] {lumaDCT, chromaDCT[0], chromaDCT[1]};
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
		
	private double[][] computeLumaDCTCoefficients(double[][] Y, int m) {
		double resY[][] = new double[m][m];
		
		for (int v = 0; v < m; v++) {
            for (int u = 0; u < m; u++) {
            	double sum = 0;
            	
                for (int x = 0; x < m; x++) {
                	double cos1 = Math.cos(((double)(2 * x + 1) * (double)v * Math.PI) / (double)(2 * m));
                	
                    for (int y = 0; y < m; y++) {
                        double cos2 = Math.cos(((double)(2 * y + 1) * (double)u * Math.PI) / (double)(2 * m)); 
                        sum += (Y[x][y] - 128) * cos1 * cos2;
                    }
                }
                
                resY[v][u] = Math.round(step(v, m) * step(u, m) * sum);
            }
        }
		
		return resY;
	}
		
	private double[][] computeLumaIDCTCoefficients(double[][] Y, int m) {
		double[][] resY = new double[m][m];
		double[][] resV = new double[m][m];
		
		for (int x = 0; x < m; x++) {
			for (int y = 0; y < m; y++) {
				double sum = 0;
				
				for (int u = 0; u < m; u++) {
					double cos1 = Math.cos(((double)(2 * x + 1) * (double)u * Math.PI) / (double)(2 * m));
					
					for (int v = 0; v < m; v++) {
						double step = step(u, m) * step(v, m);
                        double cos2 = Math.cos(((double)(2 * y + 1) * (double)v * Math.PI) / (double)(2 * m)); 
                        sum += Y[u][v] * step * cos1 * cos2;
					}
				}
				
				resY[x][y] = sum + 128;
			}
		}
		
		return resY;
	}
}
