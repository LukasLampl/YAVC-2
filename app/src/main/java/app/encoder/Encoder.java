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

package app.encoder;

import app.ArgumentProcessor;
import app.encoder.frames.BFrame;
import app.encoder.frames.Frame;
import app.engines.dct.DCTEngine;
import app.engines.prediction.interprediction.VectorEngine;
import app.engines.prediction.intraprediction.IntraEngine;
import app.io.ImagePreReader;
import app.io.OutputStream;
import app.managers.ReferenceFrameManager;
import app.utils.PixelRaster;

/**
 * The {@code Encoder} class is responsible for encoding given frames into
 * a YAVC portable format that can be read by the {@code Encoder}. This class
 * is a wrapper that is only responsible for calling the encoding methods.
 * 
 * @author Lukas Lampl
 * @since 1.1.0
 */
public class Encoder {
	/**
	 * The {@link app.engines.dct.DCTEngine DCTEngine} used in the encoding process,
	 * with precalculated values and tables.
	 */
	public DCTEngine DCT_ENGINE = null;
	
	/**
	 * The {@link app.engines.prediction.interprediction.VectorEngine VectorEngine} used for the whole
	 * encoding process.
	 */
	public static VectorEngine VECTOR_ENGINE = new VectorEngine();
	
	public static IntraEngine INTRA_ENGINE = new IntraEngine();

	/**
	 * The {@link app.managers.ReferenceFrameManager ReferenceFrameManager} used for
	 * managing all reference frames.
	 */
	private ReferenceFrameManager referenceManager = new ReferenceFrameManager();
	
	/**
	 * Creates a new Encoder object that is ready for encoding a list or folder
	 * of frames.
	 * 
	 * @param dctEngine	The DCTEngine to use in the encoding process.
	 */
	public Encoder(DCTEngine dctEngine) {
		this.DCT_ENGINE = dctEngine;
	}
	
	/**
	 * The main encode function that invokes the sub-tasks sequentially by
	 * first generating a Quadtree then calculating the differences, followed
	 * by interprediction and finally rendering and output.
	 */
	public void encode() {
		int files = ArgumentProcessor.inputFile.listFiles().length;
		final OutputStream outStream = new OutputStream(ArgumentProcessor.outputFile);
		final ImagePreReader imgReader = new ImagePreReader(files, ArgumentProcessor.inputFile, referenceManager);
		
		PixelRaster curFrame = null;
		PixelRaster prevFrame = null;
		
		long startOfTime = System.currentTimeMillis();
		
		try {
			outStream.activate();
			
			for (int i = 0; i < files; i++) {
				System.out.println("");
				System.out.println("Frame " + i + ":");
				PixelRaster frame = imgReader.getNextImage();
				
				if (frame == null) {
					System.out.println("Skip: " + i);
					continue;
				} else if (frame.notInvokedWithData == true) {
					System.out.println("Skip: " + i);
					continue;
				}
				
				if (prevFrame == null) {
					prevFrame = frame;
					outStream.writeMetadata(prevFrame.getDimension(), files - 1);
					outStream.writeStartFrame(prevFrame);
					this.referenceManager.add(prevFrame);
					continue;
				}
			
				curFrame = frame;

				Frame enc_frame = new BFrame(prevFrame, curFrame, this.referenceManager);
				enc_frame.compute();
				
				PixelRaster composit = enc_frame.getComposit();
				
				outStream.addObjectToOutputQueue(enc_frame.getProcessedQueueObject());
				
				this.referenceManager.add(composit.copy());
				prevFrame = composit;
			}
			
			long endOfTime = System.currentTimeMillis();
			System.out.println("Time used: " + (endOfTime - startOfTime) + "ms");
			outStream.finishQueue();
		} catch (Exception e) {
			outStream.shutdown();
			e.printStackTrace();
		}
	}
}
