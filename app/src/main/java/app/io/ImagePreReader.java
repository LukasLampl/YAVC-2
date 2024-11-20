package app.io;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import app.utils.PixelRaster;
import app.utils.ReferenceFrameManager;

public class ImagePreReader {
	private final int framesToReadIn;
	private final File framesDir;
	
	private final int maxHoldImages = 2;
	private ConcurrentLinkedQueue<PixelRaster> queue = new ConcurrentLinkedQueue<PixelRaster>();
	private boolean allReadIn = false;
	
	private ReferenceFrameManager frameManager = null;
	
	public ImagePreReader(final int framesToReadIn, File framesDir, ReferenceFrameManager frameManager) {
		this.framesToReadIn = framesToReadIn;
		this.framesDir = framesDir;
		this.frameManager = frameManager;
		run();
	}
	
	public PixelRaster getNextImage() {
		while (this.queue.isEmpty() && !this.allReadIn);
		return this.queue.poll();
	}
	
	private void run() {
		Thread task = new Thread(() -> {
			int currentImage = 0;
			
			while (currentImage < framesToReadIn) {
				if (queue.size() >= maxHoldImages) {
					try {
						Thread.sleep(30);
						continue;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				File file = getAwaitedFile(framesDir, currentImage++, ".bmp");
				
				if (!file.exists()) {
					System.err.println("File \"" + file.getAbsolutePath() + "\" does not exist! > Skip");
					continue;
				}

				try {
					BufferedImage img = ImageIO.read(file);
					PixelRaster raster = frameManager.getChachedPixelRasterIfAvailable();
					
					if (raster == null) {
						raster = new PixelRaster(img);
					} else {
						raster.setData(img);
					}
					
					queue.add(raster);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			allReadIn = true;
		});
		
		task.setName("ImagePreReader");
		task.start();
	}
	
	private File getAwaitedFile(File parent, int index, String format) {
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