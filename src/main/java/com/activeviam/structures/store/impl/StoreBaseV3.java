package com.activeviam.structures.store.impl;

import com.activeviam.structures.index.IMultiVersionSecondaryIndex;
import com.activeviam.structures.index.impl.MultiVersionSecondaryIndexV3;
import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;

/**
 * The base of {@link MultiVersionStoreV3}.
 */
public class StoreBaseV3 extends AStoreBase {

	protected final IMultiVersionSecondaryIndex secondaryIndex;

	public StoreBaseV3(StoreFormat format) {
		super(format);
		this.secondaryIndex = new MultiVersionSecondaryIndexV3(format.indexedFields, table.getBase());
	}

	@Override
	public IMultiVersionSecondaryIndex getSecondaryIndex() {
		return secondaryIndex;
	}

}
