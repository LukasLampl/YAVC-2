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

package app.utils.components;

import java.awt.Point;

import app.managers.Discardable;

/**
 * The {@code Component2D} is the super class of all coding units in the YAVC
 * video compressor that consist of 2D operations and data.
 * 
 * @author Lukas Lampl
 * @version 1.0.0 [optimized_prototype_2]
 */
public abstract class Component2D implements Discardable {
	/**
	 * Holds the position in the x direction of the component.
	 */
	protected int positionX = 0;
	
	/**
	 * Holds the position in the y direction of the component.
	 */
	protected int positionY = 0;
	
	/**
	 * Holds the size of the component.
	 */
	protected int size = 0;
	
	/**
	 * Holds the area of the component.
	 */
	protected int area = 0;
	
	/**
	 * Creates a new {@code Component2D} with the given position and size.
	 * 
	 * @param x		The x position of the component.
	 * @param y		The y position of the component.
	 * @param size	The size of the component.
	 */
	public Component2D(final int x, final int y, final int size) {
		this.positionX = x;
		this.positionY = y;
		this.size = size;
		this.area = size * size;
	}
	
	/**
	 * Sets the size of the component.
	 * <p>
	 * <b>WARNING:</b><br>
	 * Setting the size does not include validation of whether the size is valid
	 * or not and might cause problems for later processing.
	 * 
	 * @param size	Size of the component.
	 */
	public void setSize(final int size) {
		this.size = size;
		this.area = size * size;
	}
	
	/**
	 * Gets the size of the component.
	 * 
	 * @return The size of the component.
	 */
	public int getSize() {
		return this.size;
	}
	
	/**
	 * Gets the area of the component.
	 * 
	 * @return The area of the component.
	 */
	public int getArea() {
		return this.area;
	}
	
	/**
	 * Sets the position of the component.
	 * <p>
	 * <b>WARNING:</b><br>
	 * Setting the position does not include validation of whether the position is valid
	 * or not and might cause problems for later processing.
	 * 
	 * @param x	Position in the x direction.
	 * @param y Position in the y direction. 
	 */
	public void move(final int x, final int y) {
		this.positionX = x;
		this.positionY = y;
	}
	
	/**
	 * Gets the x position of the component.
	 * 
	 * @return The position in the x direction.
	 */
	public int getPositionX() {
		return this.positionX;
	}
	
	/**
	 * Gets the y position of the component.
	 * 
	 * @return The position in the y direction.
	 */
	public int getPositionY() {
		return this.positionY;
	}
	
	/**
	 * Gets the position of the component in form of a {@code Point}.
	 * 
	 * @return The position in form of a {@code Point}.
	 */
	public Point getPosition() {
		return new Point(this.positionX, this.positionY);
	}
	
	@Override
	public void discard() {
		setSize(0);
		move(0, 0);
	}
}
