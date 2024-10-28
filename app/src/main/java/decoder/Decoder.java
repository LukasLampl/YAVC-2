package decoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import app.config;
import exceptions.CorruptedFileException;
import exceptions.WrongBlockAssignedException;
import utils.PixelRaster;

public class Decoder {
	public void decode(File input, File output) {
		InputStream inputStream = new InputStream(input);
		InputProcessor processor = new InputProcessor();
		processor.proessMetadata(inputStream.getMetadata());
		int lenOfIndexes = processor.initFrameReader(inputStream.getNumberOfIndexes());
		processor.getIndexes(inputStream.getIndexes(lenOfIndexes));
		
		int lengthOfStartFrame = processor.getNextLength();
		byte[] startFrame = inputStream.getChunk(lengthOfStartFrame);
		BufferedImage startFrameImg = processor.constructStartFrame(startFrame);
		
		try {
			ImageIO.write(startFrameImg, "png", new File(output.getAbsolutePath() + "/SF.png"));
			int totalLen = InputProcessor.FrameCount - 1; //-1 Because of SF (Start frame)
			ArrayList<PixelRaster> refs = new ArrayList<PixelRaster>();
			refs.add(new PixelRaster(startFrameImg));
			
			for (int i = 0; i < totalLen; i++) {
				System.out.println("FRAME: " + i + " (" + refs.size() + ")");
				int lengthOfVectors = processor.getNextLength();
				int lengthOfRawBlocks = processor.getNextLength();
				byte[] vectors = inputStream.getChunk(lengthOfVectors);
				byte[] rawBlocks = inputStream.getChunk(lengthOfRawBlocks);
				PixelRaster result = processor.processFrame(vectors, rawBlocks, refs);
				
				ImageIO.write(result.toBufferedImage(), "png", new File(output.getAbsolutePath() + "/R_" + i + ".png"));
				refs.add(result);
				manageReferences(refs);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CorruptedFileException e) {
			e.printStackTrace();
		} catch (WrongBlockAssignedException e) {
			e.printStackTrace();
		}
	}
	
	private void manageReferences(ArrayList<?> references) {
		if (references == null) {
			return;
		}
		
		if (references.size() <= config.MAX_REFERENCES) {
			return;
		}
		
		references.remove(0);
	}
}
