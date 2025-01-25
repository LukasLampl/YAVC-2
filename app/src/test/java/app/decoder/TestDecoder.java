package app.decoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.interprediction.VectorEngine;
import app.engines.prediction.interprediction.VectorEngineResult;
import app.engines.quadtree.QuadtreeEngine;
import app.exceptions.CorruptedFileException;
import app.io.InputProcessor;
import app.io.Protocol;
import app.managers.ListManager;
import app.managers.LoadDistributor;
import app.managers.ReferenceFrameManager;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

public class TestDecoder {
	private static final double DELTA = 0.005;
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
				QuadtreeEngine qE = new QuadtreeEngine();
				VectorEngine vE = new VectorEngine();
				
				ArrayList<MacroBlock> quadtreeRoots = qE.constructQuadtree(frame);
				LoadDistributor<MacroBlock> leaveNodeManager = qE.getLeaveNodes(quadtreeRoots);
				leaveNodeManager.compute(frame.getWidth() * frame.getHeight());
				VectorEngineResult vectorEngineResult = vE.computeMovementVectors(leaveNodeManager.getRawData(), refMan);
				LoadDistributor<Vector> movementVectors = vectorEngineResult.getVectors();
				List<Vector> originalVectors = movementVectors.getRawData();
				byte[] vectorData = Protocol.getVectorBytes(originalVectors, false);
				ListManager<Vector> vectorListManager = new ListManager<Vector>();
				super.getVectors(vectorData, vectorListManager, true); //Single thread to keep the order of the original vectors
				List<Vector> decodedVectors = vectorListManager.getList();
				assertEquals(originalVectors.size(), decodedVectors.size());
				
				for (int i = 0; i < originalVectors.size(); i++) {
					if (i % 1000 == 0) {
						System.out.println("Vectors checked: " + i + "/" + originalVectors.size());
					}
					
					Vector originalVec = originalVectors.get(i);
					Vector decodedVec = decodedVectors.get(i);
					assertEquals(originalVec.getPosition().x, decodedVec.getPosition().x);
					assertEquals(originalVec.getPosition().y, decodedVec.getPosition().y);
					assertEquals(originalVec.getSize(), decodedVec.getSize());
					assertEquals(originalVec.getSpanX(), decodedVec.getSpanX());
					assertEquals(originalVec.getSpanY(), decodedVec.getSpanY());
					
					double[][][] originalDiffs = originalVec.getDCTCoefficientsOfAbsoluteColorDifference();
					double[][][] decodedDiffs = decodedVec.getDCTCoefficientsOfAbsoluteColorDifference();
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
				QuadtreeEngine qE = new QuadtreeEngine();
				VectorEngine vE = new VectorEngine();
				
				ArrayList<MacroBlock> list = new ArrayList<MacroBlock>();
				list.add(new MacroBlock(0, 0, 4, frame.getPixelBlock(new Point(0, 0), 4, null)));
				LoadDistributor<MacroBlock> leaveNodeManager = qE.getLeaveNodes(list);
				leaveNodeManager.compute(frame.getWidth() * frame.getHeight());
				VectorEngineResult vectorEngineResult = vE.computeMovementVectors(leaveNodeManager.getRawData(), refMan);
				LoadDistributor<Vector> movementVectors = vectorEngineResult.getVectors();
				List<Vector> originalVectors = movementVectors.getRawData();
				byte[] vectorData = Protocol.getVectorBytes(originalVectors, false);
				ListManager<Vector> vectorListManager = new ListManager<Vector>();
				super.getVectors(vectorData, vectorListManager, true); //Single thread to keep the order of the original vectors
				List<Vector> decodedVectors = vectorListManager.getList();
				assertEquals(originalVectors.size(), decodedVectors.size());
				
				for (int i = 0; i < originalVectors.size(); i++) {
					if (i % 1000 == 0) {
						System.out.println("Vectors checked: " + i + "/" + originalVectors.size());
					}
					
					Vector originalVec = originalVectors.get(i);
					Vector decodedVec = decodedVectors.get(i);
					assertEquals(originalVec.getPosition().x, decodedVec.getPosition().x);
					assertEquals(originalVec.getPosition().y, decodedVec.getPosition().y);
					assertEquals(originalVec.getSize(), decodedVec.getSize());
					assertEquals(originalVec.getSpanX(), decodedVec.getSpanX());
					assertEquals(originalVec.getSpanY(), decodedVec.getSpanY());
					
					double[][][] originalDiffs = originalVec.getDCTCoefficientsOfAbsoluteColorDifference();
					double[][][] decodedDiffs = decodedVec.getDCTCoefficientsOfAbsoluteColorDifference();
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
}
