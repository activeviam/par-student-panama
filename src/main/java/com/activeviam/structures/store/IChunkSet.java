package com.activeviam.structures.store;

/**
 * A chunk set represents a set of chunks that correspond to the columns of a {@link ITable store}.
 *
 * @author ActiveViam
 */
public interface IChunkSet {

	int readInt(int row, int column);
	double readDouble(int row, int column);

	void writeInt(int row, int column, int value);
	void writeDouble(int row, int column, double value);

	long sizeInBytes();

}
