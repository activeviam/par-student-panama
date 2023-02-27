package com.activeviam.structures.index;

import com.activeviam.mvcc.ITransaction;

/**
 * Transaction for {@link IMultiVersionPrimaryIndex}.
 *
 * @author ActiveViam
 */
public interface IPrimaryIndexTransaction extends ITransaction {

	/**
	 * Indexes a point with the given row.
	 *
	 * @param point the point to index
	 * @param row the row of the point
	 */
	void index(int[] point, int row);

	/**
	 * Gets the row of a point
	 * @param point
	 * @return the row of a point
	 */
	int getRow(int[] point);

}
