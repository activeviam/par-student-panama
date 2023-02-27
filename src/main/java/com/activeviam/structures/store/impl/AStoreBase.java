package com.activeviam.structures.store.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.structures.index.IMultiVersionPrimaryIndex;
import com.activeviam.structures.index.IMultiVersionSecondaryIndex;
import com.activeviam.structures.index.impl.MultiVersionPrimaryIndex;
import com.activeviam.structures.store.IMultiVersionStore;
import com.activeviam.structures.store.IMultiVersionTable;
import com.activeviam.structures.store.IStoreBase;
import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;

/**
 * Class to extends when one wants to change the implementation of the
 * {@link IMultiVersionSecondaryIndex} in an {@link IMultiVersionStore}.
 *
 * @author ActiveViam
 */
public abstract class AStoreBase implements IStoreBase {

	protected final StoreFormat format;
	protected final IMultiVersionTable table;
	protected final IMultiVersionPrimaryIndex primaryIndex;

	public AStoreBase(StoreFormat format) {
		this.format = format;
		this.table = new MultiVersionTable(format);
		this.primaryIndex = new MultiVersionPrimaryIndex(format.keyFields);
	}

	@Override
	public StoreFormat getFormat() {
		return format;
	}

	@Override
	public IMultiVersionTable getTable() {
		return table;
	}

	@Override
	public IMultiVersionPrimaryIndex getPrimaryIndex() {
		return primaryIndex;
	}

	@Override
	public void commit(IEpoch epoch) {
		getTable().commit(epoch);
		getPrimaryIndex().commit(epoch);
		getSecondaryIndex().commit(epoch);
	}

	@Override
	public long sizeInBytes() {
		// 16: Object header
		// 4:  Reference to format
		// 4:  Reference to table
		// 4:  Reference to primaryIndex
		// 4:  Reference to secondaryIndex
		return 16 + 4 + 4 + 4 + 4 + format.sizeInBytes() + table.sizeInBytes() + primaryIndex.sizeInBytes() + getSecondaryIndex().sizeInBytes();
	}

	@Override
	public void discardBefore(long epoch) {
		table.discardBefore(epoch);
		getSecondaryIndex().discardBefore(epoch, table.getVersions(), table.getDeletedRows());
	}
}
