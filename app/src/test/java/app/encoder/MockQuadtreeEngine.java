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

package app.encoder;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import app.config;
import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.engines.quadtree.QuadtreeBase;
import app.engines.quadtree.QuadtreeEngine;
import app.utils.MathUtils;
import app.utils.components.MacroBlock;

public abstract class MockQuadtreeEngine {
	private static Random random = new Random(21);
	
	public static List<MacroBlock> generateQuadtrees(final int number, final Dimension dim,
			final boolean splitFully) {
		List<MacroBlock> roots = new ArrayList<MacroBlock>();
		int posX = 0;
		int posY = 0;
		
		for (int i = 0; i < number; i++) {
			roots.add(generateQuadtree(posX, posY, dim, splitFully));
			
			if (posX >= dim.width) {
				posX = 0;
				posY += QuadtreeBase.MAX_SIZE;
			}
		}
		
		return roots;
	}
	
	private static MacroBlock generateQuadtree(final int posX, final int posY,
			final Dimension dim, final boolean splitFully) {
		MacroBlock root = new MacroBlock(posX, posY, 4, true);//QuadtreeBase.MAX_SIZE, true);
		generateLeaves(root, dim, splitFully);
		return root;
	}
	
	private static void generateLeaves(final MacroBlock block, final Dimension dim,
			final boolean splitFully) {
		if (block.getSize() <= 4) {
			return;
		}
		
		if (random.nextBoolean() || splitFully) {
			block.subdivide(dim);
			
			for (final MacroBlock leaf : block.getNodes()) {
				generateLeaves(leaf, dim, splitFully);
			}
		}
	}
	
	public static void assignLinks(List<MacroBlock> roots) {
		List<MacroBlock> leaves = QuadtreeEngine.getLeaveNodes(roots).getRawData();
		
		for (final MacroBlock leaf : leaves) {
			if (random.nextDouble() < 0.5) {
				generateVector(leaf);
			} else {
				generateIntraBlock(leaf);
			}
		}
	}
	
	private static void generateIntraBlock(final MacroBlock parent) {
		double[][][] deltas = MatrixOperations.generateRandom3DMatrix(parent.getSize(), 40);
		double[][] hor = MatrixOperations.generateColorBased2DMatrix(parent.getSize());
		double[][] ver = MatrixOperations.generateColorBased2DMatrix(parent.getSize());
		int angle = MathUtils.round((random.nextDouble() * 180) / 5) * 5;
		
		EncodingIntraPredictionBlock b = new EncodingIntraPredictionBlock(parent.getPositionX(),
											parent.getPositionY(), angle, parent.getSize());
		b.setYUVDelta(deltas);
		b.setHorizontal(hor);
		b.setVertical(ver);
		parent.setLink(b);
	}
	
	private static void generateVector(final MacroBlock parent) {
		double[][][] deltas = MatrixOperations.generateRandom3DMatrix(parent.getSize(), 40);
		
		EncodingVector v = new EncodingVector(parent.getPositionX(), parent.getPositionY(), parent.getSize());
		v.setSpanX(MathUtils.round(random.nextDouble() * 48));
		v.setSpanY(MathUtils.round(random.nextDouble() * 48));
		v.setReference(MathUtils.round(random.nextDouble() * config.MAX_REFERENCES));
		v.setYUVDelta(deltas);
		parent.setLink(v);
	}
}
