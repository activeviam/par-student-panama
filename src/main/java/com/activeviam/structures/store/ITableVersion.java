package com.activeviam.structures.store;

import com.activeviam.mvcc.IVersion;
import com.activeviam.structures.bitmap.IBitmap;

/**
 * {@link IVersion} for {@link IMultiVersionTable}.
 *
 * @author ActiveViam
 */
public interface ITableVersion extends IVersion, ITable {

	/**
	 * Returns <code>true</code> if the row exists for this version, or <code>false</code> if the
	 * row does not exist or has been deleted.
	 *
	 * @param row the row to check for existence
	 * @return <code>true</code> if the row exists
	 */
	boolean exists(int row);

	/**
	 * Gets the rows for the bitmap existing for this version.
	 *
	 * @param rows A set of rows
	 * @return the rows among the given set existing for this version
	 */
	IBitmap filter(IBitmap rows);

	/**
	 * Gets the number of records alive at this version.
	 *
	 * @return the number of records
	 */
	@Override
	int size();
	
	/**
	 * Gets the table size visible for this version.
	 *
	 * @return the number of records visible at this version
	 */
	int visibleSize();

}
