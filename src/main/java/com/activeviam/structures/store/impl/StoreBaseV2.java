package com.activeviam.structures.store.impl;

import com.activeviam.structures.index.IMultiVersionSecondaryIndex;
import com.activeviam.structures.index.impl.MultiVersionAppendOnlySecondaryIndex;
import com.activeviam.structures.index.impl.SecondaryIndexBaseV2;
import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;

/**
 * The base of {@link MultiVersionStoreV2}.
 *
 * @author ActiveViam
 */
public class StoreBaseV2 extends AStoreBase {

	protected final IMultiVersionSecondaryIndex secondaryIndex;

	public StoreBaseV2(StoreFormat format) {
		super(format);
		this.secondaryIndex = new MultiVersionAppendOnlySecondaryIndex<>(new SecondaryIndexBaseV2(format));
	}

	@Override
	public IMultiVersionSecondaryIndex getSecondaryIndex() {
		return secondaryIndex;
	}

}
