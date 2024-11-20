package app.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import app.exceptions.CorruptedFileException;
import app.exceptions.WrongBlockAssignedException;
import app.interprediction.Vector;
import app.interprediction.VectorEngine;
import app.interprediction.VectorEngineResult;
import app.io.InputProcessor;
import app.io.Protocol;
import app.quadtree.QuadtreeEngine;
import app.utils.ListManager;
import app.utils.LoadDistributor;
import app.utils.MacroBlock;
import app.utils.PixelRaster;
import app.utils.ReferenceFrameManager;

public class TestDecoder {
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
	public void testVectorTranslation() {
		if (this.TEST_FRAMES[0] == null
			|| this.TEST_FRAMES[1] == null
			|| this.TEST_FRAMES[2] == null
			|| this.TEST_FRAMES[3] == null) {
			throw new IllegalStateException("No valid input!");
		}
		
		class VectorTranslationTester extends InputProcessor {
			public void run() throws CorruptedFileException, WrongBlockAssignedException {
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
				VectorEngineResult vectorEngineResult = vE.computeMovementVectors(leaveNodeManager, refMan);
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
					
					ArrayList<double[][][]> originalDiffs = originalVec.getDCTCoefficientsOfAbsoluteColorDifference();
					ArrayList<double[][][]> decodedDiffs = decodedVec.getDCTCoefficientsOfAbsoluteColorDifference();
					assertEquals(originalDiffs.size(), decodedDiffs.size());
					
					if (originalVec.getSize() != 4) {
						for (int n = 0; n < originalDiffs.size(); n++) {
							double[][][] originalValues = originalDiffs.get(n);
							double[][][] decodedValues = decodedDiffs.get(n);
							
							for (int j = 0; j < 8; j++) {
								for (int k = 0; k < 8; k++) {
									assertEquals(originalValues[0][j][k], decodedValues[0][j][k]);
								}
							}
							
							for (int j = 0; j < 4; j++) {
								for (int k = 0; k < 4; k++) {
									assertEquals(originalValues[1][j][k], decodedValues[1][j][k]);
									assertEquals(originalValues[2][j][k], decodedValues[2][j][k]);
								}
							}
						}
					} else {
						double[][][] originalValues = originalDiffs.get(0);
						double[][][] decodedValues = decodedDiffs.get(0);
						
						for (int j = 0; j < 4; j++) {
							for (int k = 0; k < 4; k++) {
								assertEquals(originalValues[0][j][k], decodedValues[0][j][k]);
							}
						}
						
						for (int j = 0; j < 2; j++) {
							for (int k = 0; k < 2; k++) {
								assertEquals(originalValues[1][j][k], decodedValues[1][j][k]);
								assertEquals(originalValues[2][j][k], decodedValues[2][j][k]);
							}
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
		} catch (WrongBlockAssignedException e) {
			e.printStackTrace();
		}
	}
}
