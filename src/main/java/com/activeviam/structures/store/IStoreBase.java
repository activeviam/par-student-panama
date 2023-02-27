package com.activeviam.structures.store;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.structures.IDiscardable;
import com.activeviam.structures.index.IMultiVersionPrimaryIndex;
import com.activeviam.structures.index.IMultiVersionSecondaryIndex;
import com.activeviam.structures.store.impl.AMultiVersionStore;
import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;

/**
 * Components used by the {@link AMultiVersionStore}.
 * 
 * <p>
 *   It just contains the multiversion objects used by the {@link AMultiVersionStore}.
 * </p>
 *
 * @author ActiveViam
 */
public interface IStoreBase extends IDiscardable {

	StoreFormat getFormat();

	IMultiVersionTable getTable();

	IMultiVersionPrimaryIndex getPrimaryIndex();

	IMultiVersionSecondaryIndex getSecondaryIndex();

	/**
	 * Commits the underlying multiversion components.
	 * 
	 * <p>
	 *   Must be done at the begining of {@link IMultiVersionStore#commit(IEpoch)}.
	 * </p>
	 *
	 * @param epoch
	 */
	void commit(IEpoch epoch);

	/**
	 * Gets the total size of the store.
	 * 
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 * 
	 * @return estimated size (in bytes) of the store
	 */
	long sizeInBytes();

}
