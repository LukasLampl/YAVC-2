package decoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import app.config;
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
			ArrayList<PixelRaster> refs = new ArrayList<PixelRaster>();
			refs.add(new PixelRaster(startFrameImg));
			
			for (int i = 0; i < 100; i++) {
				System.out.println("FRAME: " + i + " (" + refs.size() + ")");
				int lengthOfData = processor.getNextLength();
				byte[] frame = inputStream.getChunk(lengthOfData);
				PixelRaster result = processor.processFrame(frame, refs);
				
				ImageIO.write(result.toBufferedImage(), "png", new File(output.getAbsolutePath() + "/R_" + i + ".png"));
				refs.add(result);
				manageReferences(refs);
			}
		} catch (IOException e) {
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
