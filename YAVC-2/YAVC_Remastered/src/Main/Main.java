/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

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

package Main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

import Encoder.DCTEngine;
import Encoder.DifferenceEngine;
import Encoder.OutputStream;
import Encoder.QuadtreeEngine;
import Encoder.VectorEngine;
import Utils.MacroBlock;
import Utils.PixelRaster;
import Utils.QueueObject;
import Utils.Vector;

public class Main {
	private static final int MAX_REFERENCES = config.MAX_REFERENCES;
	
	private static DCTEngine DCT_ENGINE = new DCTEngine();
	private static QuadtreeEngine QUADTREE_ENGINE = new QuadtreeEngine();
	private static DifferenceEngine DIFFERENCE_ENGINE = new DifferenceEngine();
	private static VectorEngine VECTOR_ENGINE = new VectorEngine();
	
	public static void main(String [] args) {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.showDialog(null, null);
		
		if (jfc.getSelectedFile() == null) {
			return;
		}
		
		File in = jfc.getSelectedFile();
		
		jfc.showDialog(null, null);
		
		if (jfc.getSelectedFile() == null) {
			return;
		}
		
		File out = jfc.getSelectedFile();
		
		encode(in, out);
	}
	
	public static void encode(File input, File output) {
		OutputStream outStream = new OutputStream(new File(input.getParent()));
		ArrayList<PixelRaster> references = new ArrayList<PixelRaster>(MAX_REFERENCES);
		PixelRaster futFrame = null;
		PixelRaster curFrame = null;
		PixelRaster prevFrame = null;
		
		int files = input.listFiles().length;
		long sumOfMilliSeconds = 0;
		
		try {
			outStream.activate();
			
			for (int i = 0; i < files; i++) {
				long start = System.currentTimeMillis();
				File frameFile = getAwaitedFile(input, i, ".bmp");
				
				if (!frameFile.exists()) {
					System.out.println("Skip: " + i);
					continue;
				}
				
				if (prevFrame == null) {
					prevFrame = new PixelRaster(ImageIO.read(frameFile));
//					futureFrame = new PixelRaster(ImageIO.read(getAwaitedFile(input, i + 1, ".bmp")));
					references.add(prevFrame);
					continue;
				}
				
				curFrame = new PixelRaster(ImageIO.read(frameFile));
//				futureFrame = new PixelRaster(ImageIO.read(getAwaitedFile(input, i + 1, ".bmp")));
				
				DCT_ENGINE.applyDCTOnPixelRaster(curFrame);
				ArrayList<MacroBlock> QuadtreeRoots = QUADTREE_ENGINE.constructQuadtree(curFrame);
				ArrayList<MacroBlock> leaveNodes = QUADTREE_ENGINE.getLeaveNodes(QuadtreeRoots);
				
				leaveNodes = DIFFERENCE_ENGINE.computeDifferences(curFrame.getColorSpectrum(), prevFrame, leaveNodes);
				ArrayList<Vector> movementVectors = VECTOR_ENGINE.computeMovementVectors(leaveNodes, references, futFrame);

//				BufferedImage vectors = VECTOR_ENGINE.drawVectors(movementVectors, curFrame.getDimension());
				VECTOR_ENGINE.drawVectorizedImage(prevFrame, movementVectors, references, futFrame);

//				ImageIO.write(vectors, "png", new File(output.getAbsolutePath() + "\\V_" + i + ".png"));
				ImageIO.write(curFrame.toBufferedImage(), "png", new File(output.getAbsolutePath() + "\\VR_" + i + ".png"));
				
				outStream.addObjectToOutputQueue(new QueueObject(movementVectors));
				
				long end = System.currentTimeMillis();
				long time = end - start;
				sumOfMilliSeconds += time;
				printStatistics(time, sumOfMilliSeconds, i, movementVectors);
				
				prevFrame = curFrame;
				references.add(prevFrame);
				manageReferences(references);
			}
			
			outStream.shutdown();
			references.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void printStatistics(long time, long fullTime, int index, ArrayList<Vector> vecs) {
		int vecArea = 0;
		for (Vector v : vecs) vecArea += v.getSize() * v.getSize();
		
		System.out.println("");
		System.out.println("Frame " + index + ":");
		System.out.println("Time: " + time + "ms | Avg. time: " + (fullTime / index) + "ms");
		System.out.println("Vectors: " + vecs.size() + " | Covered area: " + vecArea + "px | Avg. MSE: " + (VECTOR_ENGINE.getVectorMSE() / vecs.size()));
	}
	
	private static void manageReferences(ArrayList<PixelRaster> references) {
		if (references == null) {
			return;
		}
		
		if (references.size() < MAX_REFERENCES) {
			return;
		}
		
		references.remove(0);
	}
	
	private static File getAwaitedFile(File parent, int index, String format) {
		StringBuilder name = new StringBuilder(32);
		name.append(parent.getAbsolutePath() + "/");
		
		if (index < 10) {
			name.append("000");
		} else if (index < 100) {
			name.append("00");
		} else if (index < 1000) {
			name.append("0");
		}
		
		name.append(index);
		name.append(format);
		return new File(name.toString());
	}
}
