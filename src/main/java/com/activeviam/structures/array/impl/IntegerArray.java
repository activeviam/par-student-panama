package com.activeviam.structures.array.impl;

import com.activeviam.structures.array.IWritableIntegerArray;
import java.util.Arrays;

/**
 * A writable {@link IWritableIntegerArray} which is not MVCC.
 *
 * @author ActiveViam
 */
public class IntegerArray implements IWritableIntegerArray {

	/** The array's size (as defined in {@link #size()}) */
	protected volatile int size;

	/** The value that identifies an empty spot */
	protected final int emptyValue;

	/** The array containing the values */
	protected int[] data;

	/**
	 * Constructor
	 *
	 * @param emptyValue The value that identifies
	 *        an empty spot.
	 */
	public IntegerArray(final int emptyValue) {
		this.data = new int[16];
		this.size = 0;
		this.emptyValue = emptyValue;
		Arrays.fill(this.data, emptyValue);
	}

	@Override
	public void set(final int index, final int value) {
		// Validate the argument
		if (index < 0) {
			throw new IllegalArgumentException("Index cannot be negative: " + index);
		}
		// Make sure there is enough room in the array
		ensureCapacity(index + 1);
		data[index] = value;

		setSize(Math.max(index + 1, size()));
	}

	/**
	 * Ensures that the values structure has at least the given capacity.
	 *
	 * @param capacity The minimum target capacity.
	 */
	protected void ensureCapacity(final int capacity) {
		if (capacity <= data.length) {
			return;
		}

		int newLength = data.length;
		// Double the array's length until capacity is reached
		while (capacity > newLength) {
			newLength <<= 1;
			if (newLength < 0) {
				throw new IllegalArgumentException("Cannot ensure the capacity of " + this
						+ ". The maximum size allowed is " + (newLength >>> 1));
			}
		}

		final int[] newData = Arrays.copyOf(data, newLength);
		Arrays.fill(newData, data.length, newLength, emptyValue);
		this.data = newData;
	}

	@Override
	public int getValue(final int key) {
		return data[key];
	}

	@Override
	public int getEmptyValue() {
		return emptyValue;
	}

	@Override
	public int size() {
		return size;
	}

	/**
	 * Updates the size of the integer array.
	 *
	 * @param size The new size of this array
	 */
	protected void setSize(final int size) {
		this.size = size;
	}

	/**
	 * Gets the total memory used by the array.
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 * @return estimated size (in bytes) of the array
	 */
	public long getSizeInBytes() {
		// 16: Object header
		// 4: Size attribute
		// 4: emptyValue attribute
		// 4: reference to int[] array
		// 12 : header of the int[] array
		// content of int[]
		return 16 + 4 + 4 + 4 +12 + data.length * 4;
	}

}
