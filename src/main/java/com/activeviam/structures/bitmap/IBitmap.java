package com.activeviam.structures.bitmap;

import java.util.stream.IntStream;

/**
 * A bitmap is a possibly large sequence of bits on which logical operations like AND, OR, XOR can
 * be efficiently applied.
 *
 * @author ActiveViam
 */
public interface IBitmap {

	/**
	 * Sets the bit at position i to true. If the bitmap is append only (almost always the case for
	 * compressed bitmaps) the bits must be set in increasing order.
	 *
	 * @param i value to add to this bitmap
	 */
	void set(int i);

	/**
	 * Returns the value of the bit stored at some position.
	 *
	 * @param i value to add to this bitmap
	 * @return bit value at position i
	 */
	boolean get(int i);

	/**
	 * Clears the bitmap, resetting its bit size to zero; the bitmap can then be reused.
	 */
	void clear();

	/**
	 * Clears the bitmap from newSize to the end of the bitmap
	 * @param newSize the new size of the bitmap
	 */
	void truncate(int newSize);

	/**
	 * Performs a logical AND operation between two bitmaps.
	 *
	 * @param other other bitmap operand of the AND operator
	 * @return AND result bitmap
	 */
	IBitmap and(final IBitmap other);

	/**
	 * Performs a logical OR operation between two bitmaps.
	 *
	 * @param other other bitmap operand of the OR operator
	 * @return OR result bitmap
	 */
	IBitmap or(final IBitmap other);

	/**
	 * Performs a logical AND operation between two bitmaps. It is responsibility of the user to
	 * clear the result before using this logic operation.
	 *
	 * @param operand the other bitmap operand
	 * @param result the bitmap where the result is appended
	 *
	 */
	void and(final IBitmap operand, final IBitmap result);

	/**
	 * Performs a logical OR operation between two bitmaps. It is responsibility of the user to clear
	 * the result before using this logic operation.
	 *
	 * @param operand the other bitmap operand
	 * @param result the bitmap where the result is appended
	 *
	 */
	void or(final IBitmap operand, final IBitmap result);

	/**
	 * Gets the total size of the bitmap.
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 * @return estimated size (in bytes) of the bitmap
	 */
	long sizeInBytes();

	/**
	 * @return a stream of integers representing set indices
	 */
	IntStream stream();

	/**
	 * Returns true if this {@code IBitmap} contains no bits that are set to {@code true}.
	 *
	 * @return boolean indicating whether this {@code IBitmap} is empty
	 */
	boolean isEmpty();

	/**
	 * @return a clone of the bitmap
	 */
	IBitmap clone();

}
