package YAVC.Encoder;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import YAVC.Utils.MacroBlock;
import YAVC.Utils.PixelRaster;

public class IntraEngine {
		
	private int biasLeft = 0;
	private int biasRight = 0;
	private PixelRaster curFrame = null;
	private ArrayList<MacroBlock> leaveList = null;
	private double errorThreshold[] = new double[3];

	@SuppressWarnings("unchecked")
	public IntraEngine() {
		this.biasLeft = 0;
		this.biasRight = 0;
	}
	
	class MacroBlockPrediction extends MacroBlock {
		HashMap <Integer,double[]> errorMap = new HashMap<Integer,double[]>();
		HashMap <Integer,ArrayList<MacroBlock>> neighbourMap = new HashMap<Integer,ArrayList<MacroBlock>>();
		HashMap <Integer,MacroBlock> predictionMap = new HashMap<Integer,MacroBlock>();
		
		
		MacroBlockPrediction(final MacroBlock leaf) {
			super(leaf);
			for (int orientation = -2 ; orientation <= 2 ; orientation++) {
				this.predictionMap.put(orientation, leaf.clone());
				this.neighbourMap.put(orientation, new ArrayList<MacroBlock>());
				this.errorMap.put(orientation, new double[3]);
			}
		}
	}
	
	public void setBias(final int biasLeft, final int biasRight) {
		this.biasLeft = biasLeft;
		this.biasRight = biasRight;
	}
	
	private void dump(final String s, final MacroBlock p) {
		System.out.println(s + " " + p.getPosition().x + "/" + p.getPosition().y + " Size:" + p.getSize());		
	}
	
	private void dump(final int orientation, final double[] error) {
		System.out.println("(%d) [%e %e %e]".formatted(orientation, error[0], error[1], error[2]));
	}
	
	public void computeIntraPrediction(final ArrayList<MacroBlock> leaveNodes, final PixelRaster curFrame, final double[] errorThreshold) {
		this.curFrame = curFrame;
		this.errorThreshold[0] = errorThreshold[0];
		this.errorThreshold[1] = errorThreshold[1];
		this.errorThreshold[2] = errorThreshold[2];
		this.leaveList = new ArrayList<MacroBlock>(leaveNodes.size());
		for (MacroBlock b : leaveNodes)
			this.leaveList.add(b);
		
		for (MacroBlock leaf : leaveList) {
			if (leaf.getPosition().x == 0 || leaf.getPosition().y == 0)
				dump("Border:", leaf);
			else
				computeIntraPredictionBlocks(leaf);
		}
	}
	
	private void computeIntraPredictionBlocks(final MacroBlock leaf)
	{
		System.out.println("--------------------------");
		dump("Calculate:", leaf);
		MacroBlockPrediction pred = new MacroBlockPrediction(leaf);
		ArrayList<MacroBlock> neigh0 = new ArrayList<MacroBlock>();
		for (MacroBlock b : this.leaveList) {
			Point k = b.getPosition();
			int ks = b.getSize();
			Point e = pred.getPosition();
			int es = pred.getSize();
			int orientation = 9999;
			if (k.y + ks == e.y && ((k.x <= e.x && k.x + ks >= e.x + es) || (k.x >= e.x && k.x + ks <= e.x + es))) {
				//dump(">>>", b);
				orientation = 1;
				neigh0.add(b);
			}
			else if (k.x + ks == e.x && ((k.y <= e.y && k.y + ks >= e.y + es) || (k.y >= e.y && k.y + ks <= e.y + es))) {
				//dump("<<<", b);
				orientation = -1;
				neigh0.add(b);
			}
			else if (k.x + ks == e.x && k.y + ks == e.y) {
				//dump("^^^", b);
				orientation = 0;
			}
			else if (this.biasRight > 0 && k.y + ks == e.y && k.x == e.x + es) {
				//dump(">==", b);
				orientation = 2;
			}
			else if (this.biasLeft > 0 && k.x + ks == e.x && k.y == e.y + es) {
				//dump("<==", b);
				orientation = -2;
			}
			if (orientation != 9999) {
				ArrayList<MacroBlock> neighbours = pred.neighbourMap.get(orientation);		
				neighbours.add(b.clone());
				pred.neighbourMap.put(orientation, neighbours);
			}
		}
		if (pred.neighbourMap.get(0).size() == 1) {
			ArrayList<MacroBlock> neighbours = pred.neighbourMap.get(0);
			for (MacroBlock n : neigh0) {
				neighbours.add(n.clone());
			}
			pred.neighbourMap.put(0, neighbours);
		}
		intraPrediction(pred);
		analyzeErrors(pred);
	}
	
	private void intraPrediction(final MacroBlockPrediction pred) {	    
		for (int orientation = -2 ; orientation <= 2 ; orientation++) {
			ArrayList<MacroBlock> neighbourList = pred.neighbourMap.get(orientation);
			if (neighbourList == null) {
				continue;
			}
			double [] error = null;
			MacroBlock p = pred.predictionMap.get(orientation);
			switch (orientation) {
				case 1:					
					error = intraPredictionP1(p, neighbourList);
					break;
				case 2:
					error = intraPredictionP2(p, neighbourList);
					break;
				case 0:
					error = intraPrediction0(p,  neighbourList);
					break;
				case -1:
					error = intraPredictionM1(p, neighbourList);
					break;
				case -2: 
					error = intraPredictionM2(p ,neighbourList);
					break;			
			}
			pred.errorMap.put(orientation, error);
		}		
	}
	
	private double[] intraPredictionP1(MacroBlock p, ArrayList<MacroBlock> list) {
		for (MacroBlock l : list) {
			int pos1 = 0, pos2 = 0;
			int pos3 = 0, pos4 = 0;

			int dl = p.getPosition().x - l.getPosition().x;
			int ds = Math.min(l.getSize(), p.getSize()) - 1;

			if (dl >= 0) {
				pos1 = dl;
				pos3 = 0;
			} else if (dl < 0) {
				pos1 = 0;
				pos3 = -dl;
			}
			pos2 = pos1 + ds;
			pos4 = pos3 + ds;

			//System.out.println(
			//		"(%d|%d (%d)) => (%d|%d (%d))   [%d-%d] => {%d-%d})".formatted(l.getPosition().x, l.getPosition().y,
			//				l.getSize(), p.getPosition().x, p.getPosition().y, p.getSize(), pos1, pos2, pos3, pos4));

			int y1 = l.getSize() - 1;
			int y3 = 0;
			int y4 = p.getSize() - 1;

			for (int i = pos1; i <= pos2; i++) {
				double[] d = l.getYUV(i, y1);
				for (int j = pos3; j <= pos4; j++)
					for (int k = y3; k <= y4; k++)
						p.setYUV(j, k, d);
			}
		}
		double error[] = MSE(p, this.curFrame);
		//dump(1, error);
		return error;
	}

	private double [] intraPredictionP2(MacroBlock p, ArrayList<MacroBlock> list) {
		return null;
	}
	
	private double [] intraPrediction0(MacroBlock p, ArrayList<MacroBlock> list) {
		//System.out.println("/// Size=%d".formatted(list.size()));
		for (MacroBlock l : list) {
			if (p.getPosition().x == l.getPosition().x + l.getSize() && p.getPosition().y == l.getPosition().y + l.getSize()) {
			//	System.out.println("/// 0=0");
				double[] d = l.getYUV(l.getSize() - 1, l.getSize() - 1);
				for (int j = 0 ; j < p.getSize() ; j++)
					p.setYUV(j, j, d);
			}
			else if (p.getPosition().x <= l.getPosition().x && p.getPosition().y == l.getPosition().y + l.getSize()) {
			//	System.out.println("/// 0+1");				
				for (int i = 0 ; i < Math.min(p.getSize(), l.getSize()) ; i++) {
					double[] d = l.getYUV(i, l.getSize() - 1);
					for (int j = l.getPosition().x - p.getPosition().x + 1 ; j < p.getSize() ; j++)
						for (int k = 0; k < p.getSize() ; k++)
							p.setYUV(j, k, d);
				}	
			}
			else if (p.getPosition().y <= l.getPosition().y && p.getPosition().x == l.getPosition().x + l.getSize()) {
			//	System.out.println("/// 0-1");				
				for (int i = 0 ; i < Math.min(p.getSize(), l.getSize()) ; i++) {
					double[] d = l.getYUV(l.getSize() - 1, i);
					for (int j = 0 ; j < p.getSize() ; j++)
						for (int k = l.getPosition().y - p.getPosition().y + 1; k < p.getSize() ; k++)
							p.setYUV(j, k, d);
				}
			}
		}
		double error[] = MSE(p, this.curFrame);
		//dump(0, error);
		return error;
	}
	
	private double[] intraPredictionM1(MacroBlock p, ArrayList<MacroBlock> list) {
		for (MacroBlock l : list) {
			int pos1 = 0, pos2 = 0;
			int pos3 = 0, pos4 = 0;

			int dl = p.getPosition().y - l.getPosition().y;
			int ds = Math.min(l.getSize(), p.getSize()) - 1;

			if (dl >= 0) {
				pos1 = dl;
				pos3 = 0;
			} else if (dl < 0) {
				pos1 = 0;
				pos3 = -dl;
			}
			pos2 = pos1 + ds;
			pos4 = pos3 + ds;

			//System.out.println(
			//		"(%d|%d (%d)) => (%d|%d (%d))   [%d-%d] => {%d-%d})".formatted(l.getPosition().x, l.getPosition().y,
			//				l.getSize(), p.getPosition().x, p.getPosition().y, p.getSize(), pos1, pos2, pos3, pos4));

			int x1 = l.getSize() - 1;
			int x3 = 0;
			int x4 = p.getSize() - 1;

			for (int i = pos1; i <= pos2; i++) {
				double[] d = l.getYUV(x1, i);
				for (int j = pos3; j <= pos4; j++)
					for (int k = x3; k <= x4; k++)
						p.setYUV(k, j, d);
			}
		}
		double error[] = MSE(p, this.curFrame);
		//dump(-1, error);
		return error;
	}
	
	private double [] intraPredictionM2(MacroBlock p, ArrayList<MacroBlock> list) {
		return null;
	}
	
	private double [] MSE(final MacroBlock p, final PixelRaster curFrame) {
		double errorY = 0.0, errorU = 0.0, errorV = 0.0;
		double _1n2 = 1.0 / (p.getSize() * p.getSize());
		for (int i = 0 ; i < p.getSize(); i++) {
			for (int j = 0 ; j < p.getSize() ; j++) {
				double[] curFrameYUV = curFrame.getYUV(i + p.getPosition().x, j + p.getPosition().y);
				double[] curBlockYUV = p.getYUV(i, j);
				double deltaY = curFrameYUV[0] - curBlockYUV[0];
				double deltaU = curFrameYUV[1] - curBlockYUV[1];
				double deltaV = curFrameYUV[2] - curBlockYUV[2];
				errorY += deltaY * deltaY;
				errorU += deltaU * deltaU;
				errorV += deltaV * deltaV;
			}
		}
		return new double[] { _1n2 * errorY, _1n2 * errorU, _1n2 * errorV };
	}	
	
	private void analyzeErrors(final MacroBlockPrediction pred) {
		double error[] = new double[] {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
		String[] msg = { "CurFrame", "CurFrame", "CurFrame" };
		for (int orientation = -2 ; orientation <= 2 ; orientation++) {
			if (pred.errorMap.get(orientation) != null) {
				for (int y = 0 ; y < error.length ; y++) {
					if (pred.errorMap.get(orientation)[y] <= errorThreshold[y] && pred.errorMap.get(orientation)[y] < error[y]) {
						error[y] = pred.errorMap.get(orientation)[y];
						msg[y] = "%e(%d)".formatted(error[y], orientation);
						for (int i = 0 ; i < pred.getSize() ; i++) {
							for (int j = 0 ; j < pred.getSize() ; j++) {
								double d[] = pred.getYUV(i,j);
								d[y] = pred.predictionMap.get(orientation).getYUV(i, j)[y];
								pred.setYUV(i,j,d);
							}
						}
					}
				}
			}
		}
		System.out.println("*** Y:%s U:%s V:%s ***".formatted(msg[0], msg[1], msg[2]));
	}
}
