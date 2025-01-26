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

import app.ArgumentProcessor;
import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.intraprediction.IntraPredictionBlock;
import app.exceptions.CorruptedFileException;
import app.io.ImageWriter;
import app.io.InputProcessor;
import app.io.InputStream;
import app.managers.ListManager;
import app.managers.ReferenceFrameManager;
import app.rendering.VideoPlayer;
import app.utils.PixelRaster;

/**
 * The {@code Decoder} class is responsible for decoding a given file
 * to the frame representation and do "high level" work, like video playback
 * and so on.
 * 
 * @author Lukas Lampl
 * @since 1.1.0
 */
public class Decoder {
	/**
	 * A {@link app.managers.ReferenceFrameManager ReferenceFrameManager} that manages
	 * all reference frames for the decoder.
	 */
	private ReferenceFrameManager referenceManager = new ReferenceFrameManager();
	
	/**
	 * The main decode function that invokes the whole decoding process by
	 * first reading in the video file partially and finally extracting the
	 * start frame, the vectors and non-coded blocks which are then in turn
	 * processed.
	 */
	public void decode() {
		VideoPlayer player = ArgumentProcessor.playback ? new VideoPlayer() : null;
		ListManager<Vector> vectorListManager = new ListManager<Vector>();
		ListManager<IntraPredictionBlock> intraBlockManager = new ListManager<IntraPredictionBlock>();
		ImageWriter imageWriter = new ImageWriter(ArgumentProcessor.outputFile);
		InputStream inputStream = new InputStream(ArgumentProcessor.inputFile);
		InputProcessor processor = new InputProcessor();
		processor.proessMetadata(inputStream.getMetadata());
		int lenOfIndexes = processor.initFrameReader(inputStream.getNumberOfIndexes());
		processor.getIndexes(inputStream.getIndexes(lenOfIndexes));
		
		int lengthOfStartFrame = processor.getNextLength();
		byte[] startFrame = inputStream.getChunk(lengthOfStartFrame);
		PixelRaster startFrameImg = processor.constructStartFrame(startFrame);
		
		try {
			imageWriter.add(startFrameImg);
			int totalLen = InputProcessor.FrameCount - 1; //-1 Because of SF (Start frame)
			this.referenceManager.add(startFrameImg);
			
			for (int i = 0; i < totalLen; i++) {
				long start = System.currentTimeMillis();
				System.out.println("FRAME: " + i + " (" + this.referenceManager.size() + ")");
				long start_len_grab = System.currentTimeMillis();
				int lengthOfVectors = processor.getNextLength();
				int lengthOfIntraBlocks = processor.getNextLength();
				long end_len_grab = System.currentTimeMillis();
				long start_data_grab = System.currentTimeMillis();
				byte[] vectors = inputStream.getChunk(lengthOfVectors);
				byte[] intraBlocks = inputStream.getChunk(lengthOfIntraBlocks);
				long end_data_grab = System.currentTimeMillis();
				long start_render = System.currentTimeMillis();
				PixelRaster result = processor.processFrame(vectors, intraBlocks, this.referenceManager, vectorListManager, intraBlockManager);
				long end_render = System.currentTimeMillis();
				
				long start_write = System.currentTimeMillis();
				imageWriter.add(result);
				this.referenceManager.add(result);
				long end_write = System.currentTimeMillis();
				long end = System.currentTimeMillis();
				
				System.out.println("- Total time: " + (end - start) + "ms");
				System.out.println("   > Grab data length: " + (end_len_grab - start_len_grab) + "ms");
				System.out.println("   > Grab data: " + (end_data_grab - start_data_grab) + "ms");
				System.out.println("   > Render time: " + (end_render - start_render) + "ms");
				System.out.println("   > Writing time: " + (end_write - start_write) + "ms");
				System.out.println();
				if (ArgumentProcessor.playback) {
					player.addFrame(result);
				}
				
				vectorListManager.switchList();
				intraBlockManager.switchList();
			}
		} catch (CorruptedFileException e) {
			e.printStackTrace();
		} finally {
			imageWriter.terminate();
		}
	}
}
