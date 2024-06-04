package YAVC.Utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;


public class WuAADrawLine extends JFrame implements Paintable {
	
	private static final long serialVersionUID = -8300550009729460123L;

	public static void main(String[] args) {
	       WuAADrawLine frame = new WuAADrawLine();
	       drawLine(55, 55, 845, 845, 0.95f, frame);
	       drawLine(845, 55, 55, 845, 0.05f, frame);
	       drawLine(50, 50, 50, 850, 1.0f, frame);
	       drawLine(50, 850, 850, 850, 0.75f, frame);
	       drawLine(850, 850, 850, 50, 0.5f, frame);
	       drawLine(850, 50, 50, 50, 0.25f, frame);
	       drawLine(750, 10, 150, 10, 0.5f, frame);
	       drawLine(750, 10, 150, 100, 0.5f, frame);
	       drawLine(150, 10, 150, 100, 0.5f, frame);
	       
    	   //frame.scale(4);
	    	   
	       frame.addWindowFocusListener(new WindowAdapter() {
				@Override
				public void windowLostFocus(WindowEvent e) {
					System.exit(0);	
				}
	       });
	}
	
    private BufferedImage canvas;
    private JLabel l;
    
    // Constructor to set up the canvas and the JFrame
    public WuAADrawLine() {
        canvas = new BufferedImage(1280, 960, BufferedImage.TYPE_INT_ARGB);
        
//        Graphics2D g2d = (Graphics2D)canvas.getGraphics();
//        g2d.setColor(Color.RED);
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2d.drawLine(9, 120, 100, 400);
//        g2d.dispose();
        
        l = new JLabel(new ImageIcon(canvas));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 960 + 35);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(l, null);
        setVisible(true);
    }
        
    @Override
    public void setPixel(int x, int y, double brightness) {
        int c = (int)(255 * brightness);
        int color = new Color(c, c, c).getRGB();
        canvas.setRGB(x, y, color);
    }
    
	public static void drawLine(double x0, double y0, double x1, double y1, double brightness, Paintable p) {
		
		boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);

		double t;
        if (steep) {
            t = y0; y0 = x0; x0 = t;
            t = y1; y1 = x1; x1 = t;
        }

        if (x0 > x1) {
            t = x0; x0 = x1; x1 = t;
            t = y0; y0 = y1; y1 = t;
        }

		// Compute the slope
		double dx = x1 - x0;
		double dy = y1 - y0;
		double gradient;
		if (dx == 0.0)
			gradient = 1.0f;
		else
			gradient = dy / dx;
		
		// Handle first endpoint
		int xend = (int)(x0 + 0.5f);
		double yend = y0 + gradient * (xend - x0);
		double xgap = rfpart(x0 + 0.5f);
		int xpxl1 = xend;
		int ypxl1 = (int) yend;
		if (steep) {
			p.setPixel(ypxl1, xpxl1, rfpart(yend) * xgap);
			p.setPixel(ypxl1 + 1, xpxl1, fpart(yend) * xgap);
		}
		else {
			p.setPixel(xpxl1, ypxl1, rfpart(yend) * xgap);
			p.setPixel(xpxl1, ypxl1 + 1, fpart(yend) * xgap);
		}
		
		double intery = yend + gradient;

		// Handle second endpoint
		xend = (int)(x1 + 0.5f);
		yend = y1 + gradient * (xend - x1);
		xgap = fpart(x1 + 0.5f);
		int xpxl2 = xend;
		int ypxl2 = (int) yend;
		if (steep) {
			p.setPixel(ypxl2, xpxl2, rfpart(yend) * xgap);
			p.setPixel(ypxl2 + 1, xpxl2, fpart(yend) * xgap);
		} 
		else {
			p.setPixel(xpxl2, ypxl2, rfpart(yend) * xgap);
			p.setPixel(xpxl2, ypxl2 + 1, fpart(yend) * xgap);
		}	

		// Main loop
		if (steep) {
			for (int x = xpxl1 + 1; x <= xpxl2 - 1; x++) {
				// Pixel coverage is determined by fractional part of y coordinate
				p.setPixel((int) intery,     x, rfpart(intery));
				p.setPixel((int) intery + 1, x, fpart(intery));
				intery += gradient;
			}
		} else {
			for (int x = xpxl1 + 1; x <= xpxl2 - 1; x++) {
				// Pixel coverage is determined by fractional part of y coordinate
				p.setPixel(x, (int) intery,     rfpart(intery));
				p.setPixel(x, (int) intery + 1, fpart(intery));
				intery += gradient;
			}
		}
	}

   
	// Method to get the fractional part of a number
	private static double fpart(double x) {
		if (x > 0)
			return x - (double)((int) x);
		else
			return x - (double)(((int) x) + 1);
	}

	// Method to get 1 - fractional part of number
	private static double rfpart(double x) {
		return 1.0f - fpart(x);
	}
}
