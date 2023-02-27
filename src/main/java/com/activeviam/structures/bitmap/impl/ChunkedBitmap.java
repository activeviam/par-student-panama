package com.activeviam.structures.bitmap.impl;

import com.activeviam.structures.bitmap.IBitmap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ChunkedBitmap implements IBitmap {

  protected IBitmap[] chunks;
  protected int chunkSize;

  public ChunkedBitmap(int chunkSize) {
    this(new IBitmap[0], chunkSize);
    this.chunkSize = chunkSize;
  }

  public ChunkedBitmap(IBitmap[] chunks, int chunkSize) {
    this.chunks = chunks;
    this.chunkSize = chunkSize;
  }

  /**
   * Returns the current capacity of this bitmap
   *
   * @return The current capacity of this bitmap
   */
  public int capacity() {
    final IBitmap[] chunks = this.chunks;
    final int numChunks = chunks != null ? chunks.length : 0;
    return numChunks * chunkSize;
  }

  /**
   * Allocates more chunks if needed to reach the requested capacity.
   *
   * <p> This method is not thread-safe.
   *
   * @param capacity The target capacity
   * @return This bitmap's new capacity
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
   *
   * @param nbrRecords The number of records
   * @return the number of chunks
   */
  protected int getNumChunks(int nbrRecords) {
    return nbrRecords / this.chunkSize + 1;
  }

  /**
   * Sets the number of chunks that are allocated for each column.
   *
   * @param numChunks The target number of chunks that are allocated for each column
   */
  protected void setChunkCount(final int numChunks) {
    final IBitmap[] oldChunks = this.chunks;
    final int numOldChunks = oldChunks != null ? oldChunks.length : 0;
    // Nothing to do if the number of chunks is already correct
    if (numChunks == numOldChunks) {
      return;
    }

    final IBitmap[] newChunks = Arrays.copyOf(oldChunks, numChunks);
    for (int i = numOldChunks; i < numChunks; i++) {
      newChunks[i] = new BitSetBitmap();
    }
    this.chunks = newChunks;
  }

  protected void setChunk(int chunkId, IBitmap defaultBitmap) {
    if (chunkId < chunks.length && null == chunks[chunkId]) {
      chunks[chunkId] = defaultBitmap;
    }
  }

  @Override
  public void set(int i) {
    ensureCapacity(i + 1);

    int chunkId = i / chunkSize;
    int index = i % chunkSize;

    if (chunks[chunkId] == null) {
      chunks[chunkId] = new BitSetBitmap();
    }
    chunks[chunkId].set(index);
  }

  @Override
public boolean get(int i) {
    int chunkId = i / chunkSize;
    int index = i % chunkSize;

    IBitmap chunk = chunks[chunkId];
    return null != chunk && chunk.get(index);
  }

  @Override
	public boolean isEmpty() {
		for (IBitmap b : chunks) {
			if (null != b && !b.isEmpty()) {
				return false;
			}
		}
		return true;
	}

  @Override
  public void clear() {
    chunks = new IBitmap[0];
  }

  @Override
  public void truncate(int newSize) {
    int chunkId = newSize / chunkSize;
    int index = newSize % chunkSize;

    IBitmap chunk = chunks[chunkId];

    if (null != chunk) {
      // Restore last chunk
      chunk.truncate(index);
    }

    // Delete unused chunks
    for (int i = chunkId + 1; i < chunks.length; ++i) {
      chunks[i] = null;
    }
  }

  @Override
  public IBitmap and(IBitmap other) {
    final ChunkedBitmap o = (ChunkedBitmap) other;
    final int size = Math.min(o.chunks.length, this.chunks.length);

    IBitmap[] chunks = new IBitmap[size];
    for (int i = 0; i < size; ++i) {
      IBitmap left = o.chunks[i];
      IBitmap right = this.chunks[i];

      if (null == left || null == right) {
        chunks[i] = null;
      } else {
        chunks[i] = left.and(right);
      }
    }

    return new ChunkedBitmap(chunks, this.chunkSize);
  }

  @Override
  public IBitmap or(IBitmap other) {
    final ChunkedBitmap o = (ChunkedBitmap) other;
    final int size = Math.max(o.chunks.length, this.chunks.length);

    final IBitmap[] chunks = new IBitmap[size];
    for (int i = 0; i < size; ++i) {
      // Size can be larger than one of these two lengths
      IBitmap left = i < o.chunks.length ? o.chunks[i] : null;
      IBitmap right = i < this.chunks.length ? this.chunks[i] : null;

      if (null == left) {
        chunks[i] = right;
      } else if (null == right) {
        chunks[i] = left;
      } else {
        chunks[i] = left.or(right);
      }
    }

    return new ChunkedBitmap(chunks, this.chunkSize);
  }

  @Override
  public void and(IBitmap operand, IBitmap result) {
    final ChunkedBitmap r = (ChunkedBitmap) result;
    final ChunkedBitmap o = (ChunkedBitmap) operand;
    final int size = Math.min(o.chunks.length, this.chunks.length);
    r.chunks = new IBitmap[size];

    for (int i = 0; i < size; ++i) {
      IBitmap left = o.chunks[i];
      IBitmap right = this.chunks[i];

      if (null == left || null == right) {
        r.chunks[i] = null;
      } else {
        r.setChunk(i, new BitSetBitmap());
        left.and(right, r.chunks[i]);
      }
    }
  }

  @Override
  public void or(IBitmap operand, IBitmap result) {
    final ChunkedBitmap r = (ChunkedBitmap) result;
    final ChunkedBitmap o = (ChunkedBitmap) operand;
    final int size = Math.max(o.chunks.length, this.chunks.length);
    r.chunks = new IBitmap[size];

    for (int i = 0; i < size; ++i) {
      // Size can be larger than one of these two lengths
      IBitmap left = i < o.chunks.length ? o.chunks[i] : null;
      IBitmap right = i < this.chunks.length ? this.chunks[i] : null;

      if (null == left) {
        r.chunks[i] = right;
      } else if (null == right) {
        r.chunks[i] = left;
      } else {
        r.setChunk(i, new BitSetBitmap());
        left.or(right, r.chunks[i]);
      }
    }
  }

  @Override
  public long sizeInBytes() {
    // 16: Object header
    // 4: chunkSize attribute
    // 8: Reference to chunks
    long sizeInBytes = 16 + 4 + 8;

    // Content of chunks
    for (IBitmap chunk : chunks) {
      if (null != chunk) {
        sizeInBytes += chunk.sizeInBytes();
      }
    }

    return sizeInBytes;
  }

  @Override
  /**
   * This method is a bit tricky because it requires us to regenerate the original element id
   * We don't want the item 128 to be returned as 0 so we need to iterate through all items
   */
  public IntStream stream() {
    final List<IntStream> streams = new ArrayList<>(chunks.length);
    for (int i = 0; i < chunks.length; i++) {
      final IBitmap chunk = chunks[i];
      if (null == chunk)
        continue;
      final int offset = i * chunkSize;
      streams.add(chunk.stream().map(j -> j + offset));
    }
    return streams.stream().flatMapToInt(Function.identity());
  }

  @Override
  public IBitmap clone() {
    final IBitmap[] chunks = new IBitmap[this.chunks.length];

    for (int i = 0; i < this.chunks.length; ++i) {
      chunks[i] = null != this.chunks[i] ? this.chunks[i].clone() : null;
    }
    return new ChunkedBitmap(chunks, this.chunkSize);
  }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + chunkSize;
		result = prime * result + Arrays.hashCode(chunks);
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
		ChunkedBitmap other = (ChunkedBitmap) obj;
		if (chunkSize != other.chunkSize)
			return false;
		// Not fully correct (if an array ends with more nulls)
		if (!Arrays.equals(chunks, other.chunks))
			return false;
		return true;
	}

}
