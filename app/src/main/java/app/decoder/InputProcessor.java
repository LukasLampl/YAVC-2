package app.decoder;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;

import app.encoder.LoadDistributor;
import app.exceptions.CorruptedFileException;
import app.exceptions.WrongBlockAssignedException;
import app.interprediction.Vector;
import app.utils.ColorManager;
import app.utils.Deblocker;
import app.utils.MacroBlock;
import app.utils.PixelRaster;
import app.utils.Protocol;
import app.utils.ReferenceFrameManager;
import app.utils.RenderEngine;

public class InputProcessor {
	public static int FrameCount = 0;
	private Dimension FRAME_DIM = null;
	private ArrayList<Integer> lengthOfFrames = new ArrayList<Integer>();
	
	public void proessMetadata(byte[] stream) {
		if (stream.length < Protocol.META_DATA_LEN) {
			throw new IllegalArgumentException("Metadata has to be " + 4 + " bytes long.");
		}
		
		byte[][] parts = Protocol.splitArrayEvenly(stream, Protocol.SIZE_OF_INT);
		int width = Protocol.getIntFromBytes(parts[0]);
		int height = Protocol.getIntFromBytes(parts[1]);
		int frames = Protocol.getIntFromBytes(parts[2]);

		this.FRAME_DIM = new Dimension(width, height);
		FrameCount = frames;
		System.out.println("DIM: " + this.FRAME_DIM);
		System.out.println("FRAMES: " + frames);
	}
	
	public int initFrameReader(byte[] stream) {
		return Protocol.getIntFromBytes(stream);
	}
	
	public void getIndexes(byte[] stream) {
		byte[][] data = Protocol.splitArrayEvenly(stream, Protocol.SIZE_OF_INT);
		
		for (byte[] byteNum : data) {
			int length = Protocol.getIntFromBytes(byteNum);
			this.lengthOfFrames.add(length);
		}
	}
	
	public int getNextLength() {
		return this.lengthOfFrames.remove(0);
	}
	
	public PixelRaster constructStartFrame(byte[] data) {
		PixelRaster render = new PixelRaster(this.FRAME_DIM);

		for (int x = 0, index = 0; x < this.FRAME_DIM.width; x++) {
			for (int y = 0; y < this.FRAME_DIM.height; y++) {
				byte r = data[index];
				byte g = data[index + 1];
				byte b = data[index + 2];
				int rgb = (0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
				double[] YUV = ColorManager.convertRGBToYUV(rgb);
				render.setYUV(x, y, YUV);
				index += 3;
			}
		}
		
		return render;
	}
	
	public PixelRaster processFrame(byte[] content, byte[] rawBlocks, ReferenceFrameManager refs) throws CorruptedFileException, WrongBlockAssignedException {
		long start_copy = System.currentTimeMillis();
		PixelRaster render = refs.getLastFrame().copy();
		long end_copy = System.currentTimeMillis();
		Deblocker deblocker = new Deblocker();
		long start_get_vecs = System.currentTimeMillis();
		ArrayList<Vector> vecs = content.length > 1 ? getVectors(content) : new ArrayList<Vector>();
		long end_get_vecs = System.currentTimeMillis();
		long start_raw_block = System.currentTimeMillis();
		ArrayList<MacroBlock> blocks = getRawBlocks(rawBlocks);
		long end_raw_block = System.currentTimeMillis();
		long start_load_dist = System.currentTimeMillis();
		LoadDistributor<Vector> vecManager = new LoadDistributor<Vector>();
		LoadDistributor<MacroBlock> blockManager = new LoadDistributor<MacroBlock>();
		vecManager.setAllAndCompute(vecs);
		blockManager.setAllAndCompute(blocks);
		long end_load_dist = System.currentTimeMillis();
		long start_render = System.currentTimeMillis();
		if (vecs != null) {
			render = RenderEngine.renderResult(vecManager, refs, blockManager);
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
		ArrayList<MacroBlock> blocks = new ArrayList<MacroBlock>();
		int i = 0;
		byte[] lenOfBlocks = {rawBlocks[0], rawBlocks[1], rawBlocks[2], rawBlocks[3]};
		int estimatedLength = Protocol.getIntFromBytes(lenOfBlocks);
		i += Protocol.RAW_BLOCK_SIZE_CHECK_LENGTH;
		
		while (i < rawBlocks.length) {
			int posX = Protocol.getPosition(rawBlocks[i], rawBlocks[i + 1]);
			int posY = Protocol.getPosition(rawBlocks[i + 2], rawBlocks[i + 3]);
			int[] sizeBytes = Protocol.getReferenceAndSizeInt(rawBlocks[i + 4]);
			int size = sizeBytes[1];
			MacroBlock block = new MacroBlock(new Point(posX, posY), size, true);
			int length = block.getSquaredSize() * 3;
			int offset = i + Protocol.RAW_BLOCK_HEADER_LENGTH;
			int x = 0;
			int y = 0;
			
			for (int n = offset; n < length + offset; n += 3) {
				int r = rawBlocks[n] & 0xFF;
				int g = rawBlocks[n + 1] & 0xFF;
				int b = rawBlocks[n + 2] & 0xFF;
				int argb = (0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
				block.setYUV(x++, y, ColorManager.convertRGBToYUV(argb));
				
				if (x >= size) {
					x = 0;
					y++;
				}
			}
			
			i += Protocol.RAW_BLOCK_HEADER_LENGTH + length;
			blocks.add(block);
		}
		
		if (blocks.size() != estimatedLength) {
			throw new CorruptedFileException("The amount of the read-in raw-blocks appears to be unequal to the written raw-blocks.");
		}
		
		return blocks;
	}
	
	private ArrayList<Vector> getVectors(byte[] vectorPart) throws CorruptedFileException, WrongBlockAssignedException {
		ArrayList<Vector> vecs = new ArrayList<Vector>();
		
		if (vectorPart.length <= 1) {
			return vecs;
		}
		
		//  LAYOUT:
		//  POSX ⊥ POSY ⊥ SPANX ⊥ SPANY ⊥ REFERENCE << 4 | SIZE ⊥ DIFFERENCE
		// ^_____________________________________________________^
		//                      = 7 Bytes offset
		int i = 0;
		byte[] lenOfVecs = {vectorPart[0], vectorPart[1], vectorPart[2], vectorPart[3]};
		int estimatedLength = Protocol.getIntFromBytes(lenOfVecs);
		i += Protocol.VECTOR_SIZE_CHECK_LENGTH;
		ArrayList<Integer> indexesOfVectors = new ArrayList<Integer>();
		
		while (i < vectorPart.length) {
			indexesOfVectors.add(i);
			int[] refAndSize = Protocol.getReferenceAndSizeInt(vectorPart[i + 6]);
			int size = refAndSize[1];
			//Length of the vector diffs
			i += ((size * size) + 2 * ((size / 2) * (size / 2))) + Protocol.VECTOR_HEADER_LENGTH;
		}
		
		VectorConverter converter = new VectorConverter(vectorPart, indexesOfVectors);
		converter.start();
		vecs = converter.awaitTermination();
		
		if (vecs.size() != estimatedLength) {
			throw new CorruptedFileException("The amount of the read-in vectors appears to be unequal to the written vectors.");
		}
		
		return vecs;
	}
}
