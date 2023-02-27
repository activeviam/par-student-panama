package com.activeviam.structures.store.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.impl.AMultiVersion;
import com.activeviam.structures.store.IMultiVersionTable;
import com.activeviam.structures.store.ITable;
import com.activeviam.structures.store.ITableTransaction;
import com.activeviam.structures.store.ITableVersion;
import com.activeviam.structures.store.IWritableTable;
import com.activeviam.structures.store.impl.ColumnarTable.TableFormat;

/**
 * MVCC {@link IWritableTable}.
 *
 * @author ActiveViam
 */
public class MultiVersionTable
		extends AMultiVersion<ITableVersion, ITableTransaction, VersionedColumnarTable>
		implements IMultiVersionTable {

	public MultiVersionTable(TableFormat format) {
		super(new VersionedColumnarTable(format));
	}
	
	@Override
	public ITable getBase() {
		return base;
	}

	@Override
	protected ITableTransaction createNewTransaction() {
		return new TableTransaction(base, latest);
	}

	@Override
	protected ITableVersion createVersion(IEpoch epoch, ITableTransaction transaction) {
		return new TableVersion(base, base.size(), epoch);
	}

	@Override
	public long sizeInBytes() {
		// 16: Object header
		// 4: reference to base
		return 16 + 4 + base.sizeInBytes();
	}

	@Override
	public void discardBefore(long epoch) {
		base.discardBefore(epoch);
	}

	@Override
	public long[][] getVersions() {
		return this.base.getVersions();
	}

	@Override
	public int[] getDeletedRows() {
		return this.base.getDeletedRows();
	}
}
