package app.rendering;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import app.utils.PixelRaster;

public class VideoPlayer {
	private JFrame frame = new JFrame();
	private VideoDisplay display = new VideoDisplay();
	
	private ConcurrentLinkedQueue<PixelRaster> frameQueue = new ConcurrentLinkedQueue<PixelRaster>();
	private PixelRaster currentFrame = null;
	private boolean waitToFinish = false;
	
	private final int FRAME_RATE = 30;
	private final int TIME_BETWEEN_FRAMES = 1000 / FRAME_RATE;
	
	public VideoPlayer() {
		this.frame.setSize(600, 500);
		this.frame.add(this.display);
		this.frame.setVisible(true);
		run();
	}
	
	public void addFrame(PixelRaster frame) {
		this.frameQueue.add(frame);
	}
	
	public PixelRaster getFrame() {
		return this.currentFrame;
	}
	
	private PixelRaster getCurrentFrame() {
		this.currentFrame = this.frameQueue.poll();
		return getFrame();
	}
	
	private void run() {
		Thread playerThread = new Thread(() -> {
			while (!waitToFinish || frameQueue.isEmpty()) {
				try {
					Thread.sleep(TIME_BETWEEN_FRAMES);
				} catch (InterruptedException e) {}
				
				PixelRaster raster = getCurrentFrame();
				
				if (raster == null) {
					continue;
				}
				
				SwingUtilities.invokeLater(() -> display.update(raster));
			}
		});
		
		playerThread.setName("YAVC-Video-Player");
		playerThread.start();
	}
	
	public void finish() {
		this.waitToFinish = true;
	}
	
	private class VideoDisplay extends JPanel {
		private static final long serialVersionUID = -796631572266160117L;
		private PixelRaster frameToDisplay = null;
		
		public void update(PixelRaster frame) {
			this.frameToDisplay = frame;
			this.repaint();
		}
		
		@Override
		public void paint(Graphics g) {
			if (this.frameToDisplay == null) {
				return;
			}
			
			super.paintComponents(g);
			double ratioW = (double)this.getWidth() / (double)this.frameToDisplay.getWidth();
			Graphics2D g2d = (Graphics2D)g;
			g2d.scale(ratioW, ratioW);
			g2d.drawImage(this.frameToDisplay.toBufferedImage(), 0, 0, null);
			g2d.dispose();
		}
	}
}
