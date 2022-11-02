/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.UnsafeUtil;
import com.activeviam.block.IBlock;
import com.activeviam.iterator.IPrimitiveIterator;
import com.activeviam.reference.TombStoneBlock;
import java.io.ObjectStreamException;

/**
 * Implementation of a vector that is based on blocks to retrieve its underlying data.
 *
 * @author ActiveViam
 */
public abstract class AFixedBlockVector extends AVector {

	/**
	 * Counter of reference to this vector (i.e. the number of chunks it is written on). If the counter reaches
	 * zero, this vector is considered as dead, it will drop the acquire it has taken on its own block at the
	 * allocation time.
	 */
	protected volatile int refCount;

	/** Offset of the refCount field for CAS. */
	protected static final long REF_COUNT_FIELD_OFFSET = UnsafeUtil.getFieldOffset(AFixedBlockVector.class, "refCount");

	protected final int position;
	protected final int length;
	protected IBlock block;

	/**
	 * Constructor.
	 *
	 * @param block the block holding the data
	 * @param position the position at which the vector starts in the block
	 * @param length the number of components in the vector
	 */
	protected AFixedBlockVector(IBlock block, int position, int length) {
		this.block = block;
		this.position = position;
		this.length = length;
	}

	@Override
	public void acquireReference() {
		// Do CAS because a vector can be concurrently written on multiple chunks
		UnsafeUtil.getAndAddInt(this, REF_COUNT_FIELD_OFFSET, 1);
	}

	@Override
	public void releaseReference() {
		// Do CAS because a vector can be concurrently removed on multiple chunks
		final int prev = UnsafeUtil.getAndAddInt(this, REF_COUNT_FIELD_OFFSET, -1);
		final int next = prev - 1;
		// This vector is not on any chunk, so it can be destroyed
		if (next == 0) {
			final IBlock oldBlock;
			// When running compression we are not necessarily in the same thread as the ones calling collect().
			// Whence the need for synchronization.
			synchronized (this) {
				oldBlock = block;
				this.block = TombStoneBlock.INSTANCE;
			}
			assert oldBlock != TombStoneBlock.INSTANCE : "Releasing an already released vector!";
			oldBlock.release(size());
		} else if (next < 0) {
			throw new IllegalStateException("Releasing a vector that is never acquired!");
		}
	}

	@Override
	public int size() {
		return this.length;
	}

	@Override
	public Object read(final int index) {
		return this.block.read(index);
	}

	@Override
	public void write(final int index, final Object value) {
		this.block.write(index, value);
	}

	@Override
	public void fillInt(final int v) {
		this.block.fillInt(this.position, length, v);
	}

	@Override
	public void fillLong(final long v) {
		this.block.fillLong(this.position, length, v);
	}

	@Override
	public void fillFloat(final float v) {
		this.block.fillFloat(this.position, length, v);
	}

	@Override
	public void fillDouble(final double v) {
		this.block.fillDouble(this.position, length, v);
	}

	@Override
	public void scale(final int v) {
		this.block.scale(this.position, length, v);
	}

	@Override
	public void scale(final long v) {
		this.block.scale(this.position, length, v);
	}

	@Override
	public void scale(final float v) {
		this.block.scale(this.position, length, v);
	}

	@Override
	public void scale(final double v) {
		this.block.scale(this.position, length, v);
	}

	@Override
	public void divide(final int v) {
		this.block.divide(this.position, length, v);
	}

	@Override
	public void divide(final long v) {
		this.block.divide(this.position, length, v);
	}

	@Override
	public void translate(final int v) {
		this.block.divide(this.position, length, v);
	}

	@Override
	public void translate(final long v) {
		this.block.translate(this.position, length, v);
	}

	@Override
	public void translate(final float v) {
		this.block.translate(this.position, length, v);
	}

	@Override
	public void translate(final double v) {
		this.block.translate(this.position, length, v);
	}

	@Override
	public IPrimitiveIterator topK(final int k) {
		checkIndex(0, k);
		if (k == 0) {
			return (IPrimitiveIterator) EmptyVector.emptyVector(getComponentType());
		}
		return block.topK(this.position, length, k);
	}

	@Override
	public IPrimitiveIterator bottomK(final int k) {
		checkIndex(0, k);
		if (k == 0) {
			return (IPrimitiveIterator) EmptyVector.emptyVector(getComponentType());
		}
		return block.bottomK(this.position, length, k);
	}

	@Override
	public IVector subVector(final int start, final int length) {
		if (length == 0) {
			return EmptyVector.emptyVector(getComponentType());
		}
		checkIndex(start, length);
		return createVector(this.block, start, length);
	}

	@Override
	public int hashCode() {
		return block.hashCode(0, length);
	}

	/**
	 * Creates a {@link AFixedBlockVector} version of this vector.
	 *
	 * @param block the block on which to create the vector
	 * @param position the position in the block
	 * @param length the length of the vector
	 * @return a {@link AFixedBlockVector} version of this vector
	 */
	protected abstract AFixedBlockVector createVector(IBlock block, int position, int length);

	@Override
	public int[] topKIndices(final int k) {
		if (k == 0) {
			return new int[0];
		}
		return this.block.topKIndices(position, length, k);
	}

	@Override
	public int[] bottomKIndices(final int k) {
		if (k == 0) {
			return new int[0];
		}
		return this.block.bottomKIndices(position, length, k);
	}

	@Override
	public double sumDouble() {
		double sum = 0d;
		for (int i = 0; i < length; i++) {
			sum += block.readDouble(position + i);
		}

		return sum;
	}

	@Override
	public float sumFloat() {
		float sum = 0f;
		for (int i = 0; i < length; i++) {
			sum += block.readFloat(position + i);
		}

		return sum;
	}

	@Override
	public long sumLong() {
		long sum = 0L;
		for (int i = 0; i < length; i++) {
			sum += block.readLong(position + i);
		}
		return sum;
	}

	@Override
	public int sumInt() {
		int sum = 0;
		for (int i = 0; i < length; i++) {
			sum += this.block.readInt(position + i);
		}

		return sum;
	}

	@Override
	public double variance() {
		final double average = average();
		return squaredEuclideanDistance(this.block, position) / length - average * average;
	}

	/**
	 * Computes the dot product of the vector and itself.
	 *
	 * @param block block on which the vector coordinates will be read
	 * @param position position from which the vector coordinates can be read in the given block
	 * @return the value of the squared Euclidean norm of the vector
	 */
	protected abstract double squaredEuclideanDistance(IBlock block, int position);

	@Override
	public double quantileDouble(final double r) {
		return this.block.quantileDouble(position, length, r);
	}

	@Override
	public float quantileFloat(final double r) {
		return this.block.quantileFloat(position, length, r);
	}

	@Override
	public long quantileLong(final double r) {
		return this.block.quantileLong(position, length, r);
	}

	@Override
	public int quantileInt(final double r) {
		return this.block.quantileInt(position, length, r);
	}

	@Override
	public int quantileIndex(final double r) {
		return this.block.quantileIndex(position, length, r);
	}

	/**
	 * Replaces the vector with its array version when serializing.
	 *
	 * @return an on-heap array clone of this vector
	 * @throws ObjectStreamException failure
	 */
	protected Object writeReplace() throws ObjectStreamException {
		return cloneOnHeap();
	}

}
