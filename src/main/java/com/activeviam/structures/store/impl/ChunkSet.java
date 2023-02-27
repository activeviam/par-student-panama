package com.activeviam.structures.store.impl;

import com.activeviam.structures.store.IChunkSet;
import java.util.Arrays;

/**
 * @author ActiveViam
 */
public class ChunkSet implements IChunkSet {

	/** The value that identifies an empty spot */
	protected static final int EMPTY_VALUE = -1;
	
	protected final int chunkSize;
	protected final int[][] attributes;
	protected final double[][] values;

	public ChunkSet(int attributes, int values, int chunkSize) {
		this.attributes = new int[attributes][chunkSize];
		this.values = new double[values][chunkSize];
		this.chunkSize = chunkSize;
		
		for (int i = 0; i < attributes; ++i) {
			Arrays.fill(this.attributes[i], EMPTY_VALUE);
		}
	}

	@Override
	public int readInt(int row, int column) {
		return attributes[column][row];
	}

	@Override
	public double readDouble(int row, int column) {
		return values[column][row];
	}

	@Override
	public void writeInt(int row, int column, int value) {
		this.attributes[column][row] = value;
	}

	@Override
	public void writeDouble(int row, int column, double value) {
		this.values[column][row] = value;
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
