package app.decoder;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedList;

import app.encoder.LoadDistributor;
import app.exceptions.CorruptedFileException;
import app.exceptions.WrongBlockAssignedException;
import app.interprediction.ListManager;
import app.interprediction.Vector;
import app.utils.Deblocker;
import app.utils.MacroBlock;
import app.utils.Metadata;
import app.utils.PixelRaster;
import app.utils.Protocol;
import app.utils.ReferenceFrameManager;
import app.utils.RenderEngine;

public class InputProcessor {
	public static int FrameCount = 0;
	private Dimension FRAME_DIM = null;
	private LinkedList<Integer> lengthOfFrames = new LinkedList<Integer>();
	
	public void proessMetadata(byte[] stream) {
		Metadata meta = Protocol.setMetadata(stream);
		this.FRAME_DIM = meta.getDimensionOfFrames();
		FrameCount = meta.getFrameNumber();
		System.out.println("DIM: " + this.FRAME_DIM);
		System.out.println("FRAMES: " + FrameCount);
	}
	
	public int initFrameReader(byte[] stream) {
		return Protocol.getIntFromBytes(stream);
	}
	
	public void getIndexes(byte[] stream) {
		Protocol.setLengthsOfEachFramePart(stream, this.lengthOfFrames);
	}
	
	public int getNextLength() {
		if (this.lengthOfFrames.isEmpty()) {
			throw new IllegalStateException("Cannot provide data reader, since all markers are missing! (Indexes)");
		}
		
		return this.lengthOfFrames.poll();
	}
	
	public PixelRaster constructStartFrame(byte[] data) {
		return Protocol.reconstructStartFrame(data, this.FRAME_DIM);
	}
	
	public PixelRaster processFrame(byte[] content, byte[] rawBlocks, ReferenceFrameManager refs, ListManager<Vector> vectorListManager) throws CorruptedFileException, WrongBlockAssignedException {
		long start_copy = System.currentTimeMillis();
		PixelRaster render = refs.getLastFrame().copy();
		long end_copy = System.currentTimeMillis();
		Deblocker deblocker = new Deblocker();
		long start_get_vecs = System.currentTimeMillis();
		getVectors(content, vectorListManager, false);
		long end_get_vecs = System.currentTimeMillis();
		long start_raw_block = System.currentTimeMillis();
		ArrayList<MacroBlock> blocks = getRawBlocks(rawBlocks);
		long end_raw_block = System.currentTimeMillis();
		long start_load_dist = System.currentTimeMillis();
		LoadDistributor<Vector> vecManager = new LoadDistributor<Vector>();
		LoadDistributor<MacroBlock> blockManager = new LoadDistributor<MacroBlock>();
		vecManager.setAllAndCompute(vectorListManager.getList());
		blockManager.setAllAndCompute(blocks);
		long end_load_dist = System.currentTimeMillis();
		long start_render = System.currentTimeMillis();
		if (vectorListManager.getList() != null) {
			render = RenderEngine.renderResult(vecManager, refs, blockManager, true);
			deblocker.deblock(vecManager, render);
		}
		long end_render = System.currentTimeMillis();

		System.out.println("   > Copy time: " + (end_copy - start_copy) + "ms");
		System.out.println("   > Convert to vector time: " + (end_get_vecs - start_get_vecs) + "ms");
		System.out.println("   > Convert raw-block time: " + (end_raw_block - start_raw_block) + "ms");
		System.out.println("   > Load distribution time: " + (end_load_dist - start_load_dist) + "ms");
		System.out.println("   > Full rendering time: " + (end_render - start_render) + "ms");
		return render;
	}
	
	private ArrayList<MacroBlock> getRawBlocks(byte[] rawBlocks) throws CorruptedFileException, WrongBlockAssignedException {
		return Protocol.getRawBlocks(rawBlocks);
	}
	
	protected void getVectors(byte[] vectorPart, ListManager<Vector> vectorListManager, boolean singleThread) throws CorruptedFileException, WrongBlockAssignedException {
		Protocol.getVectors(vectorPart, vectorListManager, singleThread);
	}
	
	public Dimension getSizeOfFrames() {
		return this.FRAME_DIM;
	}
}
