package com.activeviam.structures.store;

import com.activeviam.mvcc.IMultiVersion;
import com.activeviam.structures.IDiscardable;

/**
 * @author ActiveViam
 */
public interface IMultiVersionTable
		extends IMultiVersion<ITableVersion, ITableTransaction>, IDiscardable {

	/**
	 * Gets the base of the table
	 * 
	 * @return the base of the table
	 */
	public ITable getBase();
	
	/**
	 * Gets the total size of the table.
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 * @return estimated size (in bytes) of the table
	 */
	long sizeInBytes();

	/**
	 * Gets the versions table from the base
	 * @return version history for all records in the table
	 */
	long[][] getVersions();

	/**
	 * @return the number of deleted rows per chunk
	 */
	int[] getDeletedRows();

}
