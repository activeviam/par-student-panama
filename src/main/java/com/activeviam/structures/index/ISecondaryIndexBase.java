package com.activeviam.structures.index;

/**
 * @author ActiveViam
 */
public interface ISecondaryIndexBase extends IWritableSecondaryIndex {

	/**
	 * @param size the new size of the base
	 */
	void truncate(int size);

	/**
	 * @return the number of indexed records
	 */
	int getSize();

	/**
	 * Gets the total size of the index.
	 *
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 *
	 * @return estimated size (in bytes) of the index
	 */
	long sizeInBytes();

	/**
	 * Physically removes the index content that no longer exist since the given epoch.
	 *
	 * @param epoch the oldest epoch on which queries can still be made
	 * @param versions the table containing the whole history of row versions
	 * @param deletedRows the number of deleted rows
	 */
	void discardBefore(long epoch, long[][] versions, int[] deletedRows);

}
