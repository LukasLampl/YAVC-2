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

import java.awt.Dimension;
import java.util.LinkedList;

import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.intraprediction.IntraPredictionBlock;
import app.exceptions.CorruptedFileException;
import app.filter.Deblocker;
import app.managers.ListManager;
import app.managers.LoadDistributor;
import app.managers.ReferenceFrameManager;
import app.rendering.RenderEngine;
import app.utils.PixelRaster;

/**
 * The {@code InputProcessor} class processes byte based input from
 * a YAVC coded file and turns it into individual frames.
 * 
 * @author Lukas Lampl
 * @since 1.5
 */
public class InputProcessor {
	/**
	 * The current frame count.
	 */
	public static int FrameCount = 0;
	
	/**
	 * Dimension of all frames.
	 */
	private Dimension FRAME_DIM = null;
	
	/**
	 * The individual lengths of each frame/frame part in the
	 * coded YAVC file.
	 */
	private LinkedList<Integer> lengthOfFrames = new LinkedList<Integer>();
	
	/**
	 * Gets the metadata out of a byte array.
	 * 
	 * @param stream	The byte array containing the metadata.
	 */
	public void proessMetadata(byte[] stream) {
		Metadata meta = Protocol.setMetadata(stream);
		this.FRAME_DIM = meta.getDimensionOfFrames();
		FrameCount = meta.getFrameNumber();
		System.out.println("DIM: " + this.FRAME_DIM);
		System.out.println("FRAMES: " + FrameCount);
	}
	
	/**
	 * Get the length of the part, where the length of each frame part
	 * is stored.
	 * 
	 * @param stream	The byte array containing the size.
	 * @return Number of frame parts.
	 */
	public int initFrameReader(byte[] stream) {
		return ProtocolBase.getSizeFromBytes(stream);
	}
	
	/**
	 * Get the start index of each frame part from a byte array.
	 * 
	 * @param stream	The byte array containing the frame part lengths.
	 */
	public void getIndexes(byte[] stream) {
		Protocol.setLengthsOfEachFramePart(stream, this.lengthOfFrames);
	}
	
	/**
	 * Tries to get the next length from the {@link #lengthOfFrames} queue.
	 * 
	 * @return The length of the next part.
	 * 
	 * @throws IllegalStateException	When no lengths are set or available.
	 */
	public int getNextLength() {
		if (this.lengthOfFrames.isEmpty()) {
			throw new IllegalStateException("Cannot provide data reader, since all markers are missing! (Indexes)");
		}
		
		return this.lengthOfFrames.poll();
	}
	
	/**
	 * Creates the start frame of the video from a byte array.
	 * 
	 * @param data	The start frame data.
	 * @return The constructed start frame.
	 */
	public PixelRaster constructStartFrame(byte[] data) {
		return Protocol.reconstructStartFrame(data, this.FRAME_DIM);
	}
	
	/**
	 * Converts a content byte array (vector byte array) and a raw block byte array
	 * (non-coded block byte array) to a PixelRaster which represents a frame.
	 * 
	 * @param content			The vectors to decode.
	 * @param rawBlocks			The non-coded blocks to decode.
	 * @param refs				All reference frames untill now.
	 * @param vectorListManager	List manager in which to store and get the vectors from.
	 * @return A PixelRaster with all non-coded blocks and vectors resolved.
	 * @throws CorruptedFileException	When either the decoded vector size or non-coded
	 * block size is unequal to the coded size.
	 */
	public PixelRaster processFrame(byte[] content, byte[] intraBlocksContent, ReferenceFrameManager refs,
			ListManager<Vector> vectorListManager,
			ListManager<IntraPredictionBlock> intraBlockManager) throws CorruptedFileException {
		long start_copy = System.currentTimeMillis();
		PixelRaster render = refs.getLastFrame().copy();
		long end_copy = System.currentTimeMillis();
		Deblocker deblocker = new Deblocker();
		long start_get_vecs = System.currentTimeMillis();
		getVectors(content, vectorListManager, false);
		long end_get_vecs = System.currentTimeMillis();
		long start_raw_block = System.currentTimeMillis();
		getIntraPreditionBlocks(intraBlocksContent, intraBlockManager, false);
		long end_raw_block = System.currentTimeMillis();
		long start_load_dist = System.currentTimeMillis();
		LoadDistributor<Vector> vecManager = new LoadDistributor<Vector>();
		LoadDistributor<IntraPredictionBlock> intraBlocks = new LoadDistributor<IntraPredictionBlock>();
		vecManager.setAllAndCompute(vectorListManager.getList());
		intraBlocks.setAllAndCompute(intraBlockManager.getList());
		long end_load_dist = System.currentTimeMillis();
		
		long start_render = System.currentTimeMillis();
		render = RenderEngine.renderComposit(vecManager, refs, intraBlocks, false);
		deblocker.deblock(vecManager, render);
		long end_render = System.currentTimeMillis();

		System.out.println("   > Copy time: " + (end_copy - start_copy) + "ms");
		System.out.println("   > Convert to vector time: " + (end_get_vecs - start_get_vecs) + "ms");
		System.out.println("   > Convert intra blocks time: " + (end_raw_block - start_raw_block) + "ms");
		System.out.println("   > Load distribution time: " + (end_load_dist - start_load_dist) + "ms");
		System.out.println("   > Full rendering time: " + (end_render - start_render) + "ms");
		return render;
	}
	
	/**
	 * Get all non-coded block out of an byte array.
	 * 
	 * @param rawBlocks	Byte array with the non-coded blocks.
	 * @return An ArrayList with all non-coded blocks.
	 * @throws CorruptedFileException	When the decoded non-coded blocks size does not match with the coded size.
	 */
	private void getIntraPreditionBlocks(byte[] rawBlocksPart, ListManager<IntraPredictionBlock> intraBlockListManager, boolean singleThread) throws CorruptedFileException {
		Protocol.getIntraBlocks(rawBlocksPart, intraBlockListManager, singleThread);
	}
	
	/**
	 * Get all vectors of a frame out of an byte array.
	 * 
	 * @param vectorPart		The byte array with the vectors.
	 * @param vectorListManager	A ListManager in which to add the vectors to.
	 * @param singleThread		Flag for whether the process should be single threaded (retains order).
	 * @throws CorruptedFileException	When the decoded vector size is not equal to the coded size.
	 */
	protected void getVectors(byte[] vectorPart, ListManager<Vector> vectorListManager, boolean singleThread) throws CorruptedFileException {
		Protocol.getVectors(vectorPart, vectorListManager, singleThread);
	}
	
	/**
	 * Gets the Dimension of all frames.
	 * 
	 * @return The Dimension of all frames.
	 */
	public Dimension getSizeOfFrames() {
		return this.FRAME_DIM;
	}
}
