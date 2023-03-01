package com.activeviam.structures.store.impl;

import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;

/**
 * Tests for {@link MultiVersionStoreV1}.
 *
 * @author ActiveViam
 */
public class TestMultiVersionStoreV1 extends ATestMultiVersionStore {

	@Override
	protected AMultiVersionStore create(StoreFormat format) {
		return new MultiVersionStoreV1(format);
	}

}
