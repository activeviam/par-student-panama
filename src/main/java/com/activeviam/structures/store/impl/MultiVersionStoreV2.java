package com.activeviam.structures.store.impl;

/**
 * Multiversionned store with a primary index and a single secondary index.
 *
 * @author ActiveViam
 */
public class MultiVersionStoreV2 extends AMultiVersionStore {

	public MultiVersionStoreV2(StoreFormat format) {
		super(new StoreBaseV2(format));
	}

}
