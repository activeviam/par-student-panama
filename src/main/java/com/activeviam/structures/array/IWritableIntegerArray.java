package com.activeviam.structures.array;

import com.activeviam.structures.array.impl.IntegerArray;

/**
 * An {@link IntegerArray} whose content can be changed.
 *
 * @author ActiveViam
 */
public interface IWritableIntegerArray extends IIntegerArray {

	/**
	 * Sets a value at the given index.
	 *
	 * @param index The index
	 * @param value The value to set
	 */
	void set(int index, int value);

}
