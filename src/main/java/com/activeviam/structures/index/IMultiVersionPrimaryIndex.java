package com.activeviam.structures.index;

import com.activeviam.mvcc.IMultiVersion;

/**
 * @author ActiveViam
 */
public interface IMultiVersionPrimaryIndex extends IMultiVersion<IPrimaryIndexVersion, IPrimaryIndexTransaction> {

	/**
	 * Gets the total size of the index.
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 * @return estimated size (in bytes) of the index
	 */
	long sizeInBytes();

}
