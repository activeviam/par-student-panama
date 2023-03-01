package com.activeviam.structures.index.impl;

import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.bitmap.impl.ChunkedBitmap;
import com.activeviam.structures.index.ISecondaryIndexBase;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;
import com.activeviam.structures.store.impl.ColumnarTable;
import java.util.Arrays;

public abstract class ASecondaryIndexBaseV2<C extends AAppendOnlySecondaryIndex> implements ISecondaryIndexBase {

  protected C[] chunks;
  /** The size of the chunks */
  protected final int chunkSize;
  /** Chunk order, at the power of 2 it is the chunk size */
  protected final int chunkOrder;
  /** Mask to extract positions within chunks */
  protected final int chunkMask;
  protected final int[] indexedFields;

  public ASecondaryIndexBaseV2(StoreFormat format, C[] emptyArray) {
    this.chunks = emptyArray;
    this.chunkSize = format.getChunkSize();
    if(Integer.bitCount(this.chunkSize) != 1) {
      throw new IllegalArgumentException("ChunkSize is not a power of 2: " + this.chunkSize);
    }
    this.chunkOrder = ColumnarTable.getOrder(format.getChunkSize());
	  this.chunkMask = (1 << this.chunkOrder) - 1;
    this.indexedFields = format.getIndexedFields();
  }

  /**
   * Returns the current capacity of this table
   *
   * @return The current capacity of this table
   */
  public int capacity() {
    final C[] chunks = this.chunks;
    final int numChunks = chunks != null ? chunks.length : 0;
    return numChunks << this.chunkOrder;
  }

  /**
   * Allocates more chunks if needed to reach the
   * requested capacity.
   *
   * <p> This method is not thread-safe.
   *
   * @param capacity The target capacity
   * @return This table's new capacity
   */
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
    final C[] oldChunks = this.chunks;
    final int numOldChunks = oldChunks != null ? oldChunks.length : 0;
    // Nothing to do if the number of chunks is already correct
    if (numChunks == numOldChunks) {
      return;
    }

    final C[] newChunks = Arrays.copyOf(oldChunks, numChunks);
    for (int i = numOldChunks; i < numChunks; i++) {
      newChunks[i] = createChunk();
    }
    this.chunks = newChunks;
  }

  protected abstract C createChunk();

  @Override
  public void index(IRecord record, int row) {
    int currentSize = getSize();
    ensureCapacity(currentSize + 1);

	final int chunkId = row >>> this.chunkOrder;
	final int chunkRow = row & this.chunkMask;
    this.chunks[chunkId].index(record, chunkRow);
  }

  @Override
  public void remove(IRecord record, int row) {
  }

  @Override
  public void truncate(int initialSize) {
    int threshold = initialSize / chunkSize;
    int nbRowsInLastChunk = initialSize % chunkSize;

    // Delete unused chunks
    for (int i = chunks.length - 1; i > threshold; --i) {
      chunks[i] = createChunk();
    }

    // Restore last chunk
    chunks[threshold].truncate(nbRowsInLastChunk);
  }

	@Override
	public IBitmap getRows(final int[] pattern) {
		final C[] chunks = this.chunks; // snapshot the array
		final IBitmap[] bChunks = new IBitmap[chunks.length];
		for (int chunkId = 0; chunkId < chunks.length; ++chunkId) {
			bChunks[chunkId] = chunks[chunkId].getRows(pattern);
		}

		return new ChunkedBitmap(bChunks, this.chunkSize);
	}

	@Override
	public IBitmap getRows(final int[][] compositePattern) {
		final C[] chunks = this.chunks; // snapshot the array
		final IBitmap[] bChunks = new IBitmap[chunks.length];
		for (int chunkId = 0; chunkId < chunks.length; ++chunkId) {
			bChunks[chunkId] = chunks[chunkId].getRows(compositePattern);
		}

		return new ChunkedBitmap(bChunks, this.chunkSize);
	}

  @Override
  public int[] extractPoint(IRecord record) {
    final int[] point = new int[indexedFields.length];
    for (int i = 0; i < point.length; ++i) {
      point[i] = record.readInt(indexedFields[i]);
    }
    return point;
  }

  @Override
  public int getSize() {
    int n = chunks.length;
    if (0 == n) return 0;
    return chunkSize * (n - 1) + chunks[n - 1].getSize();
  }

  @Override
  public long sizeInBytes() {
    // 16: Object header
    // 4: chunkSize, chunkOrder attributes
    // 8: References to indexedFields, freeRows and chunks
    long sizeInBytes = 16 + 4 * 2 + 8 * 3;

    // Content of indexedFields
    sizeInBytes += indexedFields.length * 4;
    // Content of chunks
		for (C chunk : chunks) {
			if (null != chunk) {
				sizeInBytes += chunk.sizeInBytes();
			}
		}

    return sizeInBytes;
  }

  @Override
  public void discardBefore(long epoch, long[][] versions, int[] deletedRows) {
    assert versions.length == deletedRows.length;
    for (int chunkId = 0; chunkId < versions.length; chunkId++) {
      if (deletedRows[chunkId] == this.chunkSize) {
        // check the content of versions[i] to decide if we can remove chunks[i] (i.e. do chunks[i] == null)
        boolean shouldDeleteChunk = true;
        for (long rowVersion: versions[chunkId]) {
          // If one version is still valid, break
          if ((rowVersion > 0 || (-rowVersion - 1) > epoch)) {
            shouldDeleteChunk = false;
            break;
          }
        }
        if (shouldDeleteChunk) {
          this.chunks[chunkId] = null;
        }
      }
    }
  }
}
