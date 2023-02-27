package com.activeviam.structures.index;

import com.activeviam.structures.store.IRecord;

/**
 * Interface for primary index.
 *
 * @author ActiveViam
 */
public interface IPrimaryIndex {

	/**
	 * Gets the row of a record
	 *
	 * @param record a record
	 * @return the row of the record or {@code -1} if the record is not in the index
	 */
	int getRow(IRecord record);

}
