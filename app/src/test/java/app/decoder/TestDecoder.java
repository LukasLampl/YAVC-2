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

package app.decoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import app.encoder.MockQuadtreeEngine;
import app.engines.prediction.interprediction.DecodingVector;
import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.interprediction.VectorEngine;
import app.engines.prediction.intraprediction.DecodingIntraPredictionBlock;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.engines.prediction.intraprediction.IntraEngine;
import app.engines.quadtree.QuadtreeBase;
import app.engines.quadtree.QuadtreeEngine;
import app.exceptions.CorruptedFileException;
import app.io.BitWriter;
import app.io.InputProcessor;
import app.io.Protocol;
import app.managers.ListManager;
import app.managers.LoadDistributor;
import app.managers.ReferenceFrameManager;
import app.utils.PixelRaster;
import app.utils.components.MacroBlock;

public class TestDecoder {
	private static final double DELTA = 0.005;
	private static final double YUV_CONVERTING_DELTA = 0.5;
	private BufferedImage[] TEST_FRAMES = new BufferedImage[4];
	
	public TestDecoder() {
		try {
			for (int i = 0; i < 4; i++) {
				TEST_FRAMES[i] = ImageIO.read(this.getClass().getResourceAsStream("/frame" + i + ".bmp"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Test the decoder for correct vectors when first converted to bytes and back.
	 * Basically an encoding and decoding process without rendering.
	 */
	@Test
	@Disabled
	public void testVectorTranslation001() {
		if (this.TEST_FRAMES[0] == null
			|| this.TEST_FRAMES[1] == null
			|| this.TEST_FRAMES[2] == null
			|| this.TEST_FRAMES[3] == null) {
			throw new IllegalStateException("No valid input!");
		}
		
		class VectorTranslationTester extends InputProcessor {
			public void run() throws CorruptedFileException {
				ReferenceFrameManager refMan = new ReferenceFrameManager();
				refMan.add(new PixelRaster(TEST_FRAMES[0]));
				refMan.add(new PixelRaster(TEST_FRAMES[1]));
				refMan.add(new PixelRaster(TEST_FRAMES[2]));
				PixelRaster frame = new PixelRaster(TEST_FRAMES[3]);
				VectorEngine vE = new VectorEngine();
				
				List<MacroBlock> quadtreeRoots = QuadtreeEngine.constructQuadtree(frame);
				LoadDistributor<MacroBlock> leaveNodeManager = QuadtreeEngine.getLeaveNodes(quadtreeRoots);
				leaveNodeManager.compute(frame.getWidth() * frame.getHeight());
				LoadDistributor<EncodingVector> movementVectors = vE.computeMovementVectors(leaveNodeManager.getRawData(), refMan);
				List<EncodingVector> originalVectors = movementVectors.getRawData();
				byte[] vectorData = Protocol.getVectorBytes(originalVectors, false);
				ListManager<DecodingVector> vectorListManager = new ListManager<DecodingVector>();
				super.getVectors(vectorData, vectorListManager, true); //Single thread to keep the order of the original vectors
				List<DecodingVector> decodedVectors = vectorListManager.getList();
				assertEquals(originalVectors.size(), decodedVectors.size());
				
				for (int i = 0; i < originalVectors.size(); i++) {
					if (i % 1000 == 0) {
						System.out.println("Vectors checked: " + i + "/" + originalVectors.size());
					}
					
					EncodingVector originalVec = originalVectors.get(i);
					DecodingVector decodedVec = decodedVectors.get(i);
					assertEquals(originalVec.getPosition().x, decodedVec.getPosition().x);
					assertEquals(originalVec.getPosition().y, decodedVec.getPosition().y);
					assertEquals(originalVec.getSize(), decodedVec.getSize());
					assertEquals(originalVec.getSpanX(), decodedVec.getSpanX());
					assertEquals(originalVec.getSpanY(), decodedVec.getSpanY());
					
					double[][][] originalDiffs = originalVec.getYUVDelta();
					double[][][] decodedDiffs = decodedVec.getYUVDeltas();
					assertEquals(originalDiffs.length, decodedDiffs.length);
					assertEquals(originalDiffs[0].length, decodedDiffs[0].length);
					
					for (int n = 0; n < originalDiffs.length; n++) {
						for (int j = 0; j < originalDiffs[n].length; j++) {
							assertArrayEquals(originalDiffs[n][j], decodedDiffs[n][j], DELTA);
						}
					}
				}
			}
		}
		
		VectorTranslationTester executor = new VectorTranslationTester();
		try {
			executor.run();
		} catch (CorruptedFileException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	@Disabled
	public void testVectorTranslation002() {
		if (this.TEST_FRAMES[0] == null
			|| this.TEST_FRAMES[1] == null
			|| this.TEST_FRAMES[2] == null
			|| this.TEST_FRAMES[3] == null) {
			throw new IllegalStateException("No valid input!");
		}
		
		class VectorTranslationTester extends InputProcessor {
			public void run() throws CorruptedFileException {
				ReferenceFrameManager refMan = new ReferenceFrameManager();
				refMan.add(new PixelRaster(TEST_FRAMES[0]));
				refMan.add(new PixelRaster(TEST_FRAMES[1]));
				refMan.add(new PixelRaster(TEST_FRAMES[2]));
				PixelRaster frame = new PixelRaster(TEST_FRAMES[3]);
				VectorEngine vE = new VectorEngine();
				
				ArrayList<MacroBlock> list = new ArrayList<MacroBlock>();
				list.add(new MacroBlock(0, 0, 4, frame.getPixelBlock(new Point(0, 0), 4, null)));
				LoadDistributor<MacroBlock> leaveNodeManager = QuadtreeEngine.getLeaveNodes(list);
				leaveNodeManager.compute(frame.getWidth() * frame.getHeight());
				LoadDistributor<EncodingVector> movementVectors = vE.computeMovementVectors(leaveNodeManager.getRawData(), refMan);
				List<EncodingVector> originalVectors = movementVectors.getRawData();
				byte[] vectorData = Protocol.getVectorBytes(originalVectors, false);
				ListManager<DecodingVector> vectorListManager = new ListManager<DecodingVector>();
				super.getVectors(vectorData, vectorListManager, true); //Single thread to keep the order of the original vectors
				List<DecodingVector> decodedVectors = vectorListManager.getList();
				assertEquals(originalVectors.size(), decodedVectors.size());
				
				for (int i = 0; i < originalVectors.size(); i++) {
					if (i % 1000 == 0) {
						System.out.println("Vectors checked: " + i + "/" + originalVectors.size());
					}
					
					EncodingVector originalVec = originalVectors.get(i);
					DecodingVector decodedVec = decodedVectors.get(i);
					assertEquals(originalVec.getPosition().x, decodedVec.getPosition().x);
					assertEquals(originalVec.getPosition().y, decodedVec.getPosition().y);
					assertEquals(originalVec.getSize(), decodedVec.getSize());
					assertEquals(originalVec.getSpanX(), decodedVec.getSpanX());
					assertEquals(originalVec.getSpanY(), decodedVec.getSpanY());
					
					double[][][] originalDiffs = originalVec.getYUVDelta();
					double[][][] decodedDiffs = decodedVec.getYUVDeltas();
					assertEquals(originalDiffs.length, decodedDiffs.length);
					assertEquals(originalDiffs[0].length, decodedDiffs[0].length);
					
					for (int n = 0; n < originalDiffs.length; n++) {
						for (int j = 0; j < originalDiffs[n].length; j++) {
							assertArrayEquals(originalDiffs[n][j], decodedDiffs[n][j], DELTA);
						}
					}
				}
			}
		}
		
		VectorTranslationTester executor = new VectorTranslationTester();
		try {
			executor.run();
		} catch (CorruptedFileException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Test the decoder for correct vectors when first converted to bytes and back.
	 * Basically an encoding and decoding process without rendering.
	 */
	@Test
	@Disabled
	public void testIntrablockTranslation001() {
		if (this.TEST_FRAMES[0] == null
			|| this.TEST_FRAMES[1] == null
			|| this.TEST_FRAMES[2] == null
			|| this.TEST_FRAMES[3] == null) {
			throw new IllegalStateException("No valid input!");
		}
		
		class IntrablockTranslationTester extends InputProcessor {
			public void run() throws CorruptedFileException {
				ReferenceFrameManager refMan = new ReferenceFrameManager();
				refMan.add(new PixelRaster(TEST_FRAMES[0]));
				refMan.add(new PixelRaster(TEST_FRAMES[1]));
				refMan.add(new PixelRaster(TEST_FRAMES[2]));
				PixelRaster frame = new PixelRaster(TEST_FRAMES[3]);
				IntraEngine iE = new IntraEngine();
				
				List<MacroBlock> quadtreeRoots = QuadtreeEngine.constructQuadtree(frame);
				LoadDistributor<MacroBlock> leaveNodeManager = QuadtreeEngine.getLeaveNodes(quadtreeRoots);
				leaveNodeManager.compute(frame.getWidth() * frame.getHeight());
				LoadDistributor<EncodingIntraPredictionBlock> intraBlocks = iE.computeIntraPrediction(leaveNodeManager.getRawData(), frame);
				List<EncodingIntraPredictionBlock> originalBlocks = intraBlocks.getRawData();
				byte[] intraData = Protocol.getIntraBlockBytes(originalBlocks, false);
				ListManager<DecodingIntraPredictionBlock> intraBlockManager = new ListManager<DecodingIntraPredictionBlock>();
				super.getIntraPreditionBlocks(intraData, intraBlockManager, true);
				List<DecodingIntraPredictionBlock> decodedBlocks = intraBlockManager.getList();
				assertEquals(originalBlocks.size(), decodedBlocks.size());
				
				for (int i = 0; i < originalBlocks.size(); i++) {
					if (i % 100 == 0) {
						System.out.println("Intrablocks checked: " + i + "/" + originalBlocks.size());
					}
					
					EncodingIntraPredictionBlock originalBlock = originalBlocks.get(i);
					DecodingIntraPredictionBlock decodedBlock = decodedBlocks.get(i);
					System.out.println("Size: " + originalBlock.getSize());
					assertEquals(originalBlock.getPositionX(), decodedBlock.getPositionX());
					assertEquals(originalBlock.getPositionY(), decodedBlock.getPositionY());
					assertEquals(originalBlock.getSize(), decodedBlock.getSize());
					assertEquals(originalBlock.getAngle(), decodedBlock.getAngle());
					check2DArray(originalBlock.getVertical(), decodedBlock.getVertical());
					check2DArray(originalBlock.getHorizontal(), decodedBlock.getHorizontal());
					
					double[][][] originalDiffs = originalBlock.getYUVDeltas();
					double[][][] decodedDiffs = decodedBlock.getYUVDeltas();
					assertEquals(originalDiffs.length, decodedDiffs.length);
					assertEquals(originalDiffs[0].length, decodedDiffs[0].length);
					
					for (int n = 0; n < originalDiffs.length; n++) {
						for (int j = 0; j < originalDiffs[n].length; j++) {
							assertArrayEquals(originalDiffs[n][j], decodedDiffs[n][j], DELTA);
						}
					}
				}
			}
		}
		
		IntrablockTranslationTester executor = new IntrablockTranslationTester();
		try {
			executor.run();
		} catch (CorruptedFileException e) {
			e.printStackTrace();
		}
	}
	
	private void check2DArray(final double[][] expected, final double[][] actual) {
		for (int i = 0; i < expected.length; i++) {
			assertArrayEquals(expected[i], actual[i], YUV_CONVERTING_DELTA);
		}
	}
	
	@Test
	public void test() throws IOException {
		for (int i = 0; i < 1; i++) {
			Dimension bounds = new Dimension(QuadtreeBase.MAX_SIZE, QuadtreeBase.MAX_SIZE);
			List<MacroBlock> roots = MockQuadtreeEngine.generateQuadtrees(1, bounds, false);
			MockQuadtreeEngine.assignLinks(roots);
			BitWriter data = Protocol.binarizeQuadtrees(roots);
			System.out.println("Final len: " + data.getTotalBits() + " Bits");
			System.out.println(" - " + (data.getTotalBits() / Byte.SIZE) + " Byte");
			System.out.println(" - Around " + (((double)data.getTotalBits() / (double)Byte.SIZE) / 24000) + "% of 24kB");
			
//			File f = new File("C:\\Users\\Lukas Lampl\\Documents\\EncoderOut\\enctest.bin");
//			f.createNewFile();
//			
//			if (i == 0) {
//				Files.write(Path.of(f.getAbsolutePath()), data, StandardOpenOption.TRUNCATE_EXISTING);
//				continue;
//			}
//			
//			Files.write(Path.of(f.getAbsolutePath()), data, StandardOpenOption.APPEND);
		}
	}
}
