package com.activeviam.structures.store.impl;

import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;

/**
 * Tests for {@link MultiVersionStoreV3}.
 */
public class TestMultiVersionStoreV3 extends ATestMultiVersionStore {

	@Override
	protected AMultiVersionStore create(StoreFormat format) {
		return new MultiVersionStoreV3(format);
	}

}
