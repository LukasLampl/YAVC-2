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

package app.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The {@code ListManager} is a class that allows to store a huge amount of
 * data and reaccess it to modify it and reduce the object creation and thus
 * reduce GC (Garbage Collection).
 * 
 * <p><b>Note:</b><br>
 * To use already used objects, referred as "cached", the cached one has to be
 * received by calling {@link #getCachedObj()}. Be careful though, since it might
 * return {@code null}, if no object is in the cache anymore. To add the cached object
 * to the current list, just call {@link #add(Object)}. <ul>Remember</ul> to change
 * the data in the object before adding it again, since there is no such mechanism like
 * "reset"!
 * </p>
 * 
 * @param <T> The Type that should be cached.
 * 
 * @author Lukas Lampl
 * @since 1.2.2 [Optimized prototype]
 */
public class ListManager<T> {
	/**
	 * Holds all already used objects, if empty non were used till then.
	 */
	private ConcurrentLinkedQueue<T> oldList = new ConcurrentLinkedQueue<T>();
	
	/**
	 * Holds all current objects.
	 */
	private List<T> list = Collections.synchronizedList(new ArrayList<T>());
	
	public ListManager() {}
	
	/**
	 * Switches the new list to the old and creates a new empty one for the current list.
	 * This is to keep track of all created items in order to use them again.
	 */
	public void switchList() {
		this.oldList.addAll(this.list);
		this.list = Collections.synchronizedList(new ArrayList<T>());
	}
	
	/**
	 * Returns an object that has been used already and can be filled
	 * with new data.
	 * 
	 * @return	An object that can be filled with new data. If the cache runs out
	 * it return null and a new Object has to be initialized.
	 */
	public T getCachedObj() {
		return this.oldList.poll();
	}
	
	/**
	 * Adds a single object to the current list.
	 * 
	 * @param obj	The object to add.
	 */
	public void add(final T obj) {
		if (obj == null) {
			return;
		}
		
		this.list.add(obj);
	}
	
	/**
	 * Adds a whole collection to the current list.
	 * 
	 * @param collection	The collection to add.
	 */
	public void addAll(final Collection<? extends T> collection) {
		this.list.addAll(collection);
	}
	
	/**
	 * Get the Object at the specified position.
	 * 
	 * @param index	The position from which to get the object from.
	 * @return The object at the position.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	When the given index is bigger than the max size or below 0.
	 */
	public T get(final int index) {
		if (index < 0 || index >= this.list.size()) {
			throw new ArrayIndexOutOfBoundsException("Index out of bounds " + index + " for " + this.list.size() + ".");
		}
		
		return this.list.get(index);
	}
	
	/**
	 * Get the current list of the ListManager.
	 * 
	 * @return The current list.
	 */
	public List<T> getList() {
		return this.list;
	}
}
