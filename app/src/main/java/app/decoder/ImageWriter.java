package app.decoder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import app.utils.PixelRaster;

public class ImageWriter {
	private ConcurrentLinkedQueue<PixelRaster> queue = new ConcurrentLinkedQueue<PixelRaster>();
	private boolean waitingForTermination = false;
	private int counter = 0;
	
	private static final int SLEEP_TIME = 20; //ms
	
	public ImageWriter(File output) {
		run(output);
	}
	
	public void add(PixelRaster img) {
		this.queue.add(img);
	}
	
	private void run(File output) {
		Thread writerThread = new Thread(() -> {
			while (!this.waitingForTermination) {
				if (this.queue.isEmpty()) {
					try {
						Thread.sleep(SLEEP_TIME);
					} catch (InterruptedException e) {}
					
					continue;
				}
				
				PixelRaster img = this.queue.poll();
				File out = new File(output.getAbsolutePath() + "/R_" + (counter++) + ".png");
				
				try {
					ImageIO.write(img.toBufferedImage(), "png", out);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		writerThread.setName("Image-Output-Stream");
		writerThread.start();
	}
	
	public void terminate() {
		this.waitingForTermination = true;
	}
}
