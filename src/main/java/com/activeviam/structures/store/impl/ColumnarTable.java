/*
 * (C) ActiveViam 2007-2019
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.structures.store.impl;

import static java.lang.Math.min;

import com.activeviam.chunk.IChunkAllocator;
import com.activeviam.chunk.OnHeapAllocator;
import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.bitmap.impl.BitSetBitmap;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.IWritableTable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * {@link IWritableTable} with a columnar storage.
 *
 * @author ActiveViam
 */
public class ColumnarTable implements IWritableTable {

	/**
	 * The size of {@link IRecord#getAttributes()}
	 */
	protected final int attributeCount;
	/**
	 * The size of {@link IRecord#getValues()}
	 */
	protected final int valueCount;

	/** IChunk allocator */
	protected final IChunkAllocator allocator;

	/** Data chunks */
	protected ChunkSet[] chunks;

	/** The number of records appended to this table */
	protected int size;

	/** The size of the chunks */
	protected final int chunkSize;
	/** IChunk order, at the power of 2 it is the chunk size */
	protected final int chunkOrder;
	/** Mask to extract positions within chunks */
	protected final int chunkMask;

	protected final ITableWriter writer;

	public ColumnarTable(TableFormat format) {
		this(format, new OnHeapAllocator());
	}

	public ColumnarTable(TableFormat format, IChunkAllocator allocator) {
		this.attributeCount = format.attributeCount;
		this.valueCount = format.valueCount;
		this.chunkSize = format.chunkSize;
		this.allocator = allocator;
		if (Integer.bitCount(this.chunkSize) != 1) {
			throw new IllegalArgumentException("ChunkSize is not a power of 2: " + this.chunkSize);
		}
		this.chunkOrder = getOrder(format.chunkSize);
		this.chunkMask = (1 << this.chunkOrder) - 1;
		this.chunks = new ChunkSet[0];
		this.writer = new TableWriter();
	}

	@Override
	public int size() {
		return size;
	}

	/**
	 * Returns the current capacity of this table
	 *
	 * @return The current capacity of this table
	 */
	public int capacity() {
		final ChunkSet[] chunks = this.chunks;
		final int numChunks = chunks != null ? chunks.length : 0;
		return numChunks << this.chunkOrder;
	}

	/**
	 * Allocates more chunks if needed to reach the
	 * requested capacity.
	 *
	 * <p>
	 *   This method is not thread-safe.
	 * </p>
	 *
	 * @param capacity The target capacity
	 * @return This table's new capacity
	 */
	@Override
	public int ensureCapacity(int capacity) {
		if (capacity < 0) {
			throw new RuntimeException(
					"Required capacity is " + capacity + ". Please increase the number of partitions.");
		}
		if (capacity() < capacity) {
			// Compute the target number of chunks
			final int targetChunkCount = getNumChunks(capacity);

			// Expand the chunks array
			setChunkCount(targetChunkCount);
		}

		// Return the current capacity
		return capacity();
	}

	/**
	 * Gets the number of chunks required to store the given number of records
	 * @param nbrRecords The number of records
	 * @return the number of chunks
	 */
	protected int getNumChunks(int nbrRecords) {
		return 0 == nbrRecords ? 0 : ((nbrRecords - 1) >>> chunkOrder) + 1;
	}

	/**
	 * Sets the number of chunks that are allocated for each column.
	 *
	 * @param numChunks The target number of chunks that are allocated for each column
	 */
	protected void setChunkCount(final int numChunks) {
		final ChunkSet[] oldChunks = this.chunks;
		final int numOldChunks = oldChunks != null ? oldChunks.length : 0;
		// Nothing to do if the number of chunks is already correct
		if (numChunks == numOldChunks) {
			return;
		}

		final ChunkSet[] newChunks = Arrays.copyOf(oldChunks, numChunks);
		for (int i = numOldChunks; i < numChunks; ++i) {
			newChunks[i] = new ChunkSet(attributeCount, valueCount, 1 << chunkOrder, allocator);
		}
		this.chunks = newChunks;
	}

	/**
	 * Returns the smallest integer k such as 2^k &ge; value.
	 * @param value A positive integer
	 * @return The smallest integer k such as 2^k &ge; value
	 */
	public static final int getOrder(final int value) {
		return 32 - Integer.numberOfLeadingZeros(value - 1);
	}

	@Override
	public int readInt(int row, int column) {
		final int chunkId = row >>> this.chunkOrder;
		final int chunkRow = row & this.chunkMask;
		return this.chunks[chunkId].readInt(chunkRow, column);
	}
	
	@Override
	public void readInts(final int row, final int column, final int number, final int[] result) {
		final int chunkId = row >>> this.chunkOrder;
		int chunkRow = row & this.chunkMask;
		assert chunkRow + number <= this.chunkSize : "The readInts method cannot read in more than one chunk.";
		int n = number;
		if (row + number > this.size) {
			n = this.size % number;
			for (int i = n; i < number; ++i) {
				result[i] = -1;
			}
		}
		for (int i = 0; i < n; ++i) {
			result[i] = this.chunks[chunkId].readInt(chunkRow++, column);
		}
	}

	@Override
	public double readDouble(int row, int column) {
		final int chunkId = row >>> this.chunkOrder;
		final int chunkRow = row & this.chunkMask;
		return this.chunks[chunkId].readDouble(chunkRow, column);
	}

	@Override
	public IRecord getRecord(int row) {
		final int chunkId = row >>> this.chunkOrder;
		final int chunkRow = row & this.chunkMask;
		final ChunkSet chunk = this.chunks[chunkId];

		final int[] attributes = new int[attributeCount];
		final double[] values = new double[valueCount];

		for (int i = 0; i < attributeCount; ++i) {
			attributes[i] = chunk.readInt(chunkRow, i);
		}
		for (int i = 0; i < valueCount; ++i) {
			values[i] = chunk.readDouble(chunkRow, i);
		}

		return new Record(attributes, values);
	}

	@Override
	public int append(IRecord record) {
		final int currentSize = this.size;
		ensureCapacity(currentSize + 1);

		this.writer.setRow(currentSize);
		for (int i = 0; i < attributeCount; ++i) {
			this.writer.writeInt(i, record.readInt(i));
		}
		for (int i = 0; i < valueCount; ++i) {
			this.writer.writeDouble(i, record.readDouble(i));
		}

		this.size = currentSize + 1;
		return currentSize;
	}

	@Override
	public void truncate(int newSize) {
		if (newSize >= size) {
			return;
		}

		final int targetChunkCount = getNumChunks(newSize);
		// Reduce the chunks array
		setChunkCount(targetChunkCount);

		size = newSize;
	}

	@Override
	public IBitmap findRows(int[] predicate) {
		final IBitmap result = new BitSetBitmap();
		int rowsToScan = size;
		int c = 0;
		while (rowsToScan > 0) {
			final BitSet localRows = chunks[c].findRows(predicate, min(rowsToScan, chunkSize));
			final int offset = c * chunkSize;
			localRows.stream().forEach(localRow -> result.set(localRow + offset));
			++c;
			rowsToScan -= chunkSize;
		}
		return result;
	}

	public long sizeInBytes() {
		// 16: Object header
		// 4: valueCount, size, chunkSize, chunkOrder, chunkMask attributes
		// 8: Reference to the chunks array
		// 8: Reference to the table writer

		long sizeInBytes = 16 + 6 * 4 + 2 * 8;

		// Content of the chunks array
		for (ChunkSet chunk: chunks) {
			if (null != chunk) {
				sizeInBytes += chunk.sizeInBytes();
			}
		}

		return sizeInBytes;
	}

	/**
	 * Prints the content of the table.
	 */
	public void print() {
		ITableWriter cursor = new TableWriter();
		for (int row = 0; row < size; row++) {
			cursor.setRow(row);
			System.out.println(cursor);
		}
	}

	/**
	 * Writer to efficiently add a new row in the table.
	 *
	 * @author ActiveViam
	 */
	protected class TableWriter implements ITableWriter {

		/**
		 * The chunk set to write
		 */
		protected ChunkSet chunkSet;
		/**
		 * The row (in chunk) to write
		 */
		protected int chunkRow = -1;

		@Override
		public void setRow(int row) {
			this.chunkSet = ColumnarTable.this.chunks[row >>> ColumnarTable.this.chunkOrder];
			this.chunkRow = row & ColumnarTable.this.chunkMask;
		}

		@Override
		public void writeInt(int column, int value) {
			this.chunkSet.writeInt(this.chunkRow, column, value);
		}

		@Override
		public void writeDouble(int column, double value) {
			this.chunkSet.writeDouble(this.chunkRow, column, value);
		}
		
		@Override
		public long sizeInBytes() {
			// 16: Object header
			// 8:  Reference to chunkset
			// 4:  chunkRow attribute
			return 16 + 8 + 4;
		}

		@Override
		public String toString() {
			return Stream.concat(
							IntStream.range(0, attributeCount)
									.map(a -> chunkSet.readInt(this.chunkRow, a))
									.mapToObj(value -> String.format("%3d", value)),
//						.mapToObj(Integer::toString),
							IntStream.range(0, valueCount)
									.mapToDouble(a -> chunkSet.readDouble(this.chunkRow, a))
									.mapToObj(value -> String.format("%5.1f", value)))
//						.mapToObj(Double::toString))
					.collect(Collectors.joining(" | "));
		}
	}

	public static class TableFormat {

		protected final int attributeCount;
		protected final int valueCount;
		protected final int chunkSize;

		public TableFormat(int attributeCount, int valueCount, int chunkSize) {
			this.attributeCount = attributeCount;
			this.valueCount = valueCount;
			this.chunkSize = chunkSize;
		}

		public int getChunkSize() {
			return chunkSize;
		}
	}

}
