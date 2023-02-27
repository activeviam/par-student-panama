package com.activeviam.structures.bitmap.impl;

import com.activeviam.structures.bitmap.IBitmap;
import java.util.BitSet;
import java.util.stream.IntStream;

/**
 * {@link IBitmap} implementation based on {@link BitSet}.
 *
 * @author ActiveViam
 */
public class BitSetBitmap implements IBitmap {

	protected final BitSet underlying;

	/**
	 * Constructor
	 */
	public BitSetBitmap() {
		this(new BitSet());
	}

	/**
	 * Constructor
	 *
	 * @param initialCapacity the initial size of the bit set
	 */
	public BitSetBitmap(int initialCapacity) {
		this(new BitSet(initialCapacity));
	}

	/**
	 * Constructor
	 *
	 * @param underlying the content of this bitmap
	 */
	public BitSetBitmap(BitSet underlying) {
		this.underlying = underlying;
	}

	@Override
	public void set(int i) {
		underlying.set(i);
	}

	@Override
	public boolean get(int i) {
		return underlying.get(i);
	}

	@Override
	public void clear() {
		underlying.clear();
	}

	@Override
	public boolean isEmpty() {
		return underlying.isEmpty();
	}

	@Override
	public void truncate(int newSize) {
		if (newSize > underlying.size()) {
			throw new IllegalArgumentException("The new size cannot be greater than " + underlying.size());
		}
		underlying.clear(newSize, underlying.size());
	}

	@Override
	public IBitmap and(IBitmap other) {
		final BitSetBitmap o = (BitSetBitmap) other;
		final BitSet bitset = new BitSet(Math.min(underlying.length(), o.underlying.length()));
		final BitSetBitmap r = new BitSetBitmap(bitset);

		and(this, o, r);
		return r;
	}

	@Override
	public IBitmap or(IBitmap other) {
		final BitSetBitmap o = (BitSetBitmap) other;
		final BitSet bitset = new BitSet(Math.max(underlying.length(), o.underlying.length()));
		final BitSetBitmap r = new BitSetBitmap(bitset);

		or(this, o, r);
		return r;
	}

	@Override
	public void and(IBitmap operand, IBitmap result) {
		and(this, (BitSetBitmap) operand, (BitSetBitmap) result);
	}

	@Override
	public void or(IBitmap operand, IBitmap result) {
		or(this, (BitSetBitmap) operand, (BitSetBitmap) result);
	}

	/**
	 * Performs a logical AND operation between two bitmaps.
	 *
	 * @param bitmap1 the first operand
	 * @param bitmap2 the second operand
	 * @param result the bitmap to which the result is appended
	 */
	private static void and(final BitSetBitmap bitmap1, final BitSetBitmap bitmap2,
			final BitSetBitmap result) {
		result.underlying.or(bitmap1.underlying);
		result.underlying.and(bitmap2.underlying);
	}

	/**
	 * Performs a logical OR operation between two bitmaps.
	 *
	 * @param bitmap1 the first operand
	 * @param bitmap2 the second operand
	 * @param result the bitmap to which the result is appended
	 */
	private static void or(final BitSetBitmap bitmap1, final BitSetBitmap bitmap2,
			final BitSetBitmap result) {
		result.underlying.or(bitmap1.underlying);
		result.underlying.or(bitmap2.underlying);
	}

	@Override
	public long sizeInBytes() {
		// 16 : object header
		// 8 : reference to BitSet
		// 16 : header of BitSet
		// 32 : value of the BitSet.wordsInUse
		// 8 : reference to BitSet.long[]
		// 16 : header of the BitSet.long[] array
		// content of the BitSet.long[]
		return 16 + 8 + 16 +32 + 8 + 16 + (underlying.size() / 8);
	}

	@Override
	public IntStream stream() {
		return underlying.stream();
	}

	@Override
	public BitSetBitmap clone() {
		return new BitSetBitmap((BitSet) underlying.clone());
	}

	@Override
	public String toString() {
		return "BitSetBitmap [" + underlying + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((underlying == null) ? 0 : underlying.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BitSetBitmap other = (BitSetBitmap) obj;
		if (underlying == null) {
			if (other.underlying != null)
				return false;
		} else if (!underlying.equals(other.underlying))
			return false;
		return true;
	}

}
