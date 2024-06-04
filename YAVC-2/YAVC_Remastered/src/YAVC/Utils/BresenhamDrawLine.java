package YAVC.Utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;


public class BresenhamDrawLine extends JFrame implements Paintable {

	private static final long serialVersionUID = 2088981894384212725L;

	public static void drawLine(int x0, int y0, int x1, int y1, double brightness, Paintable p) {
		int dx =  Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
		int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
		int err = dx + dy, e2;
		
		while(true) {
			p.setPixel(x0, y0, brightness);
			if (x0 == x1 && y0 == y1) break;
			e2 = err + err;
			if (e2 > dy) { err += dy; x0 += sx; }
			if (e2 < dx) { err += dx; y0 += sy; }
		}
	}
	
	
    private BufferedImage canvas;
    private JLabel l;

    public BresenhamDrawLine() {
        canvas = new BufferedImage(1280, 960, BufferedImage.TYPE_INT_ARGB);
        
 //       Graphics2D g2d = (Graphics2D)canvas.getGraphics();
 //       g2d.setColor(Color.RED);
 //       g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
 //       g2d.drawLine(9, 120, 100, 400);
 //       g2d.dispose();
        
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
	
    public static void main(String[] args) {
	       BresenhamDrawLine frame = new BresenhamDrawLine();

	       drawLine(55, 55, 845, 845, 0.8f, frame);
	       drawLine(55, 845, 845, 55, 0.2f, frame);

	       drawLine(50, 50, 50, 850, 1.0f, frame);
	       drawLine(50, 850, 850, 850, 0.75f, frame);
	       drawLine(850, 850, 850, 50, 0.5f, frame);
	       drawLine(850, 50, 50, 50, 0.25f, frame);
	       
	       drawLine(750, 10, 150, 10, 0.5f, frame);
	       drawLine(750, 10, 150, 100, 0.5f, frame);
	       drawLine(150, 100, 150, 10, 0.25f, frame);
	          
	       frame.addWindowFocusListener(new WindowAdapter() {
				@Override
				public void windowLostFocus(WindowEvent e) {
					System.exit(0);	
				}
	       });	
	}
}
