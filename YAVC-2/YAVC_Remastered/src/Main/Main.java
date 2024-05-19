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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

import Decoder.InputProcessor;
import Decoder.InputStream;
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
		decode(new File(out.getParent() + "/YAVC-Res.yavc.part"), out);
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
					outStream.writeStartFrame(output, prevFrame);
					references.add(prevFrame);
					continue;
				}
				
				prevFrame = new PixelRaster(ImageIO.read(getAwaitedFile(input, i - 1, ".bmp")));
				curFrame = new PixelRaster(ImageIO.read(frameFile));
//				futureFrame = new PixelRaster(ImageIO.read(getAwaitedFile(input, i + 1, ".bmp")));
				
				DCT_ENGINE.applyDCTOnPixelRaster(curFrame);
				ArrayList<MacroBlock> QuadtreeRoots = QUADTREE_ENGINE.constructQuadtree(curFrame);
				ArrayList<MacroBlock> leaveNodes = QUADTREE_ENGINE.getLeaveNodes(QuadtreeRoots);
				
//				BufferedImage[] part = QUADTREE_ENGINE.drawMacroBlocks(leaveNodes, curFrame.getDimension());
				leaveNodes = DIFFERENCE_ENGINE.computeDifferences(curFrame.getColorSpectrum(), prevFrame, leaveNodes);
				ArrayList<Vector> movementVectors = VECTOR_ENGINE.computeMovementVectors(leaveNodes, references, futFrame, prevFrame);
				
//				BufferedImage vectors = VECTOR_ENGINE.drawVectors(movementVectors, curFrame.getDimension());
				PixelRaster composit = outStream.renderResult(movementVectors, references, leaveNodes, prevFrame);

//				ImageIO.write(part[0], "png", new File(output.getAbsolutePath() + "/MB_" + i + ".png"));
//				ImageIO.write(part[1], "png", new File(output.getAbsolutePath() + "/MBA_" + i + ".png"));
//				ImageIO.write(vectors, "png", new File(output.getAbsolutePath() + "/V_" + i + ".png"));
				ImageIO.write(composit.toBufferedImage(), "png", new File(output.getAbsolutePath() + "/VR_" + i + ".png"));
				
				outStream.addObjectToOutputQueue(new QueueObject(movementVectors, leaveNodes));
				
				long end = System.currentTimeMillis();
				long time = end - start;
				sumOfMilliSeconds += time;
				printStatistics(time, sumOfMilliSeconds, i, movementVectors, leaveNodes);

				references.add(composit.copy());
				prevFrame = composit;
				manageReferences(references);
			}
			
			outStream.shutdown();
			references.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void printStatistics(long time, long fullTime, int index, ArrayList<Vector> vecs, ArrayList<MacroBlock> diffs) {
		System.out.println("");
		System.out.println("Frame " + index + ":");
		System.out.println("Time: " + time + "ms | Avg. time: " + (fullTime / index) + "ms");

		if (vecs != null) {
			int vecArea = 0;
			for (Vector v : vecs) vecArea += v.getSize() * v.getSize();
			System.out.println("Vectors: " + vecs.size() + " | Covered area: " + vecArea + "px | Avg. MSE: " + (VECTOR_ENGINE.getVectorMSE() / vecs.size()));
		}
		
		if (diffs != null) {
			int diffArea = 0;
			for (MacroBlock b : diffs) diffArea += b.getSize() * b.getSize();
			System.out.println("Non-Coded blocks: " + diffs.size() + " | Covered area: " + diffArea + "px");
		}
	}
	
	private static void manageReferences(ArrayList<?> references) {
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
	
	//DEBUG ONLY!
	public static void decode(File input, File output) {
		InputStream inputStream = new InputStream(input);
		InputProcessor processor = new InputProcessor();
		
		String startFrame = inputStream.getStartFrame();
		BufferedImage startFrameImg = processor.constructStartFrame(startFrame, new Dimension(176, 144));
		
		try {
			ImageIO.write(startFrameImg, "png", new File(input.getAbsolutePath() + "/SF.png"));
			ArrayList<BufferedImage> refs = new ArrayList<BufferedImage>();
			refs.add(startFrameImg);
			
			for (int i = 0; i < 100; i++) {
				String frame = inputStream.getFrame(i);
				BufferedImage result = processor.processFrame(frame, refs, new Dimension(176, 144));
				
				ImageIO.write(result, "png", new File(input.getAbsolutePath() + "/R_" + i + ".png"));
				refs.add(result);
				manageReferences(refs);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
