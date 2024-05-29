package YAVC.Encoder;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import YAVC.Utils.MacroBlock;
import YAVC.Utils.PixelRaster;

public class IntraEngine {
		
	private Point elem = null;
	private int sizeX = 0;
	private int sizeY = 0;
	private int overLeft = 0;
	private int overRight = 0;
	private PixelRaster curFrame;
	private ArrayList<MacroBlock> blockList;
	private Map<MacroBlock,ArrayList<MacroBlockExt>> intraMap;
	
	class MacroBlockExt extends MacroBlock {
		int o;
		
		MacroBlockExt(final MacroBlock block, final int orientation) {
			super(block);
			this.o = orientation;
		}
		
		int getOrientation() {
			return this.o;
		}	
	}
	
	class MacroBlockIntra extends MacroBlock {
		HashMap <Integer,double[]> errorMap = new HashMap<Integer,double[]>();
		HashMap <Integer,MacroBlock> blockMap = new HashMap<Integer,MacroBlock>();
		
		MacroBlockIntra(final MacroBlock block) {
			super(block);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void initialize(final PixelRaster curFrame, final int overLeft, final int overRight, final ArrayList<MacroBlock> leaveNodes) {
		this.curFrame = curFrame;
		this.overLeft = overLeft;
		this.overRight = overRight;
		this.sizeX = this.curFrame.getWidth();
		this.sizeY = this.curFrame.getHeight();
		this.elem = null;
		this.intraMap = new HashMap<MacroBlock,ArrayList<MacroBlockExt>>();
		this.blockList = new ArrayList<MacroBlock>(leaveNodes.size());
		for (MacroBlock b : leaveNodes)
			this.blockList.add((MacroBlock) b.clone());
	}
	
	private void dump(final String s, final MacroBlock p) {
		System.out.println(s + " " + p.getPosition().x + "/" + p.getPosition().y + " Size:" + p.getSize());		
	}
	
	private void dump(final int orientation, final double[] error) {
		System.out.println("(%d) [%e %e %e]".formatted(orientation, error[0], error[1], error[2]));
	}
	
	public void computeIntraPrediction() {
		for (MacroBlock b : blockList) {
			if (b.getPosition().x == 0 || b.getPosition().y == 0)
				dump("Border:", b);
			else
				computeIntraPredictionBlocks(b);
		}
	}
	
	private void computeIntraPredictionBlocks(final MacroBlock macroBlock)
	{
		MacroBlockIntra p = new MacroBlockIntra(macroBlock);
		System.out.println("--------------------------");
		dump("Calculate:", p);
		intraMap.put(p, new ArrayList<MacroBlockExt>());
		for (MacroBlock b : blockList) {
			Point k = b.getPosition();
			int ks = b.getSize();
			Point e = p.getPosition();
			int es = p.getSize();
			ArrayList<MacroBlockExt> list = intraMap.get(p);
			if (k.y + ks == e.y && ((k.x <= e.x && k.x + ks >= e.x + es) || (k.x >= e.x && k.x + ks <= e.x + es))) {
				dump(">>>", b);
				list.add(new MacroBlockExt(b,1));
			}
			else if (k.x + ks == e.x && ((k.y <= e.y && k.y + ks >= e.y + es) || (k.y >= e.y && k.y + ks <= e.y + es))) {
				dump("<<<", b);
				list.add(new MacroBlockExt(b,-1));
			}
			else if (k.x + ks == e.x && k.y + ks == e.y) {
				dump("^^^", b);
				list.add(new MacroBlockExt(b,0));
			}
			else if (this.overRight > 0 && k.y + ks == e.y && k.x == e.x + es) {
				dump(">==", b);
				list.add(new MacroBlockExt(b,2));
			}
			else if (this.overLeft > 0 && k.x + ks == e.x && k.y == e.y + es) {
				dump("<==", b);
				list.add(new MacroBlockExt(b,-2));
			}
			intraMap.put(p, list);
		}
		intraPrediction(p, intraMap.get(p));
	}
	
	private void intraPrediction(MacroBlockIntra p, List<MacroBlockExt> list) {
		for (int i = -2 ; i <= 2 ; i++)
			p.blockMap.put(i,p.clone());
	    
		for (MacroBlockExt l : list) {
			double [] error = null;
			switch (l.getOrientation()) {
				case 1:
					error = intraPredictionP1(p.blockMap.get(l.getOrientation()),l);
					break;
				case 2:
					error = intraPredictionP2(p.blockMap.get(l.getOrientation()),l);
					break;
				case 0:
					error = intraPrediction0(p.blockMap.get(l.getOrientation()),l);
					break;
				case -1:
					error = intraPredictionM1(p.blockMap.get(l.getOrientation()),l);
					break;
				case -2: 
					error = intraPredictionM2(p.blockMap.get(l.getOrientation()),l);
					break;			
			}
			p.errorMap.put(l.getOrientation(), error);
		}
	}
	
	private double [] intraPredictionP1(MacroBlock p, MacroBlockExt l) {
		int pos1 = 0, pos2 = 0;
		int pos3 = 0, pos4 = 0;
		
		int dl = p.getPosition().x - l.getPosition().x;
		int ds = Math.min(l.getSize(),p.getSize()) - 1;
		
		if (dl >= 0) { 
			pos1 = dl;
			pos3 = 0;
		}
		else if (dl < 0) {
			pos1 = 0; 
			pos3 = -dl;
		}
		pos2 = pos1 + ds;
		pos4 = pos3 + ds;
		
		System.out.println("(%d|%d (%d)) => (%d|%d (%d))   [%d-%d] => {%d-%d})".formatted(l.getPosition().x,l.getPosition().y,l.getSize(),p.getPosition().x,p.getPosition().y,p.getSize(),pos1,pos2,pos3,pos4));		

		int y1 = l.getSize() - 1;
		int y3 = 0;
		int y4 = p.getSize() - 1;
				
		for (int i = pos1 ; i <= pos2 ; i++) {
			double[] d = l.getYUV(i, y1);
			for (int j = pos3 ; j <= pos4 ; j++)
				for (int k = y3 ; k <= y4 ; k++)
					p.setYUV(j, k, d);
		}
		double error[] = MSE(p, this.curFrame);
		dump(1, error);
		return error;
	}

	private double [] intraPredictionP2(MacroBlock p, MacroBlockExt l) {
		return null;
	}
	
	private double [] intraPrediction0(MacroBlock p, MacroBlockExt l) {
		return null;
	}
	
	private double [] intraPredictionM1(MacroBlock p, MacroBlockExt l) {
		int pos1 = 0, pos2 = 0;
		int pos3 = 0, pos4 = 0;
		
		int dl = p.getPosition().y - l.getPosition().y;
		int ds = Math.min(l.getSize(),p.getSize()) - 1;
		
		if (dl >= 0) { 
			pos1 = dl;
			pos3 = 0;
		}
		else if (dl < 0) {
			pos1 = 0; 
			pos3 = -dl;
		}
		pos2 = pos1 + ds;
		pos4 = pos3 + ds;

		System.out.println("(%d|%d (%d)) => (%d|%d (%d))   [%d-%d] => {%d-%d})".formatted(l.getPosition().x,l.getPosition().y,l.getSize(),p.getPosition().x,p.getPosition().y,p.getSize(),pos1,pos2,pos3,pos4));		
	
		int x1 = l.getSize() - 1;
		int x3 = 0;
		int x4 = p.getSize() - 1;
				
		for (int i = pos1 ; i <= pos2 ; i++) {
			double[] d = l.getYUV(x1, i);
			for (int j = pos3 ; j <= pos4 ; j++)
				for (int k = x3 ; k <= x4 ; k++)
					p.setYUV(k, j, d);
		}
		double error[] = MSE(p, this.curFrame);
		dump(-1, error);
		return error;
	}
	
	private double [] intraPredictionM2(MacroBlock p, MacroBlockExt l) {
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
}
