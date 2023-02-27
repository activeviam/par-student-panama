package com.activeviam.structures.store.impl;

/**
 * Multiversionned store with a primary index and a single secondary index.
 *
 * @author ActiveViam
 */
public class MultiVersionStoreV3 extends AMultiVersionStore {

	public MultiVersionStoreV3(StoreFormat format) {
		super(new StoreBaseV3(format));
	}

}
