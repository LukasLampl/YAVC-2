package YAVC.Utils;

import java.awt.Point;
import java.util.ArrayList;

public class Deblocker {
	final static int MAX_QUANT = 15;
	
	final int alphas[] = {
		0, 4, 30, 80,
		4, 8, 65, 170,
		30, 52, 160, 220,
		80, 170, 220, 255
	};

	final int betas[] = {
		0, 4, 8, 10,
		4, 4, 10, 16,
		8, 10, 12, 22,
		10, 16, 22, 25
	};

	final int cs[] = {
		0, 0, 1, 4,
		0, 0, 1, 4,
		1, 1, 1, 4,
		2, 3, 4, 5
	};
	
	public void deblock(ArrayList<Vector> movementVecs, PixelRaster composit, int strength) {
		int index = clip(strength, 0, MAX_QUANT);
		int alpha = alphas[index];
		int beta = betas[index];
		int c = cs[index];
		
		for (Vector vec : movementVecs) {
			Point vecPos = vec.getPosition();
			Point blockPos = new Point(vecPos.x + vec.getSpanX(), vecPos.y + vec.getSpanY());
			int size = vec.getSize();
			
			if (blockPos.x == 0 || blockPos.y == 0) {
				continue;
			}
			
			for (int line = 0; line < size; line++) {
				executeHorizontalDeblocking(composit, blockPos, line, alpha, beta, c);
			}
			
			for (int row = 0; row < size; row++) {
				executeVerticalDeblocking(composit, blockPos, row, alpha, beta, c);
			}
		}
	}
	
	private void executeHorizontalDeblocking(PixelRaster composit, Point blockPos, int line, int alpha, int beta, int c) {
		double[] q0 = composit.getYUV(blockPos.x, line + blockPos.y);
		double[] q1 = composit.getYUV(blockPos.x + 1, line + blockPos.y);
		double[] q2 = composit.getYUV(blockPos.x + 2, line + blockPos.y);
		double[] p0 = composit.getYUV(blockPos.x - 1, line + blockPos.y);
		double[] p1 = composit.getYUV(blockPos.x - 2, line + blockPos.y);
		double[] p2 = composit.getYUV(blockPos.x - 3, line + blockPos.y);
		
		if ((Math.abs(p0[0] - q0[0]) < alpha) && (Math.abs(p1[0] - p0[0]) < beta) && (Math.abs(q0[0] - q1[0]) < beta)) {
			double deltaQ = Math.abs(q0[0] - q2[0]);
			double deltaP = Math.abs(p0[0] - p2[0]);
			int cd = c;
			
			if (deltaQ < beta) {
				cd++;
			}
			
			if (deltaP < beta) {
				cd++;
			}
			
			int average = (int)(q0[0] + p0[0] + 1) >> 1;
			int delta = clip((((int)(q0[0] - p0[0]) << 2) + (int)(p1[0] - q1[0]) + 4) >> 3, -cd, cd);
			int deltaP1 = clip((int)(p2[0] + average - ((int)p1[0] << 1)) >> 1, -c, c);
			int deltaQ1 = clip((int)(q2[0] + average - ((int)q1[0] << 1)) >> 1, -c, c);
			
			if (deltaP < beta) {
				composit.setLuma(blockPos.x - 2, line + blockPos.y, clip((int)p1[0] + deltaP1, 0, 255));
			}
			
			composit.setLuma(blockPos.x - 1, line + blockPos.y, clip((int)p0[0] + delta, 0, 255));
			composit.setLuma(blockPos.x, line + blockPos.y, clip((int)q0[0] - delta, 0, 255));
			
			if (deltaQ < beta) {
				composit.setLuma(blockPos.x + 1, line + blockPos.y, clip((int)q1[0] + deltaQ1, 0, 255));
			}
		}
	}
	
	private void executeVerticalDeblocking(PixelRaster composit, Point blockPos, int row, int alpha, int beta, int c) {
		double[] q0 = composit.getYUV(blockPos.x + row, blockPos.y);
		double[] q1 = composit.getYUV(blockPos.x + row, blockPos.y + 1);
		double[] q2 = composit.getYUV(blockPos.x + row, blockPos.y + 2);
		double[] p0 = composit.getYUV(blockPos.x + row, blockPos.y - 1);
		double[] p1 = composit.getYUV(blockPos.x + row, blockPos.y - 2);
		double[] p2 = composit.getYUV(blockPos.x + row, blockPos.y - 3);
		
		if ((Math.abs(p0[0] - q0[0]) < alpha) && (Math.abs(p1[0] - p0[0]) < beta) && (Math.abs(q0[0] - q1[0]) < beta)) {
			double deltaQ = Math.abs(q0[0] - q2[0]);
			double deltaP = Math.abs(p0[0] - p2[0]);
			int cd = c;
			
			if (deltaQ < beta) {
				cd++;
			}
			
			if (deltaP < beta) {
				cd++;
			}
			
			int average = (int)(q0[0] + p0[0] + 1) >> 1;
			int delta = clip((((int)(q0[0] - p0[0]) << 2) + (int)(p1[0] - q1[0]) + 4) >> 3, -cd, cd);
			int deltaP1 = clip((int)(p2[0] + average - ((int)p1[0] << 1)) >> 1, -c, c);
			int deltaQ1 = clip((int)(q2[0] + average - ((int)q1[0] << 1)) >> 1, -c, c);
			
			if (deltaP < beta) {
				composit.setLuma(blockPos.x + row, blockPos.y - 2, clip((int)p1[0] + deltaP1, 0, 255));
			}
			
			composit.setLuma(blockPos.x + row, blockPos.y - 1, clip((int)p0[0] + delta, 0, 255));
			composit.setLuma(blockPos.x + row, blockPos.y, clip((int)q0[0] - delta, 0, 255));
			
			if (deltaQ < beta) {
				composit.setLuma(blockPos.x + row, blockPos.y + 1, clip((int)q1[0] + deltaQ1, 0, 255));
			}
		}
	}

	private int clip(int x, int min, int max) {
		return x < min ? min : x > max ? max : x;
	};
}
