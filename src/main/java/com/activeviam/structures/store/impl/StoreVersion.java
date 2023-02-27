package com.activeviam.structures.store.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.ITransaction;
import com.activeviam.mvcc.IVersion;
import com.activeviam.mvcc.impl.ABasicVersion;
import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.index.IPrimaryIndexVersion;
import com.activeviam.structures.index.ISecondaryIndexVersion;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.IStoreVersion;
import com.activeviam.structures.store.ITableVersion;

/**
 * @author ActiveViam
 */
public class StoreVersion extends ABasicVersion<Void> implements IStoreVersion {

	protected final ITableVersion table;
	protected final IPrimaryIndexVersion primaryIndex;
	protected final ISecondaryIndexVersion secondaryIndex;

	/**
	 * Constructor
	 *
	 * @param epoch
	 * @param table
	 * @param primaryIndex
	 * @param secondaryIndex
	 */
	public StoreVersion(IEpoch epoch, ITableVersion table,
			IPrimaryIndexVersion primaryIndex, ISecondaryIndexVersion secondaryIndex) {
		super(epoch, null);
		this.table = table;
		this.primaryIndex = primaryIndex;
		this.secondaryIndex = secondaryIndex;
	}

	@Override
	public void markObsolete(IVersion newLatest, ITransaction transaction) {
		// Nothing to do
	}

	@Override
	public int readInt(int row, int column) {
		return table.readInt(row, column);
	}

	@Override
	public void readInts(final int row, final int column, final int number, final int[] result) {
		table.readInts(row, column, number, result);
	}

	@Override
	public double readDouble(int row, int column) {
		return table.readDouble(row, column);
	}

	@Override
	public IRecord getRecord(int row) {
		return table.getRecord(row);
	}

	@Override
	public int size() {
		return table.size();
	}

	@Override
	public IBitmap findRows(final int[] pattern) {
		final IBitmap rows = secondaryIndex.getRows(pattern);
		return table.filter(rows);
	}

	@Override
	public IBitmap findRows(final int[][] compositePattern) {
		final IBitmap rows = secondaryIndex.getRows(compositePattern);
		return table.filter(rows);
	}

}
