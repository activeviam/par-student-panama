package com.activeviam.structures.store.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.ITransaction;
import com.activeviam.mvcc.IVersion;
import com.activeviam.mvcc.impl.ABasicVersion;
import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.store.IMultiVersionTable;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.ITableVersion;

/**
 * {@link IVersion} for {@link IMultiVersionTable}.
 *
 * @author ActiveViam
 */
public class TableVersion extends ABasicVersion<VersionedColumnarTable> implements ITableVersion {

	/** The table size visible for this version */
	protected final int visibleSize;

	/**
	 * Constructor
	 *
	 * @param base The base.
	 * @param size The table size visible to this version
	 * @param epoch The epoch at which this version was created.
	 */
	public TableVersion(VersionedColumnarTable base, final int size, IEpoch epoch) {
		super(epoch, base);
		this.visibleSize = size;
	}

	@Override
	public void markObsolete(IVersion newLatest, ITransaction transaction) {
		// Nothing to do
	}

	@Override
	public int readInt(int row, int column) {
		return base.readInt(row, column);
	}
	
	@Override
	public void readInts(final int row, final int column, final int number, final int[] result) {
		base.readInts(row, column, number, result);
	}

	@Override
	public double readDouble(int row, int column) {
		return base.readDouble(row, column);
	}

	@Override
	public IRecord getRecord(int row) {
		return base.getRecord(row);
	}

	@Override
	public int size() {
		return base.getValidRecordCount(epoch.getId(), visibleSize);
	}

	@Override
	public IBitmap findRows(int[] predicate) {
		return filter(base.findRows(predicate));
	}

	@Override
	public int visibleSize() {
		return visibleSize;
	}

	@Override
	public boolean exists(int row) {
		return base.exists(row, epoch.getId(), visibleSize);
	}

	@Override
	public IBitmap filter(IBitmap rows) {
		return base.filter(rows, epoch.getId(), visibleSize);
	}

}
