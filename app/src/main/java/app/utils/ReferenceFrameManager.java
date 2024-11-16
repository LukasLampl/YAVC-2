/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

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

package app.utils;

import java.util.ArrayList;

import app.config;

/**
 * <p>The class {@code ReferenceFrameManager} provides functionalities
 * for managing reference frames for the YAVC video en- and decoder.</p>
 * 
 * <p>The reference manager will restrict the size of reference frames
 * to a maximum of the configured {@code MAX_REFERENCES} in the {@link app.config#MAX_REFERENCES}.
 * </p>
 * 
 * @author Lukas Lampl
 * @since 1.2 [Optimized prototype]
 * @see app.config
 */
public class ReferenceFrameManager {
	/**
	 * Holds the reference frames, so other frames can reference to
	 * previously encoded/decoded frames.
	 */
	private ArrayList<PixelRaster> references = new ArrayList<PixelRaster>(config.MAX_REFERENCES);
	
	/**
	 * Holds the last invoked frame by the function {@link #add(PixelRaster)}.
	 */
	private PixelRaster lastFrame = null;
	
	public ReferenceFrameManager() {}
	
	/**
	 * Adds a PixelRaster as a reference frame to the {@link #references} list.
	 * If the added frame causes a overflow in size, which is restricted
	 * by the {@link app.config#MAX_REFERENCES}, the reference list will be
	 * recalculated.
	 * 
	 * @see #manageSize()
	 * @param frame	The frame to add as a reference.
	 */
	public void add(PixelRaster frame) {
		if (this.lastFrame != null) {
			this.lastFrame.lock();
		}
		
		this.references.add(frame);
		this.lastFrame = frame;
		manageSize();
	}
	
	/**
	 * Manages the size of the references by removing the head reference
	 * of the {@link #references}, when the maximum size of reference frames
	 * is exceeded (restricted by {@link app.config#MAX_REFERENCES}).
	 */
	private void manageSize() {
		if (this.references.size() <= config.MAX_REFERENCES) {
			return;
		}
		
		this.references.remove(0);
	}
	
	/**
	 * Returns the reference frame at the given index {@code i}.
	 * 
	 * @param index	The index from which to get the frame from.
	 * @return The reference frame at the specified index.
	 */
	public PixelRaster get(int index) {
		return this.references.get(index);
	}
	
	/**
	 * Returns the last added reference frame, which is modified by
	 * the {@link #add(PixelRaster)} function.
	 * 
	 * @return The last added reference frame.
	 */
	public PixelRaster getLastFrame() {
		return this.lastFrame;
	}
	
	/**
	 * Returns the reference frame at the position of {@code MAX_FRAMES - reference}.
	 * 
	 * @param reference	The frames to go back until the reference.
	 * @return The reference frame.
	 */
	public PixelRaster getByReference(int reference) {
		return this.references.get(config.MAX_REFERENCES - reference);
	}
	
	/**
	 * Return the current number of references which can be accessed.
	 * 
	 * @return The number of available reference frames.
	 */
	public int size() {
		return this.references.size();
	}
}
