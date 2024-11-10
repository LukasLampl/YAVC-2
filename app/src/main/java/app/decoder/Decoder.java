package app.decoder;

import java.io.File;

import app.exceptions.CorruptedFileException;
import app.exceptions.WrongBlockAssignedException;
import app.interprediction.ListManager;
import app.interprediction.Vector;
import app.utils.PixelRaster;
import app.utils.ReferenceFrameManager;

public class Decoder {
	private ReferenceFrameManager referenceManager = new ReferenceFrameManager();
	
	public void decode(File input, File output) {
		ListManager<Vector> vectorListManager = new ListManager<Vector>();
		ImageWriter imageWriter = new ImageWriter(output);
		InputStream inputStream = new InputStream(input);
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
				int lengthOfRawBlocks = processor.getNextLength();
				long end_len_grab = System.currentTimeMillis();
				long start_data_grab = System.currentTimeMillis();
				byte[] vectors = inputStream.getChunk(lengthOfVectors);
				byte[] rawBlocks = inputStream.getChunk(lengthOfRawBlocks);
				long end_data_grab = System.currentTimeMillis();
				long start_render = System.currentTimeMillis();
				PixelRaster result = processor.processFrame(vectors, rawBlocks, this.referenceManager, vectorListManager);
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
				vectorListManager.switchList();
			}
		} catch (CorruptedFileException e) {
			e.printStackTrace();
		} catch (WrongBlockAssignedException e) {
			e.printStackTrace();
		} finally {
			imageWriter.terminate();
		}
	}
}
