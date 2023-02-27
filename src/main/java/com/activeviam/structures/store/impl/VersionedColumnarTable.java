package com.activeviam.structures.store.impl;

import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.bitmap.impl.BitSetBitmap;
import com.activeviam.structures.store.IVersionedTable;
import java.util.Arrays;
import java.util.Set;

/**
 * A {@link ColumnarTable} where each record hold a version number.
 *
 * @author ActiveViam
 */
public class VersionedColumnarTable extends ColumnarTable implements IVersionedTable {

	/**
	 * The column holding the version number at which each row has been inserted or removed.
	 *
	 * <p>
	 *   A positive version value indicates that this row's current state is valid since this version,
	 *   while a strictly negative version means that a row has been removed at {@code -version-1}.
	 * </p>
	 */
	protected long[][] versions;

	/**
	 * The number of deletions per chunk.
	 */
	protected int[] deletedRows;

	public VersionedColumnarTable(TableFormat format) {
		super(format);
		this.versions = new long[0][];
		this.deletedRows = new int[0];
	}

	@Override
	public long sizeInBytes() {
		long size = super.sizeInBytes();
		size += 4; // reference to versions
		size += 12; // header of versions
		size += versions.length * 4; // array of references
		for (long[] v : versions) {
			if (null != v) {
				size += 12; // header of v
				size += v.length * 8; // size of v
			}
		}
		size += 4; // reference to deletions
		size += 12; // header of deletions
		size += deletedRows.length * 4; // content of deletions
		return size;
	}

	@Override
	protected void setChunkCount(final int numChunks) {
		super.setChunkCount(numChunks);
		final long[][] oldVersionChunks = this.versions;
		final int numOldChunks = oldVersionChunks != null ? oldVersionChunks.length : 0;
		// Nothing to do if the number of chunks is already correct
		if (numChunks == numOldChunks) {
			return;
		}

		final long[][] newVersionChunks = Arrays.copyOf(oldVersionChunks, numChunks);
		for (int i = numOldChunks; i < numChunks; ++i) {
			newVersionChunks[i] = new long[1 << chunkOrder];
		}
		this.versions = newVersionChunks;
		this.deletedRows =  Arrays.copyOf(deletedRows, numChunks);
	}

	/**
	 * Commits this base.
	 *
	 * @param initialSize The size of the table at the beginning of the transaction.
	 * @param epoch The epoch to commit.
	 * @param deletions The set of rows that are deleted in this transaction.
	 */
	public void commit(int initialSize, long epoch, Set<Integer> deletions) {
		for (int row = initialSize; row < size; ++row) {
			final int chunkId = row >>> this.chunkOrder;
			final int chunkRow = row & this.chunkMask;
			versions[chunkId][chunkRow] = epoch;
		}
		deletions.forEach(row -> {
			final int chunkId = row >>> this.chunkOrder;
			final int chunkRow = row & this.chunkMask;
			versions[chunkId][chunkRow] = -1 - epoch;
			++deletedRows[chunkId];
		});
	}

	@Override
	public void discardBefore(long epoch) {
		final int[] deletedRows = this.deletedRows;
		for (int chunkId = 0; chunkId < deletedRows.length; chunkId++) {
			if (deletedRows[chunkId] == chunkSize) {
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
					chunks[chunkId] = null;
				}
			}
		}
	}

	@Override
	public boolean exists(int row, long epoch, int visibleSize) {
		if (row >= visibleSize) {
			return false;
		}
		final int chunkId = row >>> this.chunkOrder;
		final int chunkRow = row & this.chunkMask;
		final long[] versionChunk = versions[chunkId];
		if (null == versionChunk) {
			return false;
		}
		final long rowVersion = versionChunk[chunkRow];
		// If negative number because line has been deleted
		return rowVersion > 0 || (-rowVersion - 1) > epoch;
	}

	@Override
	public IBitmap filter(IBitmap rows, long epoch, int visibleSize) {
		final IBitmap r = new BitSetBitmap();
		// FIXME iterate on versions (this will avoid the decoding)
		rows.stream()
			.takeWhile(row -> row < visibleSize)
			.filter(row -> exists(row, epoch, visibleSize))
			.forEach(r::set);
		return r;
	}

	@Override
	public int getValidRecordCount(long epoch, int visibleSize) {
		int recordCount = 0;
		final int numChunks = versions != null ? ((visibleSize - 1) >>> this.chunkOrder) + 1 : 0;
		int chunkSize = 1 << this.chunkOrder;
		for (int i = 0; i < numChunks; ++i) {
			final long[] versionChunk = versions[i];
			if (i == numChunks - 1) {
				chunkSize = visibleSize % chunkSize;
				if (chunkSize == 0) {
					chunkSize = 1 << this.chunkOrder;
				}
			}
			for (int j = 0; j < chunkSize; ++j) {
				final long rowVersion = versionChunk[j];
				if (rowVersion > 0 || (-rowVersion - 1) > epoch) {
					++recordCount;
				}
			}
		}
		return recordCount;
	}

	/**
	 * @return the column holding the version number at which each row has been inserted or removed
	 */
	protected long[][] getVersions() {
		return versions;
	}

	/**
	 * @return the number of deleted rows per chunk
	 */
	protected int[] getDeletedRows() {
		return deletedRows;
	}

}
