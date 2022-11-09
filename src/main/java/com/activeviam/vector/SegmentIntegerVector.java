/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.Types;
import com.activeviam.allocator.AllocationType;
import com.activeviam.chunk.ADirectVectorBlock;
import com.activeviam.chunk.IBlock;
import com.activeviam.chunk.SegmentIntegerBlock;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;

/**
 * Basic implementation for an integer segment block vector.
 *
 * @author ActiveViam
 */
public class SegmentIntegerVector extends ASegmentVector {

	/**
	 * Constructor.
	 *
	 * @param block the block on which to create the vector
	 * @param position the position in the block
	 * @param length the length of the vector
	 */
	public SegmentIntegerVector(SegmentIntegerBlock block, int position, int length) {
		super(block, position, length);
	}

	@Override
	public AllocationType getAllocation() {
		return this.block.getAllocation();
	}

	@Override
	public ITransientVector sort() {
		final int[] a = toIntArray();
		Arrays.sort(a);
		return new ArrayIntegerVector(a);
	}

	@Override
	public Types getComponentType() {
		return Types.INTEGER;
	}

	@Override
	public void copyFrom(int[] src) {
		checkIndex(0, src.length);
		this.block.write(position, src);
	}

	@Override
	public void copyFrom(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);
		var segVector = (SegmentIntegerVector) vector;
		MemorySegment.copy(
			segVector.block.getSegment(),
			(long) segVector.position * 4,
			block.getSegment(),
			(long) position * 4,
			(long) length * 4);
	}

	@Override
	public Integer read(final int index) {
		return readInt(index);
	}

	@Override
	public int readInt(final int index) {
		checkIndex(index);
		return this.block.readInt(this.position + index);
	}

	@Override
	public long readLong(final int index) {
		return readInt(index);
	}

	@Override
	public float readFloat(final int index) {
		return readInt(index);
	}

	@Override
	public double readDouble(final int index) {
		return readInt(index);
	}

	@Override
	public void write(final int index, final Object value) {
		if (value instanceof Number) {
			writeInt(index, ((Number) value).intValue());
		}
	}

	@Override
	public void writeInt(final int index, final int value) {
		checkIndex(index);
		this.block.writeInt(this.position + index, value);
	}

	@Override
	public void addInt(final int position, final int addedValue) {
		writeInt(position, readInt(position) + addedValue);
	}

	@Override
	public double average() {
		return (double) sumInt() / this.length;
	}

	@Override
	protected double squaredEuclideanDistance(final IBlock block, final int position) {
		int s = 0;
		for (int i = 0; i < this.length; i++) {
			final int a = block.readInt(position + i);
			s += a * a;
		}
		return s;
	}

	@Override
	public IVector cloneOnHeap() {
		return new ArrayIntegerVector(toIntArray());
	}
	
	public static final int VECTOR_STEP = SegmentIntegerBlock.VECTOR_SPECIES.length();
	
	protected IntVector getSimd(int i) {
		return ((SegmentIntegerBlock) block).getSimd(position + i, position + length);
	}
	protected void putSimd(int i, IntVector vec) {
		((SegmentIntegerBlock) block).putSimd(position + i, position + length, vec);
	}

	@Override
	public void plus(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);
		var right = (SegmentIntegerVector) vector;
		for(int i = 0; i < length; i += VECTOR_STEP) {
			putSimd(i, getSimd(i).add(right.getSimd(i)));
		}
	}

	@Override
	public void plusPositiveValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);
		var right = (SegmentIntegerVector) vector;
		for(int i = 0; i < length; i += VECTOR_STEP) {
			putSimd(i, getSimd(i).add(right.getSimd(i).max(0)));
		}
	}

	@Override
	public void plusNegativeValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);
		var right = (SegmentIntegerVector) vector;
		for(int i = 0; i < length; i += VECTOR_STEP) {
			putSimd(i, getSimd(i).add(right.getSimd(i).min(0)));
		}
	}

	@Override
	public void minus(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);
		var right = (SegmentIntegerVector) vector;
		for(int i = 0; i < length; i += VECTOR_STEP) {
			putSimd(i, getSimd(i).sub(right.getSimd(i)));
		}
	}

	@Override
	public void minusPositiveValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);
		var right = (SegmentIntegerVector) vector;
		for(int i = 0; i < length; i += VECTOR_STEP) {
			putSimd(i, getSimd(i).sub(right.getSimd(i).max(0)));
		}
	}

	@Override
	public void minusNegativeValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);
		var right = (SegmentIntegerVector) vector;
		for(int i = 0; i < length; i += VECTOR_STEP) {
			putSimd(i, getSimd(i).sub(right.getSimd(i).min(0)));
		}
	}

}
