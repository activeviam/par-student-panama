package com.activeviam.structures.index.impl;

import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;

public class SecondaryIndexBaseV2 extends ASecondaryIndexBaseV2<BitSetBasedAppendOnlySecondaryIndex> {

	public SecondaryIndexBaseV2(StoreFormat format) {
		super(format, new BitSetBasedAppendOnlySecondaryIndex[0]);
	}

	@Override
	protected BitSetBasedAppendOnlySecondaryIndex createChunk() {
		return new BitSetBasedAppendOnlySecondaryIndex(indexedFields);
	}

}
