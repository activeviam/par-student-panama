package com.activeviam.structures.store.impl;

import com.activeviam.structures.index.IMultiVersionSecondaryIndex;
import com.activeviam.structures.index.impl.BitSetBasedAppendOnlySecondaryIndex;
import com.activeviam.structures.index.impl.MultiVersionAppendOnlySecondaryIndex;
import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;

/**
 * The base of {@link MultiVersionStoreV1}.
 *
 * @author ActiveViam
 */
public class StoreBaseV1 extends AStoreBase {

	protected final IMultiVersionSecondaryIndex secondaryIndex;

	public StoreBaseV1(StoreFormat format) {
		super(format);
		this.secondaryIndex = new MultiVersionAppendOnlySecondaryIndex<>(
				new BitSetBasedAppendOnlySecondaryIndex(format.getIndexedFields()));
	}

	@Override
	public IMultiVersionSecondaryIndex getSecondaryIndex() {
		return secondaryIndex;
	}

}
