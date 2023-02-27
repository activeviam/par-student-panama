package com.activeviam.structures.store.impl;

/**
 * Multiversionned store with a primary index and a single secondary index.
 *
 * @author ActiveViam
 */
public class MultiVersionStoreV1 extends AMultiVersionStore {

	public MultiVersionStoreV1(StoreFormat format) {
		super(new StoreBaseV1(format));
	}

}
