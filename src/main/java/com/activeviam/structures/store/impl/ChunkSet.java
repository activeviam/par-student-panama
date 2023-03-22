package com.activeviam.structures.store.impl;

import com.activeviam.chunk.DoubleChunk;
import com.activeviam.chunk.IChunkAllocator;
import com.activeviam.chunk.IntegerChunk;
import com.activeviam.chunk.SegmentIntegerBlock;
import com.activeviam.structures.store.IChunkSet;
import jdk.incubator.vector.VectorMask;

import java.util.Arrays;
import java.util.BitSet;

/**
 * @author ActiveViam
 */
public class ChunkSet implements IChunkSet {

	/** The value that identifies an empty spot */
	protected static final int EMPTY_VALUE = -1;

	/** The size of a chunk */
	protected final int chunkSize;

	/** The values of the attribute columns */
	protected final IntegerChunk[] attributes;

	/** The values of the value columns */
	protected final DoubleChunk[] values;

	/**
	 * Constructor
	 *
	 * @param attributes Number of attributes
	 * @param values Number of values
	 * @param chunkSize Size of a chunk
	 */
	public ChunkSet(int attributes, int values, int chunkSize, IChunkAllocator allocator) {
		this.attributes = new IntegerChunk[attributes];
		for (int i = 0; i < attributes; i++) {
			this.attributes[i] = allocator.allocateIntegerChunk(chunkSize);
		}
		this.values = new DoubleChunk[values];
		for (int i = 0; i < values; i++) {
			this.values[i] = allocator.allocateDoubleChunk(chunkSize);
		}
		this.chunkSize = chunkSize;
	}

	@Override
	public int readInt(final int row, final int column) {
		return attributes[column].readInt(row);
	}

	@Override
	public double readDouble(final int row, final int column) {
		return values[column].readDouble(row);
	}

	@Override
	public void writeInt(final int row, final int column, final int value) {
		this.attributes[column].writeInt(row, value);
	}

	@Override
	public void writeDouble(final int row, final int column, final double value) {
		this.values[column].writeDouble(row, value);
	}

	@Override
	public BitSet findRows(int[] predicate, int limit) {
		BitSet result = null;

		for (int p = 0; p < predicate.length; p++) {
			final int value = predicate[p];
			if (value < 0) {
				// no condition
				continue;
			}

			final IntegerChunk chunk = attributes[p];
			final BitSet partialResult = chunk.findRows(value, limit);
			if (partialResult != null) {
				if (result == null) {
					result = partialResult;
				} else {
					result.and(partialResult);
				}
				if (result.isEmpty()) {
					return result;
				}

			} else {
				return new BitSet();
			}
		}

		if (null == result) {
			result = new BitSet(limit);
			result.flip(0, limit);
		}
		return result;
	}

	public VectorMask<Integer> findRowsSIMD(int[] predicate, int limit) {
		VectorMask<Integer> m = null;
		for (int p = 0; p < predicate.length; p++) {
			final int value = predicate[p];
			if (value < 0) {
				// no condition
				continue;
			}

			final SegmentIntegerBlock chunk = (SegmentIntegerBlock) attributes[p];
			final VectorMask<Integer> partialResult = chunk.findRowsSIMD(value, limit);
			if (partialResult != null) {
				if (m == null) {
					m = partialResult;
				} else {
					m = m.and(partialResult);
				}
				if (!m.anyTrue()) {
					return m;
				}

			} else {
				return null;
			}
		}
		return m;
	}

	@Override
	public long sizeInBytes() {
		// 16: Object header
		// 8: Reference to the attributes array
		// 8: Reference to the values array
		long sizeInBytes = 16 + 8 + 8;

		// Content to the attributes array
		sizeInBytes += attributes.length * chunkSize * 4;

		// Content to the values array
		sizeInBytes += values.length * chunkSize * 8;

		return sizeInBytes;
	}

}
