package com.activeviam.structures.store.impl;

import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;

/**
 * Tests for {@link MultiVersionStoreV2}.
 *
 * @author ActiveViam
 */
public class TestMultiVersionStoreV2 extends ATestMultiVersionStore {

	@Override
	protected AMultiVersionStore create(StoreFormat format) {
		return new MultiVersionStoreV2(format);
	}

}
