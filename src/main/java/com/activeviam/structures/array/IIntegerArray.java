package com.activeviam.structures.array;

/**
 * Interface for an integer array.
 *
 * @author ActiveViam
 */
public interface IIntegerArray {

	/**
	 * Returns the value stored at the given
	 * index, or the {@link #getEmptyValue() empty
	 * value} if there is no value stored there.
	 *
	 * @param index An index
	 * @return The value stored at that index
	 */
	int getValue(int index);

	/**
	 * @return The size of this array
	 */
	int size();

	/**
	 * Returns the value that marks an empty spot
	 * in this array.
	 *
	 * @return The value that marks an empty spot
	 * in this array.
	 */
	int getEmptyValue();

}
