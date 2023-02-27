package com.activeviam.structures.store.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.ITableTransaction;
import com.activeviam.structures.store.ITableVersion;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ActiveViam
 */
public class TableTransaction implements ITableTransaction {

	protected final VersionedColumnarTable base;

	protected final Set<Integer> deletions = new HashSet<>();

	/** Size of the store before the transaction */
	protected final int initialSize;

	protected final ITableVersion currentVersion;

	public TableTransaction(VersionedColumnarTable base, ITableVersion currentVersion) {
		this.base = base;
		this.currentVersion = currentVersion;
		this.initialSize = base.size();
	}

	@Override
	public void commit(IEpoch epoch) {
		base.commit(initialSize, epoch.getId(), deletions);
	}

	@Override
	public void rollback() {
		deletions.clear();
		base.truncate(initialSize);
	}

	@Override
	public IRecord read(int row) {
		return exists(row) ? base.getRecord(row) : null;
	}

	@Override
	public boolean exists(int row) {
		// A row cannot exist above the size
		if (row >= base.size()) {
			return false;
		}

		// A row does not exist if it has been
		// deleted in this transaction
		if (deletions.contains(row)) {
			return false;
		}

		// A row exists if it was added in this
		// transaction.
		if (row >= initialSize) {
			return true;
		}

		// This row does not belong to this transaction.
		// Check the current version.
		return currentVersion.exists(row);
	}

	@Override
	public int append(IRecord record) {
		return base.append(record);
	}

	@Override
	public void delete(int row) {
		deletions.add(row);
	}

	@Override
	public void ensureCanAdd(int count) {
		base.ensureCapacity(base.size() + count);
	}

}
