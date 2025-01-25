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

package app.io;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import app.utils.PixelRaster;

/**
 * The {@code ImageWriter} class provides functions for writing images
 * asynchronously to a specified output folder.
 * 
 * @author Lukas Lampl
 * @since 1.2 [Optimized prototype]
 */
public class ImageWriter {
	/**
	 * Holds all PixelRasters that should be written to the output folder.
	 */
	private ConcurrentLinkedQueue<PixelRaster> queue = new ConcurrentLinkedQueue<PixelRaster>();
	
	/**
	 * Flag for whether the writing process should terminate or not.
	 */
	private boolean waitingForTermination = false;
	
	/**
	 * Counter of the written frames (as scrap extension at the end).
	 */
	private int counter = 0;
	
	/**
	 * Defines the time to wait, when no image is in the {@link #queue}.
	 */
	private static final int SLEEP_TIME = 20; //ms
	
	/**
	 * Initializes an ImageWriter with the specified output folder
	 * and starts the writing thread.
	 * 
	 * @param output	The output folder of the images.
	 * @throws IllegalArgumentException When the specified output is not a folder.
	 */
	public ImageWriter(File output) {
		if (output == null) {
			return;
		}
		
		if (!output.isDirectory()) {
			throw new IllegalArgumentException("Can't write images into file!");
		}
		
		run(output);
	}
	
	/**
	 * Adds an given image to the writing {@link #queue}.
	 * 
	 * @param img	The image to add to the output queue.
	 */
	public void add(PixelRaster img) {
		this.queue.add(img);
	}
	
	/**
	 * Starts the writing thread and runs until the {@link #waitingForTermination}
	 * flag turns true.
	 * 
	 * <p>The thread works down the queue by head to tail and sleeps when
	 * no work is available. Else it tries to output a {@code .png} version
	 * of the currently processed image to the specified output.
	 * 
	 * @param output	The output folder.
	 */
	private void run(File output) {
		Thread writerThread = new Thread(() -> {
			while (!this.waitingForTermination || !this.queue.isEmpty()) {
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
	
	/**
	 * Invokes the termination process of the writer thread.
	 */
	public void terminate() {
		this.waitingForTermination = true;
	}
}
