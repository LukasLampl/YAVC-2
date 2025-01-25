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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import app.config;
import app.utils.PixelRaster;
import app.utils.ReferenceFrameManager;

/**
 * The {@code ImagePreReader} reads in images asynchronously from the rest
 * of the process to assure, that at least as many images as specified in
 * {@code config.MAX_PRE_READ_IMAGES} to reduce conversion overhead and I/O
 * overhead in the main thread.
 * 
 * @author Lukas Lampl
 * @since 1.4.2 [Optimized prototype]
 */
public class ImagePreReader {
	/**
	 * The total amount of frames to read in.
	 * This is needed due to the fact that this module does not
	 * know anything about the total number of frames.
	 */
	private final int framesToReadIn;
	
	/**
	 * The directory in which the frames are located at.
	 */
	private final File framesDir;

	/**
	 * The PixelRaster queue that provides the pre-read images.
	 */
	private ConcurrentLinkedQueue<PixelRaster> queue = new ConcurrentLinkedQueue<PixelRaster>();
	
	/**
	 * Flag for whether the ImagePreReader has finished its work and read in all
	 * available images/frames.
	 */
	private boolean allReadIn = false;
	
	/**
	 * The ReferenceFrameManager used in the current processor to give the
	 * already disposed/discarded PixelRasters a new change to be filled with
	 * new data if available.
	 */
	private ReferenceFrameManager frameManager = null;
	
	/**
	 * Constructs a new {@code ImagePreReader} and starts it.
	 * 
	 * @param framesToReadIn	The number of frames to read in, in a lifetime.
	 * @param framesDir			The directory to the frames.
	 * @param frameManager		The current frame manager.
	 */
	public ImagePreReader(final int framesToReadIn, File framesDir, ReferenceFrameManager frameManager) {
		this.framesToReadIn = framesToReadIn;
		this.framesDir = framesDir;
		this.frameManager = frameManager;
		run();
	}
	
	/**
	 * Returns the next image in the queue.
	 * 
	 * <p><b>Warning:</b><br>
	 * This function blocks until an image is in the queue or the
	 * ImagePreReader has finished its work.
	 * </p>
	 * 
	 * @return
	 */
	public PixelRaster getNextImage() {
		while (this.queue.isEmpty() && !this.allReadIn);
		return this.queue.poll();
	}
	
	/**
	 * Runs the ImagePreReader asynchronously. It tries to get a file/image and
	 * convert it to a PixelRaster and put it into the queue. If possible the
	 * PixelRaster if not a new instance, but an already discarded one from a
	 * {@link app.utils.ReferenceFrameManager ReferenceFrameManager}.
	 */
	private void run() {
		Thread task = new Thread(() -> {
			int currentImage = 0;
			
			while (currentImage < framesToReadIn) {
				if (queue.size() >= config.MAX_PRE_READ_IMAGES) {
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
	
	/**
	 * Gets a file with a specific name in the given folder and format.
	 * 
	 * @param parent	The folder in which the file should be located at.
	 * @param index		Number of the file.
	 * @param format	Extension/Format of the file (".png", ".bmp" ect.).
	 * @return A file of the awaited name.
	 */
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