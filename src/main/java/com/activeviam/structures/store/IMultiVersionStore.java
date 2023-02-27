package com.activeviam.structures.store;

import com.activeviam.mvcc.IMultiVersion;
import com.activeviam.structures.IDiscardable;

/**
 * Interface for MVCC store.
 * <p>
 * A store is an {@link IWritableTable} with indexes.
 *
 * @author ActiveViam
 */
public interface IMultiVersionStore extends IMultiVersion<IStoreVersion, IStoreTransaction>, IDiscardable {

	/**
	 * Gets the total size of the store.
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 * @return estimated size (in bytes) of the store
	 */
	long sizeInBytes();

}
